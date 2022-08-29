package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.mapper.OmsOrderItemMapper;
import cn.tedu.mall.order.mapper.OmsOrderMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.order.utils.IdGeneratorUtils;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderItemAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.dto.OrderStateUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.order.vo.OrderDetailVO;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import cn.tedu.mall.product.service.order.IForOrderSkuService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@DubboService
@Service
@Slf4j
public class OmsOrderServiceImpl implements IOmsOrderService {

    // 利用Dubbo获得减少库存功能的业务逻辑实现
    @DubboReference
    private IForOrderSkuService dubboSkuService;
    @Autowired
    private IOmsCartService cartService;
    @Autowired
    private OmsOrderMapper orderMapper;
    @Autowired
    private OmsOrderItemMapper orderItemMapper;

    // 因为当前项目使用Dubbo来操作了其它微服务模块的数据
    // 所以这个事务是一个分布式事务操作
    // 使用注解激活分布式事务,令Seata来保证事务的原子性
    @GlobalTransactional
    @Override
    public OrderAddVO addOrder(OrderAddDTO orderAddDTO) {
        // 第一部分,收集信息,准备数据
        // 操作数据库的类型是OmsOrder,需要将参数orderAddDTO中同名属性赋值到其中
        OmsOrder order=new OmsOrder();
        BeanUtils.copyProperties(orderAddDTO,order);
        // 因为需要收集和计算的业务代码较多,单独编写一个方法
        loadOrder(order);
        // 上面是为订单OmsOrder赋值
        // 下面开始为订单中的订单项赋值
        // orderAddDTO中包含本订单中所有的商品,也是订单项
        // 获取这些订单项,检查是否为空
        List<OrderItemAddDTO> itemAddDTOs=orderAddDTO.getOrderItems();
        if(itemAddDTOs==null || itemAddDTOs.isEmpty()){
            // 如果订单项集合为空或没有元素,直接抛出异常,终止业务
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,"订单中必须包含商品");
        }
        // 现在我们有List<OrderItemAddDTO>的集合,来保存订单项信息
        // 但是持久性新增order_item的参数类型List<OmsOrderItem>
        // 所以我们要遍历当前itemAddDTOs集合,将其中的对象转换为OmsOrderItem类型,并保存在新的集合中
        List<OmsOrderItem> omsOrderItems=new ArrayList<>();
        // 遍历itemAddDTOs
        for(OrderItemAddDTO addDTO: itemAddDTOs){
            // 首先还是创建OmsOrderItem对象,然后将同名属性赋值
            OmsOrderItem orderItem=new OmsOrderItem();
            BeanUtils.copyProperties(addDTO,orderItem);
            // 和Order一样,OrderItem也要单独写一个方法判断赋值
            loadOrderItem(orderItem);
            // 上面的方法赋值完成,能够确定orderItem的id,下面将omsOrder的订单id赋值给这个对象
            orderItem.setOrderId(order.getId());
            // 最后将orderItem保存在集合中
            omsOrderItems.add(orderItem);
            // 到此为止,我们第一部分的赋值过程就完成了
            // 第二部分,完成新增订单的业务逻辑层操作数据库的过程
            // 1.减少sku库存
            // 获得skuId
            Long skuId=orderItem.getSkuId();
            // 执行减少库存
            int rows=dubboSkuService.reduceStockNum(skuId,orderItem.getQuantity());
            // 判断执行的影响行数
            if(rows==0){
                log.warn("商品skuId:{},库存不足",skuId);
                // 减少库存失败,大概率是库存不足导致的,所有要抛出异常,给出提示
                // 此时Seata会回滚所有之前数据库操作
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,"库存不足");
            }
            // 2.删除购物车信息
            OmsCart omsCart=new OmsCart();
            omsCart.setUserId(order.getUserId());
            omsCart.setSkuId(skuId);
            cartService.removeUserCarts(omsCart);
        }
        // 3.新增订单
        // 使用OmsOrderMapper的方法完成订单的新增
        orderMapper.insertOrder(order);
        // 4.新增订单项
        // 使用OmsOrderItemMapper的方法完成订单项的新增
        orderItemMapper.insertOrderItems(omsOrderItems);
        // 最后收集生成订单过程中的一些重要数据,返回给前端
        // 程序设计使用OrderAddVO来完成
        OrderAddVO addVO=new OrderAddVO();
        addVO.setId(order.getId());  // 订单id
        addVO.setSn(order.getSn());  // 订单号sn
        addVO.setCreateTime(order.getGmtOrder());  // 订单生成时间
        addVO.setPayAmount(order.getAmountOfActualPay());   // 实际支付金额
        // 最后别忘了返回
        return addVO;
    }

    private void loadOrderItem(OmsOrderItem orderItem) {
        if(orderItem.getId()==null){
            Long id=IdGeneratorUtils.getDistributeId("order_item");
            orderItem.setId(id);
        }
        if(orderItem.getSkuId()==null){
            throw new CoolSharkServiceException(
                            ResponseCode.BAD_REQUEST,"订单商品中必须包含skuId");
        }
    }

    // 新增订单业务中,需要收集和计算的order对象信息的方法
    private void loadOrder(OmsOrder order) {
        // 针对order对象必须具备但是为null的值进行赋值
        // 判断id是否为空
        if(order.getId()==null){
            // 使用Leaf获得分布式Id
            Long id= IdGeneratorUtils.getDistributeId("order");
            order.setId(id);
        }
        // 判断 userId是否为空
        if(order.getUserId()==null){
            // 从SpringSecurity上下文中获得用户id
            order.setUserId(getUserId());
        }
        // 判断订单号sn并赋值
        if (order.getSn()==null){
            order.setSn(UUID.randomUUID().toString());
        }
        // 判断 state并赋值
        if(order.getState()==null){
            // 新生成的订单为"未支付"状态,状态码为0
            order.setState(0);
        }
        // 为了保证生成订单时间和数据的创建时间以及最后修改时间一致
        // 我们本次新增订单使用手动统一赋值
        if(order.getGmtOrder()==null){
            LocalDateTime now=LocalDateTime.now();
            order.setGmtOrder(now);
            order.setGmtCreate(now);
            order.setGmtModified(now);
        }
        // 后端计算实际支付金额
        // 计算基本公式: 原价+运费-优惠=实际支付金额
        // 判断运费和优惠是否为null 如果是null默认赋值0
        if(order.getAmountOfFreight()==null){
            order.setAmountOfFreight(new BigDecimal(0.0));
        }
        if(order.getAmountOfDiscount()==null){
            order.setAmountOfDiscount(new BigDecimal(0.0));
        }
        // 如果原价是null 业务无法继续,抛出异常
        if(order.getAmountOfOriginalPrice()==null){
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,"没有订单原价!");
        }
        // 计算实际支付金额
        // 原价+运费-优惠
        BigDecimal price=order.getAmountOfOriginalPrice();
        BigDecimal freight=order.getAmountOfFreight();
        BigDecimal discount=order.getAmountOfDiscount();
        BigDecimal actualPay=price.add(freight).subtract(discount);
        // 给实际支付金额赋值
        order.setAmountOfActualPay(actualPay);

    }

    // 根据订单id修改订单状态的业务逻辑层方法
    @Override
    public void updateOrderState(OrderStateUpdateDTO orderStateUpdateDTO) {
        // 参数orderStateUpdateDTO包含订单id和状态码
        // 我们修改订单的方法参数是OmsOrder,所以需要实例化这个类型对象并赋值
        OmsOrder order=new OmsOrder();
        BeanUtils.copyProperties(orderStateUpdateDTO,order);
        // 提交到修改订单状态的方法
        // 动态修改OmsOrder的方法,因为只有id和state两个属性被赋值,所以其它属性不会受到影响
        orderMapper.updateOrderById(order);
    }

    // 分页查询当前登录用户在指定时间范围内(默认一个月内)所有订单
    // 订单包含订单信息和订单项信息两个方面(xml的sql语句是关联查询)
    @Override
    public JsonPage<OrderListVO> listOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO) {
        // 因为默认查询最近一个月的订单,所以参数对象中的startTime和endTime为空的话,就赋默认值
        // 如果有值的话,还要检查起始和结束时间是否合理
        validaTimeAndLoadTimes(orderListTimeDTO);
        // 获得用户Id
        Long userId=getUserId();
        // orderListTimeDTO肯定没有用户Id的,所以将用户id赋值到这个对象中
        orderListTimeDTO.setUserId(userId);
        // 设置分页条件
        PageHelper.startPage(orderListTimeDTO.getPage(),orderListTimeDTO.getPageSize());
        // 执行查询
        List<OrderListVO> list=orderMapper.selectOrdersBetweenTimes(orderListTimeDTO);
        // 别忘了返回
        return JsonPage.restPage(new PageInfo<>(list));
    }

    private void validaTimeAndLoadTimes(OrderListTimeDTO orderListTimeDTO) {
        // 取出起始时间和结束时间
        LocalDateTime start=orderListTimeDTO.getStartTime();
        LocalDateTime end=orderListTimeDTO.getEndTime();
        // 只要起始或结束时间有一个为null,就设置查询最近一个月的订单
        if(start==null || end==null){
            // 起始时间赋值为现在时间减一个月
            start=LocalDateTime.now().minusMonths(1);
            // 默认结束时间是当前时间
            end=LocalDateTime.now();
            // 赋值到orderListTimeDTO
            orderListTimeDTO.setStartTime(start);
            orderListTimeDTO.setEndTime(end);
        }else{
            // 如果start和end都不是null 进入else代码块
            // 我们要检查start时间是否在end时间之前
            // 如果要支持国际时间的判断,需要添加时区的修正
            if(end.toInstant(ZoneOffset.of("+8")).toEpochMilli()<
                start.toInstant(ZoneOffset.of("+8")).toEpochMilli()){
                // 如果结束时间小于起始时间,抛出异常,结束业务
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,"结束时间小于起始时间");
            }
        }

    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        return null;
    }


    public CsmallAuthenticationInfo getUserInfo(){
        // 获得SpringSecurity上下文(容器)对象
        UsernamePasswordAuthenticationToken authenticationToken=
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext()
                        .getAuthentication();
        if(authenticationToken==null){
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED,"没有登录信息");
        }
        // 如果authenticationToken不为空,获得其中的用户信息
        CsmallAuthenticationInfo csmallAuthenticationInfo=
                (CsmallAuthenticationInfo) authenticationToken.getCredentials();
        // 返回登录用户信息
        return csmallAuthenticationInfo;
    }
    // 业务逻辑层大多数方法都是需要获得用户Id,所以我们编写一个方法,专门返回当前登录用户的id
    public Long getUserId(){
        return getUserInfo().getId();
    }


}






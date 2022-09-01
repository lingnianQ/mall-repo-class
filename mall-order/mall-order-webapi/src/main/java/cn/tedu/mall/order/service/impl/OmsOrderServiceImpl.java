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
import org.apache.lucene.search.CollectionTerminatedException;
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

// 后期秒杀功能会调用这个生成订单的方法
@DubboService
@Service
@Slf4j
public class OmsOrderServiceImpl implements IOmsOrderService {

    @Autowired
    private OmsOrderMapper omsOrderMapper;
    @Autowired
    private OmsOrderItemMapper omsOrderItemMapper;
    @DubboReference
    private IForOrderSkuService dubboSkuService;
    @Autowired
    private IOmsCartService omsCartService;

    // 当前方法调用了product模块的数据库操作
    // 当有运行异常时,所有数据库操作都需要回滚,保证事务的原子性
    // 所以要激活分布式事务Seata的功能
    @GlobalTransactional
    @Override
    public OrderAddVO addOrder(OrderAddDTO orderAddDTO) {
        // 第一部分:收集信息,准备数据
        // OrderAddDTO对象中包含订单和订单项的集合
        // 我们要先获得其中订单的信息,做订单的新增
        OmsOrder order=new OmsOrder();
        // 同名属性赋值过去
        BeanUtils.copyProperties(orderAddDTO,order);
        // 可以对比OrderAddDTO和OmsOrder类的属性,发现若干属性需要我们来手动赋值
        // 以为赋值属性较多,建议单独编写一个方法处理
        loadOrder(order);
        // 到此为止,order赋值完成
        // 下面开始为orderAddDTO对象中的OrderItemAddDTO的集合转换和赋值
        // 首先检查它是不是null
        List<OrderItemAddDTO> itemAddDTOs=orderAddDTO.getOrderItems();
        if(itemAddDTOs==null || itemAddDTOs.isEmpty()){
            // 订单中的订单项如果为空,那么抛出异常,终止订单新增业务
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,
                    "订单中必须包含至少一件商品");
        }
        // 我们确认订单中有商品后,需要将现在itemAddDTOs集合中的对象转换为OmsOrderItem类型
        // 还需要将不存在的属性赋上值,最后才能进行数据库新增操作
        // 所以我们要实例化一个OmsOrderItem类型的集合,接收转换后的对象
        List<OmsOrderItem> omsOrderItems=new ArrayList<>();
        // 遍历itemAddDTOs
        for(OrderItemAddDTO addDTO : itemAddDTOs){
            // 还是先将当前遍历的addDTO对象的同名属性赋值给OmsOrderItem
            OmsOrderItem orderItem=new OmsOrderItem();
            BeanUtils.copyProperties(addDTO,orderItem);
            // 赋值id
            Long itemId=IdGeneratorUtils.getDistributeId("order_item");
            orderItem.setId(itemId);
            // 赋值orderid
            orderItem.setOrderId(order.getId());
            // 将赋好值的orderItem对象添加到集合中
            omsOrderItems.add(orderItem);
            // 第二部分:执行数据库操作
            // 1.减少sku库存
            // 获得skuId
            Long skuId=orderItem.getSkuId();
            // 执行减少库存的方法
            int rows=dubboSkuService.reduceStockNum(
                                            skuId,orderItem.getQuantity());
            // 判断执行修改影响的行数
            if(rows==0){
                log.warn("商品skuId:{},库存不足",skuId);
                // 库存不足导致运行失败,抛出异常
                // Seata会在抛出异常时终止事务
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,
                                "库存不足!");
            }
            // 2.删除购物车信息
            OmsCart omsCart=new OmsCart();
            omsCart.setUserId(order.getUserId());
            omsCart.setSkuId(skuId);
            // 执行删除操作
            omsCartService.removeUserCarts(omsCart);
        }
        // 3.新增订单
        // OmsOrderMapper调用新增订单的方法即可
        omsOrderMapper.insertOrder(order);
        // 4.新增订单项
        // 使用OmsOrderItemMapper调用新增订单项的方法
        omsOrderItemMapper.insertOrderItems(omsOrderItems);
        // 第三部分:收集返回值信息最终返回
        // 当前方法要求返回OrderAddVO类型
        OrderAddVO addVO=new OrderAddVO();
        // 给addVO各属性赋值
        addVO.setId(order.getId());
        addVO.setSn(order.getSn());
        addVO.setCreateTime(order.getGmtCreate());
        addVO.setPayAmount(order.getAmountOfActualPay());
        // 最后别忘了返回!!!!
        return addVO;
    }

    private void loadOrder(OmsOrder order) {
        // 针对order对象为空的属性,来赋值,保证新增成功
        // 给id赋值,我们使用Leaf分布式方式来赋值
        Long id= IdGeneratorUtils.getDistributeId("order");
        order.setId(id);

        // 赋值用户id
        order.setUserId(getUserId());

        // 赋值sn(订单号)
        order.setSn(UUID.randomUUID().toString());
        // 如果订单状态为null赋值为0
        if(order.getState()==null)
            order.setState(0);

        // 为了保证下单时间\创建时间\最后修改时间一致
        // 我们单独为他们赋值
        LocalDateTime now=LocalDateTime.now();
        order.setGmtOrder(now);
        order.setGmtCreate(now);
        order.setGmtModified(now);

        // 后端计算实际支付金额
        // 计算公式: 实际支付金额=原价+运费-优惠
        // 数据类型BigDecimal,是支持精确计算的类型
        BigDecimal price=order.getAmountOfOriginalPrice();
        BigDecimal freight=order.getAmountOfFreight();
        BigDecimal discount=order.getAmountOfDiscount();
        BigDecimal actualPay=price.add(freight).subtract(discount);
        // 将计算得到的实际支付金额,赋值到order
        order.setAmountOfActualPay(actualPay);

    }

    @Override
    public void updateOrderState(OrderStateUpdateDTO orderStateUpdateDTO) {

    }

    // 分页查询当前登录用户在指定时间范围内(默认一个月内)所有订单
    // 查询结果OrderListVO包含订单信息和该订单中的订单项(通过xml关联查询实现)
    @Override
    public JsonPage<OrderListVO> listOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO) {

        // 默认查询一个月内的所有订单,所有要判断参数中startTime和endTime是否为空
        // 还要判断startTime和endTime时间先后顺序是否合理
        validaTimeAndLoadTimes(orderListTimeDTO);
        // 获得用户Id
        Long userId=getUserId();
        // 将用户Id赋值到参数中
        orderListTimeDTO.setUserId(userId);
        // 设置分页条件
        PageHelper.startPage(orderListTimeDTO.getPage(),
                             orderListTimeDTO.getPageSize());
        // 执行关联查询,获得符合条件的订单和订单项
        List<OrderListVO> list=omsOrderMapper
                            .selectOrdersBetweenTimes(orderListTimeDTO);
        // 别忘了返回
        return JsonPage.restPage(new PageInfo<>(list));
    }



    private void validaTimeAndLoadTimes(OrderListTimeDTO orderListTimeDTO) {
        // 取出起始时间和结束时间
        LocalDateTime start=orderListTimeDTO.getStartTime();
        LocalDateTime end=orderListTimeDTO.getEndTime();
        // 为了不再添加业务的复杂度,当start和end有一个为null 就查进一个月的
        if(start==null || end==null){
            // 开始时间设置为现在减一个月
            start=LocalDateTime.now().minusMonths(1);
            end=LocalDateTime.now();
            // 赋值到参数中
            orderListTimeDTO.setStartTime(start);
            orderListTimeDTO.setEndTime(end);
        }else{
            // 如果start和end都非空
            // 要判断start实际是否在end之前
            // 如果要编写支持国际时区时间判断,要添加时区的修正
            if(end.toInstant(ZoneOffset.of("+8")).toEpochMilli()<
                start.toInstant(ZoneOffset.of("+8")).toEpochMilli()){
                // 如果结束时间小于开始时间,抛出异常,终止业务
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,
                        "结束时间应大于起始时间!");
            }
        }
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        return null;
    }

    // 业务逻辑层中获得用户信息的方法
    // 目标是从SpringSecurity上下文中获取由JWT解析而来的对象
    public CsmallAuthenticationInfo getUserInfo(){
        // 声明SpringSecurity上下文对象
        UsernamePasswordAuthenticationToken authenticationToken=
                (UsernamePasswordAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
        // 为了保险起见,判断一下这个对象是否为空
        if(authenticationToken==null){
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED,"没有登录");
        }
        // 从上下文中获取登录用户的信息
        // 这个信息是由JWT解析获得的
        CsmallAuthenticationInfo csmallAuthenticationInfo=
                (CsmallAuthenticationInfo) authenticationToken.getCredentials();
        // 返回登录信息
        return csmallAuthenticationInfo;
    }
    // 业务逻辑层大多数方法都是只需要用户的ID,所以专门编写一个方法返回id
    public Long getUserId(){
        return getUserInfo().getId();
    }
}

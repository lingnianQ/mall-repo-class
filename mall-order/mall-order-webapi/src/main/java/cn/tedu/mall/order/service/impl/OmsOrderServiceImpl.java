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
import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.dto.OrderStateUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.order.vo.OrderDetailVO;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import cn.tedu.mall.product.service.order.IForOrderSkuService;
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

        return null;
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
            order.setId(getUserId());
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

    @Override
    public void updateOrderState(OrderStateUpdateDTO orderStateUpdateDTO) {

    }

    @Override
    public JsonPage<OrderListVO> listOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO) {
        return null;
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






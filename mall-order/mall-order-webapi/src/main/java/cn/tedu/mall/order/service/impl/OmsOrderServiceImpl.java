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

// 后期秒杀业务也需要生成订单,可以直接调用当前类中的方法
@DubboService
@Service
@Slf4j
public class OmsOrderServiceImpl implements IOmsOrderService {

    @Autowired
    private OmsOrderMapper omsOrderMapper;
    @Autowired
    private OmsOrderItemMapper omsOrderItemMapper;
    @Autowired
    private IOmsCartService omsCartService;
    @DubboReference
    private IForOrderSkuService dubboSkuService;

    // 新增订单的方法
    // 这个方法调用了product模块的数据库操作功能,
    // 运行发送异常时,必须依靠分布式事务组件(seata)进行回滚,以保证事务的原子性
    // 我们要利用注解激活Seata的分布式事务功能
    @GlobalTransactional
    @Override
    public OrderAddVO addOrder(OrderAddDTO orderAddDTO) {
        // 第一部分:收集信息,准备数据
        // 先实例化OmsOrder对象
        OmsOrder order=new OmsOrder();
        // 将当前方法参数OrderAddDTO类型对象的同名属性赋值给OmsOrder对象
        BeanUtils.copyProperties(orderAddDTO,order);
        // orderAddDTO中的属性较少,order对象还有一些属性不能被赋值,需要我们手动计算或赋值
        // 我们可以专门编写一个方法,在这个方法中处理
        loadOrder(order);


        // 第二部分:执行数据库操作

        // 第三部分:返回订单信息给前端
        return null;
    }

    private void loadOrder(OmsOrder order) {
        // 本方法针对order对象未被赋值的属性,进行手动赋值
        // 给订单id赋值,我们使用Leaf分布式方式来赋值
        Long id= IdGeneratorUtils.getDistributeId("order");
        order.setId(id);

        // 赋值用户id
        // 以后做秒杀时,用户id会被赋值,所以这里要判断一下,没有用户id再为其赋值
        if(order.getUserId()==null){
            // getUserId方法是从SpringSecurity上下文中获得的用户id
            order.setUserId(getUserId());
        }

        // 赋值订单号
        // 使用随机的UUID做订单号即可
        order.setSn(UUID.randomUUID().toString());
        // 为订单状态赋值
        // 如果订单状态为null,默认赋值为0
        if(order.getState()==null){
            order.setState(0);
        }
        // 为了保证下单时间\数据创建时间\最后修改时间一致
        // 我们在这里为它们赋相同的值
        LocalDateTime now=LocalDateTime.now();
        order.setGmtOrder(now);
        order.setGmtCreate(now);
        order.setGmtModified(now);

        // 计算实际支付金额
        // 计算公式: 实际支付金额=原价-优惠+运费
        // 数据类型使用BigDecimal,是没有浮点偏移的精确计算
        BigDecimal price=order.getAmountOfOriginalPrice();
        BigDecimal freight=order.getAmountOfFreight();
        BigDecimal discount=order.getAmountOfDiscount();
        BigDecimal actualPay=price.subtract(discount).add(freight);
        // 最后将计算完成的实际支付金额赋值给order
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
        // 编码获得SpringSecurity上下文中保存的权限
        UsernamePasswordAuthenticationToken authenticationToken=
                (UsernamePasswordAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
        // 为了保险起见,判断一下从SpringSecurity中获得的信息是不是null
        if(authenticationToken == null){
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED,
                    "请您先登录!");
        }
        // 上下文信息确定存在后,获取其中的用户信息
        // 这个信息就是有JWT解析获得的
        CsmallAuthenticationInfo csmallAuthenticationInfo=
                (CsmallAuthenticationInfo) authenticationToken.getCredentials();
        // 返回登录信息
        return csmallAuthenticationInfo;
    }
    // 业务逻辑层大多数方法需要用户的信息实际上就是用户的ID,编写一个只返回用户ID的方法方便调用
    public Long getUserId(){
        return getUserInfo().getId();
    }


}

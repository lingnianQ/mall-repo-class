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
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 订单模块
 * 秒杀需要调用该模块
 *
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/3 18:40
 */
@DubboService
@Service
@Slf4j
public class OmsOrderServiceImpl implements IOmsOrderService {

    @DubboReference
    private IForOrderSkuService dubboSkuService;
    @Autowired
    private IOmsCartService omsCartService;
    @Autowired
    private OmsOrderMapper omsOrderMapper;
    @Autowired
    private OmsOrderItemMapper omsOrderItemMapper;


    /**
     * 新增订单
     * 这个方法dubbo调用了Product模块的方法,操作了数据库,有分布式的事务需求
     * 所以要使用注解激活Seata分布式事务的功能
     *
     * @param orderAddDTO
     * @return 订单编号
     */
    @GlobalTransactional
    @Override
    public OrderAddVO addOrder(OrderAddDTO orderAddDTO) {
        //1.为order赋值
        OmsOrder order = new OmsOrder();
        BeanUtils.copyProperties(orderAddDTO, order);
        loadOrder(order);
        //2.为orderItem集合 赋值
        List<OrderItemAddDTO> itemAddDTOS = orderAddDTO.getOrderItems();
        if (itemAddDTOS == null || itemAddDTOS.isEmpty()) {
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST, "订单中至少包含一件商品");
        }
        //List<OrderItemAddDTO>转换为List<OmsOrderItem>
        List<OmsOrderItem> omsOrderItemList = new ArrayList<>();
        for (OrderItemAddDTO itemAddDTO : itemAddDTOS) {
            //2.为orderItem集合 赋值
            OmsOrderItem orderItem = new OmsOrderItem();
            BeanUtils.copyProperties(itemAddDTO, orderItem);
            Long itemId = IdGeneratorUtils.getDistributeId("order_item");
            orderItem.setId(itemId);
            orderItem.setOrderId(order.getId());
            omsOrderItemList.add(orderItem);

            // 第二部分:执行操作数据库的指令
            // 当前循环是订单中的一件商品,我们可以在此处对这个商品进行库存的减少
            // 当前对象属性中是包含skuId和要购买的商品数量的,所以可以执行库存的修改
            // 1.减少库存
            // 先获取skuId
            Long skuId = orderItem.getSkuId();
            int rows = dubboSkuService.reduceStockNum(skuId, orderItem.getQuantity());
            if (rows == 0) {
                log.warn("商品库存不足,skuId:{}", skuId);
                // 库存不足不能继续生成订单,抛出异常,终止事务进行回滚
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,
                        "库存不足!");
            }

            //移除购物车
            OmsCart cart = new OmsCart();
            cart.setUserId(order.getUserId());
            cart.setSkuId(skuId);
            omsCartService.removeUserCarts(cart);
        }
        //3.新增订单
        omsOrderMapper.insertOrder(order);
        //4.新增订单列表项
        omsOrderItemMapper.insertOrderItemList(omsOrderItemList);

        //最后生成返回值
        OrderAddVO orderAddVO = new OrderAddVO();
        orderAddVO.setId(order.getId());
        orderAddVO.setSn(order.getSn());
        orderAddVO.setCreateTime(order.getGmtCreate());
        orderAddVO.setPayAmount(order.getAmountOfActualPay());

        return orderAddVO;
    }

    /**
     * 为Order对象补全属性值的方法
     *
     * @param order
     */
    private void loadOrder(OmsOrder order) {
        //设置id
        Long id = IdGeneratorUtils.getDistributeId("order");
        order.setId(id);

        //生成订单号
        order.setSn(UUID.randomUUID().toString());

        //赋值userId
        if (order.getUserId() == null) {
            order.setId(getUserId());
        }

        //为订单状态赋值--0表示未支付
        if (order.getState() == null) {
            order.setState(0);
        }

        //为下单时间,.创建时间,,修改时间赋值
        LocalDateTime now = LocalDateTime.now();
        order.setGmtOrder(now);
        order.setGmtCreate(now);
        order.setGmtModified(now);

        //计算实际支付金额
        BigDecimal price = order.getAmountOfOriginalPrice();
        BigDecimal freight = order.getAmountOfFreight();
        BigDecimal discount = order.getAmountOfDiscount();
        BigDecimal actualPay = price.add(freight).subtract(discount);
        order.setAmountOfActualPay(actualPay);

    }

    /**
     * 更新订单状态
     *
     * @param orderStateUpdateDTO
     */
    @Override
    public void updateOrderState(OrderStateUpdateDTO orderStateUpdateDTO) {

    }

    /**
     * 根据起始结束时间查询订单列表
     *
     * @param orderListTimeDTO
     */
    @Override
    public JsonPage<OrderListVO> listOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO) {
        return null;
    }

    /**
     * 根据sn查询订单详细信息
     *
     * @param id
     * @return
     */
    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        return null;
    }

    /**
     * 业务逻辑层中有获得当前登录用户信息的需求
     * 我们的项目会在控制器方法运行前运行的过滤器中,解析前端传入的JWT
     * 将解析获得的用户信息保存在SpringSecurity上下文中
     * 这里可以编写方法从SpringSecurity上下文中获得用户信息
     *
     * @return csmallAuthenticationInfo
     */
    public CsmallAuthenticationInfo getUserInfo() {
        // 编写SpringSecurity上下文中获得用户信息的代码
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
        // 为了逻辑严谨性,判断一下SpringSecurity上下文中的信息是不是null
        if (authenticationToken == null) {
            throw new CoolSharkServiceException(
                    ResponseCode.UNAUTHORIZED, "您没有登录!");
        }
        // 确定authenticationToken不为null
        // 就可以从中获得用户信息了
        CsmallAuthenticationInfo csmallAuthenticationInfo =
                (CsmallAuthenticationInfo) authenticationToken.getCredentials();
        // 别忘了返回
        return csmallAuthenticationInfo;
    }

    /**
     * 业务逻辑层中的方法实际上都只需要用户的id即可
     * 我们可以再编写一个方法,从用户对象中获得id
     *
     * @return getUserInfo().getId();
     */
    public Long getUserId() {
        return getUserInfo().getId();
    }

}

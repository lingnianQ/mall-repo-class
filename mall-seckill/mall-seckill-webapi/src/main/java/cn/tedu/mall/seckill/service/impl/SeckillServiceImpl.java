package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderItemAddDTO;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.model.Success;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.config.RabbitMqComponentConfiguration;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/16 17:39
 */
@Service
public class SeckillServiceImpl implements ISeckillService {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @DubboReference
    private IOmsOrderService dobboOrderService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 1.判断用户是否为重复购买和Redis中该Sku是否有库存
     * 2.秒杀订单转换成普通订单,需要使用dubbo调用order模块的生成订单方法
     * 3.使用消息队列(RabbitMQ)将秒杀成功记录信息保存到success表中
     * 4.秒杀订单信息返回
     *
     * @param seckillOrderAddDTO
     * @return
     */
    @Override
    public SeckillCommitVO commitSeckill(SeckillOrderAddDTO seckillOrderAddDTO) {
        //第一阶段
        Long skuId = seckillOrderAddDTO.getSeckillOrderItemAddDTO().getSkuId();
        Long userId = getUserId();
        String reseckillCheckKey = SeckillCacheUtils.getReseckillCheckKey(skuId, userId);
        Long seckillTimes = stringRedisTemplate
                .boundValueOps(reseckillCheckKey).increment();
        //seckillTimes设置为1,表示该用户只可以购买一次
        if (seckillTimes > 100) {
            // 抛出异常,提示不能重复购买,终止程序
            throw new CoolSharkServiceException(ResponseCode.FORBIDDEN,
                    "您已经购买过这个商品了,谢谢您的支持!");
        }

        String skuStockKey = SeckillCacheUtils.getStockKey(skuId);
        Long leftStock = stringRedisTemplate
                .boundValueOps(skuStockKey).decrement();
        if (leftStock < 0) {
            stringRedisTemplate.boundValueOps(reseckillCheckKey).decrement();
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,
                    "对不起,您要购买的商品暂时售罄");
        }
        //第二阶段
        OrderAddDTO orderAddDTO = convertSeckillOrderToOrder(seckillOrderAddDTO);
        orderAddDTO.setUserId(userId);
        OrderAddVO orderAddVO = dobboOrderService.addOrder(orderAddDTO);

        Success success = new Success();
        // Success大部分属性和秒杀sku信息重叠,可以使用秒杀订单项对象,给他同名属性赋值
        BeanUtils.copyProperties(seckillOrderAddDTO.getSeckillOrderItemAddDTO(),
                success);
        // 把缺失的信息补齐
        success.setUserId(userId);
        success.setOrderSn(orderAddVO.getSn());
        success.setSeckillPrice(
                seckillOrderAddDTO.getSeckillOrderItemAddDTO().getPrice());
        rabbitTemplate.convertAndSend(
                RabbitMqComponentConfiguration.SECKILL_EX,
                RabbitMqComponentConfiguration.SECKILL_RK,
                success
        );
        SeckillCommitVO commitVO = new SeckillCommitVO();
        BeanUtils.copyProperties(orderAddVO, commitVO);

        return commitVO;
    }

    private OrderAddDTO convertSeckillOrderToOrder(SeckillOrderAddDTO seckillOrderAddDTO) {
        OrderAddDTO orderAddDTO = new OrderAddDTO();
        BeanUtils.copyProperties(seckillOrderAddDTO, orderAddDTO);

        OrderItemAddDTO orderItemAddDTO = new OrderItemAddDTO();
        BeanUtils.copyProperties(seckillOrderAddDTO.getSeckillOrderItemAddDTO(), orderItemAddDTO);

        List<OrderItemAddDTO> list = new ArrayList<>();
        list.add(orderItemAddDTO);
        orderAddDTO.setOrderItems(list);

        return orderAddDTO;
    }


    /**
     * 解析jwt
     *
     * @return
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

    // 业务逻辑层中的方法实际上都只需要用户的id即可
    // 我们可以再编写一个方法,从用户对象中获得id
    public Long getUserId() {
        return getUserInfo().getId();
    }
}

package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.service.ISeckillService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SeckillServiceImpl implements ISeckillService {

    // 需要dubbo调用mall_order的生成普通订单的方法
    @DubboReference
    private IOmsOrderService dubboOrderService;
    // 要用Redis减少库存,库存数是字符串类型的数字
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    // 秒杀成功后需要将秒杀成功信息保存到数据库,但是使用消息队列
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /*
    1.要判断当前用户是否为重复购买
    2.从Redis中判断是否有库存
    3.秒杀订单转换成普通订单,需要使用dubbo在order模块完成
    4.用消息队列(RabbitMQ)的方式将秒杀成功信息保存在success表中
     */

    @Override
    public SeckillCommitVO commitSeckill(SeckillOrderAddDTO seckillOrderAddDTO) {
        // 第一阶段:利用redis检查重复购买和是否有库存
        // 从方法参数的订单项中获得skuId
        Long skuId=seckillOrderAddDTO.getSeckillOrderItemAddDTO().getSkuId();



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

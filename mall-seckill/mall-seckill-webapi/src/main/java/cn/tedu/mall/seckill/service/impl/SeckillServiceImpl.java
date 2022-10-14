package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
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

    // 秒杀业务中,使用redis都是在判断数值,直接使用字符串类型的Redis对象即可
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    // 需要Dubbo调用mall_order模块生成普通订单的业务逻辑方法
    @DubboReference
    private IOmsOrderService dubboOrderService;
    // 我们的业务中要将秒杀成功的信息发送给消息队列,所以准备发送消息的对象
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
        // 第一阶段:利用redis检查重复购买和库存数
        // 从方法参数中获得要购买的商品skuId
        Long skuId=seckillOrderAddDTO.getSeckillOrderItemAddDTO().getSkuId();
        // 从SpringSecurity中获得用户id
        Long userId=getUserId();
        // 这样就明确了是哪个userId要购买哪个skuId
        // 秒杀业务规定,每个用户只能购买每个skuId一次
        // 所以我们可以根据当前userId和skuId进行重复购买的检查
        // 先获得检查重复购买的Key
        // mall:seckill:reseckill:[skuId]:[userId]
        String reSeckillCheckKey= SeckillCacheUtils
                        .getReseckillCheckKey(skuId,userId);
        // 使用上面生成的key来调用redis的increment()方法
        // increment是增长的意思
        // 1.如果当前key不存在,redis会创建这个key,并保存它的值为1
        // 2.如果当前key存在,redis会在当前值的基础上加1 ,例如现在值为1,运行后会变为2
        // 3.运行完上面的操作,会将最终的值返回
        // 也就是说如果返回的值为1,表示这个用户没有成功购买过这个商品
        Long seckillTimes=
             stringRedisTemplate.boundValueOps(reSeckillCheckKey).increment();
        // 如果这个秒杀次数值大于1,表示这个用户已经购买过这个商品
        if(seckillTimes>1){
            // 不允许重复购买,抛出异常终止程序
            throw new CoolSharkServiceException(ResponseCode.FORBIDDEN,
                    "您已经购买过该商品了,本商城秒杀禁止重复购买");
        }
        // 程序运行到此处,表示用户没有购买过这个商品
        // 开始检查库存
        // 获得指定skuId库存数的key
        // mall:seckill:sku:stock:1
        String seckillSkuCountKey=SeckillCacheUtils.getStockKey(skuId);
        // 根据当前key获取redis中保存的sku库存数
        // 使用decrement()方法,将当前库存数-1之后返回
        Long leftStock=stringRedisTemplate
                .boundValueOps(seckillSkuCountKey).decrement();
        // leftStock是用户购买后剩余库存数
        // 只有leftStock小于0时才表示售罄了


        // 第二阶段:将秒杀订单转换为普通订单

        // 第三阶段:秒杀成功信息消息队列的发送

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

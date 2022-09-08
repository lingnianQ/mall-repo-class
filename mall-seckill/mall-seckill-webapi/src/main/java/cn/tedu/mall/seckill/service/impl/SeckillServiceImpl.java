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
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
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
        Long userId=getUserId();
        // 有了userId和skuId相当于知道了谁买了什么
        // 秒杀业务规定,一个用户只能购买一个skuId的商品一次
        // 所以我们可以根据userId和skuId判断当前用户是否已经购买过
        // 先获得该用户对该商品购买的key
        // mall:seckill:reseckill:[skuId]:[userId]
        String reseckillCheckKey=
            SeckillCacheUtils.getReseckillCheckKey(skuId,userId);
        // 使用上面生成的Key利用Redis的功能调用increment()方法
        // increment是增长的意思,方法效果如下
        // 1.如果当前的key不存在,redis会创建这个key,并保存他的值为1
        // 2.如果当前的key存在,redis会给当前值加1,例如现在值是1,运行方法后会变为2
        // 3.方法会将新增之后的值返回
        Long seckillTimes=
            stringRedisTemplate.boundValueOps(reseckillCheckKey).increment();
        // seckillTimes实际就是当前用户第几次购买这个商品了
        if(seckillTimes>1){
            // 购买次数超过1,证明已经购买过,终止业务,抛出异常
            throw new CoolSharkServiceException(ResponseCode.FORBIDDEN,
                            "您已经购买过该商品了!");
        }
        // 程序运行到此处,表示当前用户是第一次购买该商品
        // 下面检查是否有库存
        // 获得库存数的key
        String seckillSkuCountKey=SeckillCacheUtils.getStockKey(skuId);
        // 根据当前key从redis中获得库存数
        // 使用decrement()方法,将当前skuId商品库存-1之后返回
        Long leftStock=stringRedisTemplate
                .boundValueOps(seckillSkuCountKey).decrement();
        // leftStock是当前用户购买后剩余的库存数
        // 如果leftStock值为0,表示当前用户购买到了最后一件商品
        // 所以判断leftStock<0时,才是没有货的情况
        if(leftStock<0){
            // 将当前用户购买此商品的次数减为0
            stringRedisTemplate.boundValueOps(reseckillCheckKey).decrement();
            // 因为库存不足抛出异常
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,
                    "对不起您要购买的商品已经售罄");
        }
        // 运行到此处表示当前用户是第一次购买该商品,同时商品还有货
        // 下面进入业务的第二阶段:将秒杀订单seckillOrderAddDTO转换成普通订单OrderAddDTO
        // 所以我们专门编写一个方法进行转换操作
        OrderAddDTO orderAddDTO=convertSeckillOrderToOrder(seckillOrderAddDTO);
        // 上面的转换完成,所有订单信息都已经被赋值,但是用户Id不会被赋值
        // 为了能够保证订单顺利新增,必须为userId赋值
        orderAddDTO.setUserId(userId);
        // 信息完成完整后直接调用dubbo实现新增订单的功能
        OrderAddVO orderAddVO=dubboOrderService.addOrder(orderAddDTO);
        // 第三部分:将秒杀成功信息发送到RabbitMQ中进行进一步处理
        // 我们向RabbitMQ中发送消息的目标是向sucesss表中添加数据,保存秒杀成功信息
        // 保存秒杀成功信息是典型的不急迫运行的操作
        // 能够容忍延迟,特别适合消息队列的操作
        // 在并发高时,进行削峰填谷,在不忙时再进行操作
        // 实例化Success对象
        Success success=new Success();
        // Success对象中有大多数属性来自sku实体,秒杀订单项就是对sku实体的描述
        // 可以将秒杀订单项实体的同名属性赋值给success
        BeanUtils.copyProperties(
                seckillOrderAddDTO.getSeckillOrderItemAddDTO(),success);
        // 补全缺少的信息
        success.setUserId(userId);
        success.setOrderSn(orderAddVO.getSn());
        success.setSeckillPrice(
                seckillOrderAddDTO.getSeckillOrderItemAddDTO().getPrice());
        // 未完待续...

        // 将新增订单获得的orderAddVO对象转换为SeckillCommitVO后返回
        SeckillCommitVO commitVO=new SeckillCommitVO();
        BeanUtils.copyProperties(orderAddVO,commitVO);
        // 返回commitVO!!!!!
        return commitVO;
    }
    private OrderAddDTO convertSeckillOrderToOrder(SeckillOrderAddDTO seckillOrderAddDTO) {
        // 实例化返回值类型对象
        OrderAddDTO orderAddDTO=new OrderAddDTO();
        // 将参数秒杀订单对象中同名属性赋值到OrderAddDTO
        BeanUtils.copyProperties(seckillOrderAddDTO,orderAddDTO);
        // seckillOrderAddDTO对象包含秒杀订单项对象seckillOrderItemAddDTO
        // 但是普通订单orderAddDTO对象包含的是普通订单项的集合List<OrderItemAddDTO>
        // 所以我们要将秒杀订单项转换为普通订单项,再将这个普通订单项保存到上面集合中
        // 实例化一个普通订单项对象
        OrderItemAddDTO orderItemAddDTO=new OrderItemAddDTO();
        BeanUtils.copyProperties(
            seckillOrderAddDTO.getSeckillOrderItemAddDTO(),orderItemAddDTO);
        // 实例化普通订单项的集合,用于赋值到orderAddDTO对象的orderItems属性
        List<OrderItemAddDTO> orderItemAddDTOs=new ArrayList<>();
        orderItemAddDTOs.add(orderItemAddDTO);
        // 向orderAddDTO的orderItems属性赋值
        orderAddDTO.setOrderItems(orderItemAddDTOs);
        // 到此为止orderAddDTO是既包含了订单信息,又包含了订单项信息的对象
        return orderAddDTO;
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

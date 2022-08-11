package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderItemAddDTO;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
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

    // 需要普通订单生成的方法,Dubbo调用的
    @DubboReference
    private IOmsOrderService dubboOrderService;
    // 减少sku库存数的redis对象,是操作字符串的
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    // 秒杀订单提交成功后,消息队列负责记录秒杀成功的信息
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
        // 第一阶段:利用redis检查库存数和检查是否重复购买
        // 先来获得用户Id和要购买商品的SkuId
        Long skuId=seckillOrderAddDTO.getSeckillOrderItemAddDTO().getSkuId();
        Long userId=getUserId();
        // 我们可以根据userId和skuId的组合来确定谁买了什么商品
        // 秒杀业务规定,一个用户id只能购买一个skuId一次
        // 我们利用用户的id和skuId生成一个key
        // 将这个key保存在redis中,表示当前用户已经购买过
        // mall:seckill:reseckill:1:2
        String reSeckillKey= SeckillCacheUtils.getReseckillCheckKey(skuId,userId);
        // 向Redis中保存这个key的信息可以使用increment()方法
        // increment()效果如下
        // 1.如果当前key不存在,redis会创建这个key,并保存它的值为1
        // 2.如果当前key存在,redis会给当前key的值加1,例如现在是1的话,运行之后的值变成2
        // 3.最后会讲当前key的值返回给调用者
        Long seckillTimes=stringRedisTemplate.boundValueOps(reSeckillKey).increment();
        // 如果seckillTimes值大于1,表示之前已经购买过
        if(seckillTimes>1){
            // 购买次数超过1,证明不是第一次购买,终止业务,抛出异常
            throw new CoolSharkServiceException(
                                ResponseCode.FORBIDDEN,"您已经购买过该商品");
        }
        // 程序运行到此处,表示用户是第一次购买
        // 下面检查是否有库存
        // 先获得当前商品的库存key
        String seckillSkuCountKey=SeckillCacheUtils.getStockKey(skuId);
        // 从Redis中获得库存数
        // 使用decrement()方法,将当前库存数减1后返回
        Long leftStock=stringRedisTemplate
                        .boundValueOps(seckillSkuCountKey).decrement();
        // leftStock表示当前用户购买之后的剩余库存数
        // 如果是0,表示当前用户购买完之后为0,所以leftStock为负值时才是没有库存
        if(leftStock<0){
            // 删除用户购买记录
            stringRedisTemplate.boundValueOps(reSeckillKey).decrement();
            // 异常:库存不足
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,
                                                "对不起您购买的商品已经无货");
        }
        // 运行到此处证明用户是第一次购买,而且还有库存
        // 进入第二阶段:将秒杀订单SeckillOrderAddDTO转换成普通订单orderAddDTO
        // 我们确定要做的就是将SeckillOrderAddDTO的同名属性赋值到orderAddDTO对象中
        // 而且还有将SeckillOrderAddDTO中订单项的属性赋值给orderAddDTO的订单项集合中
        // 所以我们为这个转换单独编写个方法,减少当前方法的代码量
        OrderAddDTO orderAddDTO=convertSeckillOrderToOrder(seckillOrderAddDTO);

        return null;
    }
    private OrderAddDTO convertSeckillOrderToOrder(SeckillOrderAddDTO seckillOrderAddDTO) {
        // 实例化OrderAddDTO
        OrderAddDTO orderAddDTO=new OrderAddDTO();
        // 赋值同名属性值
        BeanUtils.copyProperties(seckillOrderAddDTO,orderAddDTO);
        // seckillOrderAddDTO对象包含seckillOrderItemAddDTO订单项对象
        // 而orderAddDTO包含的是OrderItemAddDTO泛型的List集合
        // 我们要做的是将seckillOrderItemAddDTO转换为OrderItemAddDTO并保存到集合中
        OrderItemAddDTO orderItemAddDTO=new OrderItemAddDTO();
        BeanUtils.copyProperties(seckillOrderAddDTO.getSeckillOrderItemAddDTO(),
                                orderItemAddDTO);
        // 实例化一个List集合,将赋好值的orderItemAddDTO新增到集合中
        List<OrderItemAddDTO> orderItemAddDTOs=new ArrayList<>();
        orderItemAddDTOs.add(orderItemAddDTO);
        // 最后将集合赋值到orderAddDTO对象的orderItems属性中
        orderAddDTO.setOrderItems(orderItemAddDTOs);
        // 转换完成,将最终包含所有信息的orderAddDTO对象返回
        return orderAddDTO;
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

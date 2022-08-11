package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.service.ISeckillService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
    1.从Redis中判断是否有库存
    2.要判断当前用户是否为重复购买
    3.秒杀订单转换成普通订单,需要使用dubbo在order模块完成
    4.用消息队列(RabbitMQ)的方式将秒杀成功信息保存在success表中
     */

    @Override
    public SeckillCommitVO commitSeckill(SeckillOrderAddDTO seckillOrderAddDTO) {
        return null;
    }
}

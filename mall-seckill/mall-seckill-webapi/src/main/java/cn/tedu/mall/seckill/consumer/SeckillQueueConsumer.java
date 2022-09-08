package cn.tedu.mall.seckill.consumer;

import cn.tedu.mall.pojo.seckill.model.Success;
import cn.tedu.mall.seckill.config.RabbitMqComponentConfiguration;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.mapper.SuccessMapper;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 将这个对象保存到Spring容器
@Component
// RabbitMQ监听器声明监听的队列
@RabbitListener(queues = {RabbitMqComponentConfiguration.SECKILL_QUEUE})
public class SeckillQueueConsumer {
    // 业务需要减少秒杀sku的库存并且新增success到数据库
    @Autowired
    private SuccessMapper successMapper;
    @Autowired
    private SeckillSkuMapper skuMapper;

    // 当前方法中,标记用于接收消息队列信息,并处理的方法
    @RabbitHandler
    public void process(Success success){
        // 先减少库存
        skuMapper.updateReduceStockBySkuId(
                success.getSkuId(),success.getQuantity());
        //  新增success对象到数据库
        successMapper.saveSuccess(success);

        // 如果上面的数据库操作发生异常
        // 我们可以设计捕获这个异常,在异常梳理方法中向RabbitMQ死信队列发送消息
        // 发送消息到死信队列作为处理异常的最后手段使用,人工处理

    }



}

package cn.tedu.mall.seckill.consumer;

import cn.tedu.mall.pojo.seckill.model.Success;
import cn.tedu.mall.seckill.config.RabbitMqComponentConfiguration;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.mapper.SuccessMapper;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

// 必须保存到Spring容纳中
@Component
// RabbitMQ监听器声明
@RabbitListener(queues = {RabbitMqComponentConfiguration.SECKILL_QUEUE})
public class SeckillQueueConsumer {
    //业务需要的Mapper对象装配
    @Autowired
    private SuccessMapper successMapper;
    @Autowired
    private SeckillSkuMapper skuMapper;
    // 当前类上标记的队列收到消息时
    // 下面方法会接收这个消息,自动运行
    @RabbitHandler
    public void process(Success success){
        // 先减库存
        // 减少seckill_sku表中的库存数并不迫切,运行可能延迟,真正运行时,秒杀可能已经结束了
        // 这个库存的减少操作也不会影响秒杀过程中redis的库存数
        skuMapper.updateReduceStockBySkuId(success.getSkuId(),success.getQuantity());
        // 新增success对象到数据库
        successMapper.saveSuccess(success);

        // 如果上面操作数据库发生异常
        // 我们可以设计再向RabbitMq发送消息来处理
        // 最后手段就是将这个信息发送给死信队列,人工处理

    }



}

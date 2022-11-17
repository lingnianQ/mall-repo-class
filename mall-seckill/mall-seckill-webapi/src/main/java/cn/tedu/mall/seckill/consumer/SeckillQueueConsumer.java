package cn.tedu.mall.seckill.consumer;

import cn.tedu.mall.pojo.seckill.model.Success;
import cn.tedu.mall.seckill.config.RabbitMqComponentConfiguration;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.mapper.SuccessMapper;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitMqComponentConfiguration.SECKILL_QUEUE)
public class SeckillQueueConsumer {
    @Autowired
    private SeckillSkuMapper skuMapper;
    @Autowired
    private SuccessMapper successMapper;

    // 下面开始编写接收消息队列中消息的方法
    @RabbitHandler
    public void process(Success success){
        // 先减少库存
        skuMapper.updateReduceStockBySkuId(
                success.getSkuId(),success.getQuantity());
        // 新增success对象到数据库
        successMapper.saveSuccess(success);
        // 如果上面两个数据库操作发生异常
        // 可能会引发事务问题,如果统计的不需要非常精确,不处理也可以
        // 如果需要精确的需求,发生异常时,可以将错误信息发送给死信队列,由人工处理
        // 死信队列是最后的办法,实际开发中慎用

    }
}

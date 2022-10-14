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
    // 下面方法会在消息队列中有出现消息时自动运行
    @RabbitHandler
    public void process(Success success){
        // 先减少库存
        skuMapper.updateReduceStockBySkuId(
                success.getSkuId(),success.getQuantity());
        // 新增Success对象到数据库
        successMapper.saveSuccess(success);

        // 上面两个数据库操作发送异常,可能会引发事务问题
        // 我们可以编写异常处理方法,将消息发送给死信队列,后续人工处理
        // 死信队列的办法是最后手段,不要频繁使用
    }

}

package cn.tedu.mall.seckill.timer.job;

import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
public class SeckillInitialJob implements Job {
    @Autowired
    private SeckillSkuMapper skuMapper;
    @Autowired
    private SeckillSpuMapper spuMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    /*
    RedisTemplate对象在保存数据到Redis时,会将当前数据序列化后保存
    这样做的好处是将序列化后的数据保存到Redis,读写效率高,缺点是不能在Redis中修改数据
    我们现在要预热的信息包含sku的库存数,这个库存数如果也用上面的序列化的方式保存
    就会因为高并发情况下的线程安全问题引发"超卖"
    解决方案,我们需要一个能够直接在Redis中减少库存的方法来避免超卖的发生
    SpringDataRedis提供一个可以直接在Redis中操作数值的对象:StringRedisTemplate
    使用StringRedisTemplate向Redis中保存数据,数据都会以字符串的方式保存
    又因为Redis可以直接操作数值类型的字符串,所以可以通过它实现直接修改库存数
     */



    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    }
}

package cn.tedu.mall.seckill.timer.job;

import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
public class SeckillInitialJob implements Job {

    // 查询秒杀sku库存数的mapper
    @Autowired
    private SeckillSkuMapper skuMapper;
    // 需要查询秒杀spu相关的信息
    @Autowired
    private SeckillSpuMapper spuMapper;
    // 操作Redis的对象
    @Autowired
    private RedisTemplate redisTemplate;
    /*
    RedisTemplate对象在保存数据到Redis时,会先将当前数据序列化后保存
    这样做的好处是序列化后的数据保存到Redis读写效率更高,缺点是不能在Redis中修改数据
    我们现在要预热的是秒杀sku库存数,这个库存数如果也用上面的redisTemplate保存的方式
    就容易在高并发情况下由于线程安全问题导致"超卖"
    解决方案就是我们需要创建一个能够直接在Redis中修改数据的对象,来避免超卖的发生
    SpringDataRedis提供了一个可以直接在Redis中操作数值的对象:StringRedisTemplate
    使用StringRedisTemplate向Redis保存数据,默认都会以字符串的方式保存(不序列化了)
    又因为Redis支持直接将字符串类型的数值进行修改,所以可以将库存数保存并直接修改
    这样就不需要写java代码读取和修改了,直接使用Redis内部的功能修改库存数
    最后结合Redis天生单线程的特性,避免线程安全问题方式超卖
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplatel;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {




    }
}

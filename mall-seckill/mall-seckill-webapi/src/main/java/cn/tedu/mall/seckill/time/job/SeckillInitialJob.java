package cn.tedu.mall.seckill.time.job;

import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
public class SeckillInitialJob implements Job {
    @Autowired
    private SeckillSpuMapper spuMapper;
    @Autowired
    private SeckillSkuMapper skuMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    // RedisTemplate对象在保存对象时,会将对象序列化为二进制格式
    // 再将二进制数据保存在Redis中,这样做读写速度快,缺点是不能在Redis中修改数据
    // 但是我们修改库存的时候,需要直接在Redis中进行修改才能避免超卖现象
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    }
}

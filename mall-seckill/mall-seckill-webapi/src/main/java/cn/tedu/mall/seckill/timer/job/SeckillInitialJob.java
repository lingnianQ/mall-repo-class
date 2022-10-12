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


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    }
}

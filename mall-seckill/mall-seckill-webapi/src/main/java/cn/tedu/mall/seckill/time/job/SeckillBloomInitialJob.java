package cn.tedu.mall.seckill.time.job;

import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.utils.RedisBloomUtils;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

public class SeckillBloomInitialJob implements Job {
    @Autowired
    private RedisBloomUtils redisBloomUtils;
    @Autowired
    private SeckillSpuMapper seckillSpuMapper;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 首先确定保存布隆过滤器的批次的key
        // 我们这里设计添加两个秒杀批次的布隆过滤器
        // 避免两个批次之间瞬间的空档期
        // 而且也允许让用户看到下一个批次的商品
        // "spu:bloom:filter:2022-08-15"
        String bloomTodayKey= SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        String bloomTomorrowKey= SeckillCacheUtils.getBloomFilterKey(
                                                        LocalDate.now().plusDays(1));
        // 实际开发中要到数据库中根据秒杀时间(秒杀批次)查询对应SpuId的集合
        // 学习过程中,我们只能将全部的商品保存在布隆过滤器中
        // 所以我们查询当前秒杀spu表中所有spuId的集合
        Long[] spuIds=seckillSpuMapper.findAllSeckillSpuIds();
        // 布隆过滤器支持String[]数组类型的参数,将数据保存在Redis中
        // 所以我们要将Long[]转换为String[]
        String[] spuIdStrs=new String[spuIds.length];
        // 将spuIds数组中的元素转换为String类型赋值到spuIdStrs
        for(int i=0;i<spuIdStrs.length;i++){
            spuIdStrs[i]=spuIds[i]+"";
        }
        // 将赋值完毕的spuIdStrs保存到布隆过滤器中
        // 实际开发中应该查询两个批次,每个布隆过滤器保存对应批次的商品spuIds
        redisBloomUtils.bfmadd(bloomTodayKey,spuIdStrs);
        redisBloomUtils.bfmadd(bloomTomorrowKey,spuIdStrs);
        System.out.println("两个批次的布隆过滤器加载完成");

    }
}

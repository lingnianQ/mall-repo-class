package cn.tedu.mall.seckill.timer.job;

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
        // 首先查询下个批次的所有数据
        // 有需求要求查询两个批次或更多批次的数据
        // 因为可能允许更多批次的商品被用户提前浏览
        // 我们可以预热今天和明天的秒杀商品(我们的需求是一天一批)
        // 获取今天的key(正在进行秒杀的商品)
        // "spu:bloom:filter:2022-09-13"
        String bloomTodayKey= SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        // 获取明天的key
        String bloomTomorrowKey=
                SeckillCacheUtils.getBloomFilterKey(LocalDate.now().plusDays(1));
        // 实际开发中,可以按照实际去查询对应批次的所有秒杀商品
        // 学习过程中因为数据库数据量少,所以只查询同一批次即可
        // 根据时间,查询在这个时间正在进行秒杀的商品的所有id数据
        Long[] spuIds=seckillSpuMapper.findAllSeckillSpuIds();
        // 布隆过滤器支持保存的是字符串数组
        // 所以我们要将Long[]转换成String[]
        String[] spuIdsStr=new String[spuIds.length];
        // 遍历spuIds,将其中元素转换赋值到spuIdsStr
        for(int i=0;i<spuIds.length;i++){
            spuIdsStr[i]=spuIds[i]+"";
        }
        // 将赋值完毕的字符串数组数据保存到布隆过滤器中
        // 实际开发应该按照对应的批次保存,学习过程中就不分批次了
        redisBloomUtils.bfmadd(bloomTodayKey,spuIdsStr);
        redisBloomUtils.bfmadd(bloomTomorrowKey,spuIdsStr);
        System.out.println("两个批次的布隆过滤器加载完成");

    }
}

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

    // 装配操作布隆过滤器的对象
    @Autowired
    private RedisBloomUtils redisBloomUtils;
    // 查询出spuId的集合\数组,以保存到布隆过滤器中
    @Autowired
    private SeckillSpuMapper spuMapper;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        // 首先查询下个批次的所有数据
        // 有需求要求查询两个或更多批次的数据
        // 因为有些未开始秒杀的商品可以被用户提前浏览
        // 我们的设计就是提交加载两批商品(我们的商城设置为每一天一批)
        // 获取今天的日期当做布隆过滤器的key即可
        // spu:bloom:filter:2022-10-18
        String bloomTodayKey=
                SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        // 获取明天的key
        String bloomTomorrowKey=
                SeckillCacheUtils.getBloomFilterKey(LocalDate.now().plusDays(1));
        // 实际开发中,会按照今明两天的实际秒杀商品去查询spuId返回
        // 但是因为学习过程中,数据量较少,这里就只查询现有所有数据了
        // 最终目标是布隆过滤器中包含这两个批次的spuId,防止缓存穿透
        Long[] spuIds=spuMapper.findAllSeckillSpuIds();
        // 布隆过滤器只支持将字符串数组进行保存
        // 所以当前Long[]要转换为String[]
        String[] spuIdsStr=new String[spuIds.length];
        // 遍历spuIds数组,将元素转换为String类型并添加到spuIdsStr数组中
        for(int i=0;i<spuIds.length;i++){
            spuIdsStr[i]=spuIds[i]+"";
        }
        // 按照上面逻辑,应该获取两个String数组分别保存今天的和明天的spuId
        // 然后再往布隆过滤器中进行新增
        redisBloomUtils.bfmadd(bloomTodayKey,spuIdsStr);
        redisBloomUtils.bfmadd(bloomTomorrowKey,spuIdsStr);
        System.out.println("两个批次的布隆过滤器加载完毕!!!");
    }
}

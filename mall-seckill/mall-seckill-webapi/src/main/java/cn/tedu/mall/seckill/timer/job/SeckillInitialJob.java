package cn.tedu.mall.seckill.timer.job;

import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/14 12:25
 */
@Slf4j
public class SeckillInitialJob implements Job {

    /**
     * 查询sku信息的mapper
     */
    @Autowired
    private SeckillSkuMapper skuMapper;
    /**
     * 需要查询秒杀spu相关信息的mapper
     */
    @Autowired
    private SeckillSpuMapper spuMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * RedisTemplate对象在保存数据到Redis时,实际上会将数据序列化后保存
     * 这样做,对java对象或类似的数据在Redis中的读写效率较高,缺点是不能在Redis中修改这个数据
     * 我们现在要预热的是秒杀sku库存数,如果这个库存数也用RedisTemplate保存到Redis
     * 就容易在高并发情况下,由于线程安全问题导致"超卖"
     * 解决方法就是我们需要创建一个能够直接在Redis中修改数据的对象,来避免超卖的发生
     * SpringDataRedis提供了StringRedisTemplate类型,直接操作Redis中的字符串类型对象
     * 使用StringRedisTemplate向Redis保存数据,就没有序列化的过程,直接保存字符串值
     * Redis支持直接将数值格式的字符串直接进行修改,所以适合保存库存数
     * 这样就不需要java代码编写库存数的修改了,
     * 最后结合Redis操作数据的是单线程的特征,避免线程安全问题防止超卖
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * @param jobExecutionContext
     * @throws JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 当前方法是执行缓存预热工作的
        // 本方法运行的时机是秒杀开始前5分钟,所以要获取分钟后进行秒杀的所有商品
        LocalDateTime time = LocalDateTime.now().plusMinutes(5);
        // 查询这个时间所有进行秒杀的商品
        List<SeckillSpu> seckillSpusByTime = spuMapper.findSeckillSpusByTime(time);
        for (SeckillSpu seckillSpu : seckillSpusByTime) {
            // 我们的目标是缓存本批次所有商品的库存数
            // 那么就需要根据spuId查询到sku列表,sku对象中才有要执行秒杀的库存数
            List<SeckillSku> seckillSkus = skuMapper.findSeckillSkusBySpuId(seckillSpu.getSpuId());
            for (SeckillSku sku : seckillSkus) {
                log.info("开始将{}号sku商品的库存数预热到Redis",sku.getSkuId());
                // 要操作Redis,先确定保存值用的Key
                // SeckillCacheUtils.getStockKey()是获取库存字符串常量的方法
                // 参数会追加在常量最后
                // skuStockKey的实际值为:  mall:seckill:sku:stock:1
                String skuStockKey = SeckillCacheUtils.getStockKey(sku.getSkuId());
            }
        }
    }
}

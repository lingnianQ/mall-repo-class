package cn.tedu.mall.seckill.timer.job;

import cn.tedu.mall.common.config.PrefixConfiguration;
import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 当前方法是缓存预热操作
        // 它执行的时间是秒杀开始前5分钟,所以获取一个5分钟后的时间对象
        LocalDateTime time=LocalDateTime.now().plusMinutes(5);
        // 查询这个时间所有进行秒杀的商品
        List<SeckillSpu> seckillSpus=spuMapper.findSeckillSpusByTime(time);
        // 遍历当前批次所有秒杀的spu
        for(SeckillSpu spu: seckillSpus){
            // spu是商品的品类,必须确定规格也就是确定sku后,才能明确库存
            // 所以我们要根据spuId查询sku,然后将sku的库存数保存到Redis
            List<SeckillSku> seckillSkus=skuMapper.
                    findSeckillSkusBySpuId(spu.getSpuId());
            // 遍历获得了当前spu对应的sku列表,还要遍历sku列表才能保存库存数
            for(SeckillSku sku: seckillSkus){
                log.info("开始将{}号sku商品库存预热到Redis",sku.getSkuId());
                // 要在操作Redis之前,获得key常量
                // SeckillCacheUtils.getStockKey是获得事先准备好的字符串常量的方法
                // 它的实际的值为  mall:seckill:sku:stock:1
                String skuStockKey=SeckillCacheUtils.getStockKey(sku.getSkuId());
                // 检查Redis中是否已经包含了这个key
                if(redisTemplate.hasKey(skuStockKey)){
                    // 如果key已经存在,证明之前已经缓存过了,直接跳过
                    log.info("{}号sku商品已经缓存过了",sku.getSkuId());
                }else{
                    // 如果key不存在,就要将我们sku对象的库存数保存到Redis
                    // 使用stringRedisTemplate保存库存数到Redis
                    stringRedisTemplate.boundValueOps(skuStockKey)
                            .set(sku.getSeckillStock()+"",
                                   10*60*1000+ RandomUtils.nextInt(10000),
                                    TimeUnit.MILLISECONDS);
                    log.info("成功为{}号sku商品预热缓存",sku.getSkuId());
                }
            }

        }



    }
}

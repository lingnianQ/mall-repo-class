package cn.tedu.mall.seckill.timer.job;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    这样就不需要编写java代码判断了,再配合Redis天生单线程的特性,避免线程安全问题,防止超卖
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 当前方法是Quartz调度运行的,运行时是要预热的需求,所以秒杀还没有到时间
        // 我们设计的是提前5分钟预热
        // 所以我们要查询5分钟之后进行秒杀的商品信息
        LocalDateTime time = LocalDateTime.now().plusMinutes(5);
        // 查询这个时间所有的秒杀商品
        List<SeckillSpu> seckillSpus = spuMapper.findSeckillSpusByTime(time);
        // 遍历所有即将进行秒杀的商品,将它们的库存数保存到Redis
        for (SeckillSpu spu : seckillSpus) {
            // 当前spu是商品的品类,并没有库存数
            // 库存数保存在具体规格商品表sku中,所以要先根据spuId查询sku列表
            List<SeckillSku> seckillSkus = skuMapper.
                    findSeckillSkusBySpuId(spu.getSpuId());
            // 当前循环是变量spu的,查询到的sku列表需要再嵌套一层循环
            for (SeckillSku sku : seckillSkus) {
                log.info("开始将{}号sku商品的库存预热到Redis", sku.getSkuId());
                // 下面要确定当前sku的库存数的key
                // SeckillCacheUtils.getStockKey是能够获得事先准备好的库存key常量名称的方法
                // 所以skuStockKey可能是"mall:seckill:sku:stock:1"
                String skuStockKey=
                        SeckillCacheUtils.getStockKey(sku.getSkuId());
                // 检查Redis中是否已经包含了这个key
                if(redisTemplate.hasKey(skuStockKey)){
                    // 如果key已经存在,证明之前已经缓存过了,直接跳过即可
                    log.info("{}号sku商品已经缓存过了",sku.getSkuId());
                }else{
                    // 如果key不在Redis中,就要将sku的库存数保存到Redis
                    // 这里要将库存数的字符串格式保存,以便后续直接在Redis中减少库存的操作
                    // 设置过期时间,应该是秒杀活动时间,加5分钟,最好再加个随机数防雪崩
                    stringRedisTemplate.boundValueOps(skuStockKey)
                            .set(sku.getSeckillStock()+"",
                                    125*60*1000+ RandomUtils.nextInt(10000),
                                    TimeUnit.MILLISECONDS);
                    log.info("成功为{}号sku商品预热缓存",sku.getSkuId());
                }
            }
            // 上面sku库存数预热完成
            // 下面开始预热每个spu的随机码
            // 随机码的作用简单来说就是给访问spu设置一个随机的路径
            // 如果不知道这个随机的路径是无法访问我们spu信息的
            // 能够减少服务器的压力
            // 在缓存预热时我们的操作就是生成随机码并保存到Redis,以便在后续业务中获取
            // randCodeKey=mall:seckill:spu:url:rand:code:2
            String randCodeKey=SeckillCacheUtils.getRandCodeKey(spu.getSpuId());
            // 判断随机码是否已经生成
            if(redisTemplate.hasKey(randCodeKey)){
                // 如果已经有随机码的key存在,取出它,下面输出,方便测试时使用
                String code=redisTemplate.boundValueOps(randCodeKey).get()+"";
                log.info("{}号spu商品的随机码已经缓存:{}",spu.getSpuId(),code);
            }else{
                // 生成随机码100000~999999随机生成
                int randCode=RandomUtils.nextInt(900000)+100000;
                redisTemplate.boundValueOps(randCodeKey)
                        .set(randCode,125*60*1000+RandomUtils.nextInt(10000),
                                TimeUnit.MILLISECONDS);
                log.info("spuId为{}号的商品随机码为:{}",spu.getSpuId(),randCode);
            }
        }
    }
}

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
     * 秒杀预热
     *
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
                log.info("开始将{}号sku商品的库存数预热到Redis", sku.getSkuId());
                // 要操作Redis,先确定保存值用的Key
                // SeckillCacheUtils.getStockKey()是获取库存字符串常量的方法
                // 参数会追加在常量最后
                // skuStockKey的实际值为:  mall:seckill:sku:stock:1
                String skuStockKey = SeckillCacheUtils.getStockKey(sku.getSkuId());
                //检查redis是否存在已经包含这个key
                if (redisTemplate.hasKey(skuStockKey)) {
                    //如果key已经存在,证明之前已经缓存过了,直接跳过,
                    log.info("{}号sku商品已经缓存过了", sku.getSkuId());
                } else {
                    // 如果key不存在,就要将当前sku对象的库存数保存到Redis
                    // 使用StringRedisTemplate类型对象执行保存
                    stringRedisTemplate.boundValueOps(skuStockKey)
                            .set(sku.getSeckillStock().toString(),
                                    10 * 50 * 1000 + RandomUtils.nextInt(10000),
                                    TimeUnit.MILLISECONDS);
                    log.info("{}号sku商品库存数成功进入缓存!", sku.getSkuId());
                }
            }
            // 上面循环结束,完成了当前正在遍历的spu对应的所有sku库存数的缓存
            // 下面开始缓存预热spu对应的随机码
            // 随机码就是一个随机数,随机的范围可以自己定
            // 随机码会在用户提交订单时,进行验证,不提供正确随机码的用户不能生成订单
            // 我们下面要做的操作就是生成随机码并保存到Redis中
            // 确定key   "mall:seckill:spu:url:rand:code:2"
            String randCodeKey = SeckillCacheUtils.getRandCodeKey(seckillSpu.getSpuId());
            if (redisTemplate.hasKey(randCodeKey)) {
                // 如果已经存在这个key,不需要做任何其它操作
                // 但是为了方便今后的测试,我们将正确的随机码输出到控制台
                int randCode = (int) redisTemplate.boundValueOps(randCodeKey).get();
                log.info("{}号spu商品的随机码已经缓存过了,值为:{}",
                        seckillSpu.getSpuId(), randCode);
            } else {
                // 如果这个key没有保存过,就生成随机码保存到Redis
                // 生成随机码的范围自定,这里使用100000-999999
                int randCode = RandomUtils.nextInt(900000) + 100000;
                redisTemplate.boundValueOps(randCodeKey)
                        .set(randCode, 10 * 60 * 1000 + RandomUtils.nextInt(10000),
                                TimeUnit.MILLISECONDS);
                log.info("spuId为{}号的随机码生成成功!值为:{}",
                        seckillSpu.getSpuId(), randCode);
            }
        }
    }
}

package cn.tedu.mall.seckill.time.job;

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
    // 库存数是数字,Redis支持直接在Redis内部对数字格式的数据进行增减,无需java代码操作
    // 这样我们就要将库存数以字符串方式保存到Redis中,而不能使用二进制方式
    // StringRedisTemplate就是实现将数据以字符串的方法保存在redis中
    // 秒杀时,可以直接操作redis获得库存和减少库存的操作,避免线程安全问题,防止超卖
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 本方法是Quartz调度运行的,运行时秒杀还没有开始,要做的是将预热信息保存在Redis中
        // 我们设计的是秒杀开始5分钟前进行预热
        // 所以我们可以创建一个5分钟之后的时间,检查秒杀是否开始
        LocalDateTime time=LocalDateTime.now().plusMinutes(5);
        // 查询这个时间的所有秒杀商品
        List<SeckillSpu> seckillSpus=spuMapper.findSeckillSpusByTime(time);
        // 遍历所有正在秒杀的商品,将它们的库存缓存到Redis
        for(SeckillSpu spu : seckillSpus){
            // 当前spu是商品品类,需要缓存的库存数是sku中的信息
            // 所有要根据spuId查询秒杀sku列表
            List<SeckillSku> seckillSkus=skuMapper.findSeckillSkusBySpuId(spu.getSpuId());
            // 当前循环过程中再嵌套一个循环,将当前spu对相应的所有sku列表对象中的库存数保存在Redis
            for(SeckillSku sku : seckillSkus){
                log.info("开始将{}号商品的库存数预热到Redis",sku.getSkuId());
                // 获得当前skuId对应库存数的Redis的Key
                // SeckillCacheUtils.getStockKey能够获得项目为它设置好的常量值,参数会追加到最后
                // 最终的常量可能为
                // skuStockKey="mall:seckill:sku:stock:1"
                String skuStockKey= SeckillCacheUtils.getStockKey(sku.getSkuId());


            }
        }

    }
}

package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.pojo.product.vo.SkuStandardVO;
import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.vo.SeckillSkuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSkuService;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.service.ISeckillSkuService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillSkuServiceImpl implements ISeckillSkuService {
    @Autowired
    private SeckillSkuMapper skuMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    // Dubbo调用product模块查询sku普通信息
    @DubboReference
    private IForSeckillSkuService dubboSkuService;

    // 根据spuId查询sku列表
    @Override
    public List<SeckillSkuVO> listSeckillSkus(Long spuId) {
        //SeckillSkuVO作为返回值的泛型,也是既包含秒杀信息,又包含普通信息
        // 我们的查询也要分为两部分为他赋值
        List<SeckillSku> seckillSkus=skuMapper.findSeckillSkusBySpuId(spuId);
        // 声明最终返回值
        List<SeckillSkuVO> seckillSkuVOs=new ArrayList<>();
        for(SeckillSku sku : seckillSkus){
            // 声明一个SeckillSkuVO类型的空对象
            // 用于检查Redis中是否存在
            SeckillSkuVO seckillSkuVO=null;
            // 获得key并判断
            Long skuId=sku.getSkuId();
            // mall:seckill:sku:vo:1
            String seckillSkuVOKey= SeckillCacheUtils.getSeckillSkuVOKey(skuId);
            if(redisTemplate.hasKey(seckillSkuVOKey)){
                seckillSkuVO=(SeckillSkuVO) redisTemplate
                        .boundValueOps(seckillSkuVOKey).get();
            }else{
                // 当Redis中没有这个key时,连接数据库查询
                // 要查询两方面信息,秒杀sku信息和普通sku信息
                // dubbo调用根据skuId查询sku普通信息的方法
                SkuStandardVO skuStandardVO=dubboSkuService.getById(skuId);
                // 实例化seckillSkuVO对象,并赋值同名属性
                seckillSkuVO=new SeckillSkuVO();
                BeanUtils.copyProperties(skuStandardVO,seckillSkuVO);
                // 剩余的属性手动赋值
                seckillSkuVO.setStock(sku.getSeckillStock());
                seckillSkuVO.setSeckillPrice(sku.getSeckillPrice());
                seckillSkuVO.setSeckillLimit(sku.getSeckillLimit());
                // 保存到Redis中
                redisTemplate.boundValueOps(seckillSkuVOKey).set(
                        seckillSkuVO,120*60*1000+ RandomUtils.nextInt(10000),
                                    TimeUnit.MILLISECONDS);
            }
            // 将赋好值的seckillSkuVO对象添加到前面声明的集合中
            seckillSkuVOs.add(seckillSkuVO);
        }
        // 最后别忘了将添加好元素的集合返回
        return seckillSkuVOs;
    }
}

// {1,2,3,4,5,6,7,8,9} 数据库
// {1,2,3,4,5}  ->Redis
// {1,3,5,7,9}  ->Redis
// {2,4,6,8}    ->Redis





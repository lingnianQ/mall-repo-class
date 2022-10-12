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
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillSkuServiceImpl implements ISeckillSkuService {
    @Autowired
    private SeckillSkuMapper skuMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    // Dubbo调用product模块查询sku常规信息
    @DubboReference
    private IForSeckillSkuService dubboSkuService;

    // 根据spuId查询sku列表
    @Override
    public List<SeckillSkuVO> listSeckillSkus(Long spuId) {
        //SeckillSkuVO作为返回值集合的泛型,其中也是既包含秒杀sku信息,又包含常规sku信息
        // 我们最终也要将两个部分都赋值之后返回
        List<SeckillSkuVO> seckillSkuVOs=new ArrayList<>();
        // 根据spuId查询sku列表
        List<SeckillSku> seckillSkus=skuMapper.findSeckillSkusBySpuId(spuId);
        // 遍历所有sku
        for(SeckillSku sku : seckillSkus){
            // 获取当前sku的id以备后续使用
            Long skuId=sku.getSkuId();
            // 声明sku对象的Key
            // mall:seckill:sku:vo:1
            String seckillSkuVOKey= SeckillCacheUtils.getSeckillSkuVOKey(skuId);
            // 声明返回值类型对象,先赋null
            SeckillSkuVO seckillSkuVO=null;
            // 判断Redis中是否已经包含这个Key
            if(redisTemplate.hasKey(seckillSkuVOKey)){
                seckillSkuVO=(SeckillSkuVO) redisTemplate
                        .boundValueOps(seckillSkuVOKey).get();
            }else{
                // Redis没有这个sku就需要到数据库中去查询
                // 先查询常规信息
                SkuStandardVO skuStandardVO=dubboSkuService.getById(skuId);
                // 当前循环正在遍历的对象sku就是秒杀信息
                // 两个方面信息都有了,就实例化返回值seckillSkuVO赋值
                seckillSkuVO=new SeckillSkuVO();
                // 常规同名属性赋值
                BeanUtils.copyProperties(skuStandardVO,seckillSkuVO);
                // 秒杀信息手动赋值
                seckillSkuVO.setSeckillPrice(sku.getSeckillPrice());
                seckillSkuVO.setStock(sku.getSeckillStock());
                seckillSkuVO.setSeckillLimit(sku.getSeckillLimit());
                // 将sku保存到Redis中
                redisTemplate.boundValueOps(seckillSkuVOKey)
                        .set(seckillSkuVO,10*60*1000+ RandomUtils.nextInt(10000),
                                TimeUnit.MILLISECONDS);
            }
            // 将赋好值的seckillSkuVO对象添加到返回值集合中
            // seckillSkuVO可能是Redis直接获取的,也可能是数据库查出来的
            seckillSkuVOs.add(seckillSkuVO);
        }
        // 最后别忘了返回集合!!!
        return seckillSkuVOs;
    }
}

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
    // 操作Redis
    @Autowired
    private RedisTemplate redisTemplate;
    // Dubbo调用product模块查询sku的方法
    @DubboReference
    private IForSeckillSkuService dubboSkuService;

    // 根据SpuId查询秒杀Sku列表
    // sku中也是包含秒杀信息和一般信息
    @Override
    public List<SeckillSkuVO> listSeckillSkus(Long spuId) {
        // 根据spuId查询秒杀表中的sku列表
        List<SeckillSku> seckillSkus=skuMapper.findSeckillSkusBySpuId(spuId);
        // 声明泛型类型为当前业务方法指定的集合,以备作为返回值
        List<SeckillSkuVO> seckillSkuVOs=new ArrayList<>();
        // 遍历seckillSkus集合
        for(SeckillSku sku: seckillSkus){
            // 循环的目标是实例化SeckillSkuVO对象
            // 将一般信息和秒杀信息都存在其中后新增到seckillSkuVOs集合中
            SeckillSkuVO seckillSkuVO=null;
            // 获得skuId,拼接获得Redis的key
            Long skuId=sku.getSkuId();
            // mall:seckill:sku:vo:1
            String seckillSkuVOKey= SeckillCacheUtils.getSeckillSkuVOKey(skuId);
            if(redisTemplate.hasKey(seckillSkuVOKey)){
                seckillSkuVO=(SeckillSkuVO) redisTemplate
                                        .boundValueOps(seckillSkuVOKey).get();
            }else{
                // 当Redis没有保存当前key时,要连接数据库查询后,保存到Redis
                // 利用Dubbo调用product模块根据SkuId查询sku一般信息的方法
                SkuStandardVO skuStandardVO=dubboSkuService.getById(skuId);
                // 获得了sku的一般信息,将它赋值给SeckillSkuVO对象的同名属性
                seckillSkuVO=new SeckillSkuVO();
                BeanUtils.copyProperties(skuStandardVO,seckillSkuVO);
                // 下面将秒杀信息也手动赋值
                seckillSkuVO.setStock(sku.getSeckillStock());
                seckillSkuVO.setSeckillPrice(sku.getSeckillPrice());
                seckillSkuVO.setSeckillLimit(sku.getSeckillLimit());
                // seckillSkuVO赋值完毕保存到Redis
                redisTemplate.boundValueOps(seckillSkuVOKey).set(
                        seckillSkuVO,125*60*1000+ RandomUtils.nextInt(100*1000),
                                    TimeUnit.MILLISECONDS);
            }
            // 将赋好值的seckillSkuVO对象保存到集合中
            seckillSkuVOs.add(seckillSkuVO);
        }
        // 最后别忘了返回包含一般信息和秒杀信息的SkuVo的集合
        return seckillSkuVOs;
    }
}

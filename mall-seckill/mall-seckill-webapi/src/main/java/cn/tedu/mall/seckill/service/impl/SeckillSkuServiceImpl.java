package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.pojo.product.vo.SkuStandardVO;
import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.vo.SeckillSkuVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
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

/**
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/15 16:25
 */
@Slf4j
@Service
public class SeckillSkuServiceImpl implements ISeckillSkuService {

    @Autowired
    private SeckillSkuMapper skuMapper;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * dubbo调用获得sku详细信息
     */
    @DubboReference
    private IForSeckillSkuService dubboSkuService;

    @Override
    public List<SeckillSkuVO> listSeckillSkus(Long spuId) {
        //执行查询,根据spuId查询sku列表
        List<SeckillSku> seckillSkus = skuMapper.findSeckillSkusBySpuId(spuId);
        //实例化集合
        List<SeckillSkuVO> seckillSkuVOList = new ArrayList<>();
        for (SeckillSku sku : seckillSkus) {
            Long skuId = sku.getSkuId();
            String seckillSkuVOKey = SeckillCacheUtils.getSeckillSkuVOKey(skuId);
            SeckillSkuVO seckillSkuVO = null;
            if (redisTemplate.hasKey(seckillSkuVOKey)) {
                seckillSkuVO = (SeckillSkuVO) redisTemplate
                        .boundValueOps(seckillSkuVOKey)
                        .get();
            } else {
                //redis中不存在这个sku,查询数据库
                SkuStandardVO skuStandardVO = dubboSkuService.getById(skuId);
                seckillSkuVO = new SeckillSkuVO();
                BeanUtils.copyProperties(skuStandardVO, seckillSkuVO);
                seckillSkuVO.setSeckillPrice(sku.getSeckillPrice());
                seckillSkuVO.setStock(sku.getSeckillStock());
                seckillSkuVO.setSeckillLimit(sku.getSeckillLimit());
                //赋值完,存储到redis
                redisTemplate.boundValueOps(seckillSkuVOKey)
                        .set(seckillSkuVO, 10 * 60 * 1000 + RandomUtils.nextInt(10000),
                                TimeUnit.MILLISECONDS);
            }
            seckillSkuVOList.add(seckillSkuVO);
        }
        return seckillSkuVOList;
    }
}

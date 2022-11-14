package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/14 11:22
 */
@Repository
public interface SeckillSkuMapper {
    /**
     * 根据spuId查询sku列表
     *
     * @param spuId
     * @return
     */
    List<SeckillSku> findSeckillSkusBySpuId(Long spuId);
}

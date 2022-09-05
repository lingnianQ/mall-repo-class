package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeckillSpuMapper {

    // 查询秒杀商品列表
    List<SeckillSpu> findSeckillSpus();

    // 根据指定时间,查询正在进行秒杀的商品信息
    List<SeckillSpu> findSeckillSpusByTime(LocalDateTime time);

    // 根据spuId查询spu商品信息
    SeckillSpu findSeckillSpuById(Long spuId);

    // 布隆过滤器用,查询所有商品的spuId
    Long[] findAllSeckillSpuIds();


}

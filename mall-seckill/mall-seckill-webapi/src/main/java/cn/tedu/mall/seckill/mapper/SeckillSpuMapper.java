package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/13 10:18
 */
@Repository
public interface SeckillSpuMapper {

    /**
     * 查询秒杀商品列表的方法
     *
     * @return
     */
    List<SeckillSpu> findSeckillSpus();

    /**
     * 根据指定时间,查询正在进行秒杀的商品列表
     *
     * @param time
     * @return
     */
    List<SeckillSpu> findSeckillSpusByTime(LocalDateTime time);

    /**
     * 根据SpuId查询spu秒杀信息
     *
     * @param spuId
     * @return
     */
    SeckillSpu findSeckillSpuById(Long spuId);

    /**
     * 布隆过滤器用:查询获得所有秒杀商品的SpuId数组
     *
     * @return array
     */
    Long[] findAllSeckillSpuIds();
}

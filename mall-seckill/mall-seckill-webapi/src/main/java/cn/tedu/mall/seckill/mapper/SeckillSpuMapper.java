package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import org.springframework.stereotype.Repository;

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
}

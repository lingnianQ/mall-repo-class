package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeckillSpuMapper {
    // 查询秒杀商品列表的方法
    List<SeckillSpu> findSeckillSpus();
}




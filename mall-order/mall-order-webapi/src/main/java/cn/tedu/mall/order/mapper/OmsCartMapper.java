package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsCart;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OmsCartMapper {

    // 判断当前用户的购物车中是否已经包含指定sku商品
    OmsCart selectExistsCart(@Param("userId") Long userId,
                             @Param("skuId") Long skuId);
    // 新增sku信息到购物车
    int saveCart(OmsCart omsCart);

    // 修改购物车中sku的数量
    int updateQuantityById(OmsCart omsCart);




}

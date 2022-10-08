package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsCartMapper {

    // 判断当前用户的购物车中是否已经包含指定sku商品
    OmsCart selectExistsCart(@Param("userId") Long userId,
                             @Param("skuId") Long skuId);
    // 新增sku信息到购物车
    int saveCart(OmsCart omsCart);

    // 修改购物车中sku的数量
    int updateQuantityById(OmsCart omsCart);

    // 根据用户id查询购物车中sku信息
    List<CartStandardVO> selectCartsByUserId(Long userId);

    // 根据用户选中的id,删除购物车中的商品(支持一次删除多个商品)
    int deleteCartsByIds(Long[] ids);

    // 清空指定用户购物车中所有sku商品
    int deleteCartsByUserId(Long userId);

    // 根据用户id和skuId删除商品
    int deleteCartByUserIdAndSkuId(OmsCart omsCart);

}

package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsCartMapper {

    // 判断当前用户的购物车中是否已经包含指定sku的商品
    OmsCart selectExistsCart(@Param("userId") Long userId,
                             @Param("skuId") Long skuId);

    // 新增sku信息到购物车表
    void saveCart(OmsCart omsCart);
    // 修改购物车中指定sku的数量
    void updateQuantityById(OmsCart omsCart);

    // 根据用户id查询购物车中sku信息
    List<CartStandardVO> selectCartsByUserId(Long userId);



}

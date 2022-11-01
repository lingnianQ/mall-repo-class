package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsCart;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * @author sytsn
 */
@Repository
public interface OmsCartMapper {
    /**
     * 判断当前用户购物车中是否已经包含指定的商品
     *
     * @param userId 1
     * @param skuId 1
     * @return OmsCart
     */
    OmsCart selectExistsCart(@Param("userId") Long userId,
                             @Param("skuId") Long skuId);

    /**
     * 新增sku信息到购物车
     *
     * @param omsCart 1
     * @return rows
     */
    int saveCart(OmsCart omsCart);

    /**
     * 修改购物车中sku商品的数量
     *
     * @param omsCart 1
     * @return rows
     */
    int updateQuantityById(OmsCart omsCart);

}

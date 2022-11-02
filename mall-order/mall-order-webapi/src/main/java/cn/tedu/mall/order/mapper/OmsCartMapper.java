package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author sytsn
 */
@Repository
public interface OmsCartMapper {
    /**
     * 判断当前用户购物车中是否已经包含指定的商品
     *
     * @param userId 1
     * @param skuId  1
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

    /**
     * 根据用户id查询购物车中sku信息
     *
     * @param userId
     * @return
     */
    List<CartStandardVO> selectCartsByUserId(Long userId);

    /**
     * 据用户选中的一个或多个id,删除购物车中的商品(批量删除操作)
     *
     * @param ids array
     * @return rows
     */
    int deleteCartsByIds(Long[] ids);

    /**
     * 清空指定用户购物车中所有sku商品
     *
     * @param userId user_id
     * @return rows
     */
    int deleteCartsByUserId(Long userId);

    /**
     * 根据用户id和SkuId删除商品
     *
     * @param omsCart omsCart
     * @return rows
     */
    int deleteCartsByUserIdAndSkuId(OmsCart omsCart);


}

package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.*;
import cn.tedu.mall.product.service.front.IForFrontAttributeService;
import cn.tedu.mall.product.service.front.IForFrontSkuService;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/1 22:57
 */
@Slf4j
@Service
public class FrontProductServiceImpl implements IFrontProductService {

    @DubboReference
    private IForFrontSpuService dubboSpuService;
    /**
     * 根据spuId查询sku信息用的对象
     */
    @DubboReference
    private IForFrontSkuService dubboSkuService;

    /**
     * 根据spuId查询查询当前商品的所有属性/规格用的对象
     */
    @DubboReference
    private IForFrontAttributeService dubboAttributeService;

    /**
     * 根据分类id查询spu列表
     *
     * @param categoryId
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public JsonPage<SpuListItemVO> listSpuByCategoryId(Long categoryId, Integer page, Integer pageSize) {
        JsonPage<SpuListItemVO> jsonPage =
                dubboSpuService.listSpuByCategoryId(categoryId, page, pageSize);
        return jsonPage;
    }

    /**
     * 根据id查询spuvo对象
     * 根据spuId查询spu信息
     *
     * @param id
     */
    @Override
    public SpuStandardVO getFrontSpuById(Long id) {
        SpuStandardVO spuStandardVO = dubboSpuService.getSpuById(id);
        return spuStandardVO;
    }

    /**
     * 根据spuId查询对应的sku列表
     *
     * @param spuId
     * @return
     */
    @Override
    public List<SkuStandardVO> getFrontSkusBySpuId(Long spuId) {
        List<SkuStandardVO> list =
                dubboSkuService.getSkusBySpuId(spuId);
        return list;
    }

    /**
     * 利用spuId查询spu详情
     * 根据spuId查询spuDetail信息
     *
     * @param spuId
     * @return
     */
    @Override
    public SpuDetailStandardVO getSpuDetail(Long spuId) {
        SpuDetailStandardVO spuDetailStandardVO =
                dubboSpuService.getSpuDetailById(spuId);
        return spuDetailStandardVO;
    }

    /**
     * 微服务调用pms查询一个spu绑定的所有属性和值
     * 根据spuId查询对应的所有属性\规格列表
     *
     * @param spuId
     * @return
     */
    @Override
    public List<AttributeStandardVO> getSpuAttributesBySpuId(Long spuId) {
        List<AttributeStandardVO> list = dubboAttributeService
                .getSpuAttributesBySpuId(spuId);
        return list;
    }
}

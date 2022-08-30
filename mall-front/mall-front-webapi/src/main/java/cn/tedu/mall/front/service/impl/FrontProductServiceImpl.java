package cn.tedu.mall.front.service.impl;


import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.*;
import cn.tedu.mall.product.service.front.IForFrontAttributeService;
import cn.tedu.mall.product.service.front.IForFrontSkuService;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FrontProductServiceImpl implements IFrontProductService {

    @DubboReference
    private IForFrontSpuService dubboSpuService;
    // 消费skuService的相关服务:根据spuId查询sku列表
    @DubboReference
    private IForFrontSkuService dubboSkuService;
    // 消费指定商品查询所有参数选项的相关服务:根据spuId查询参数列表
    @DubboReference
    private IForFrontAttributeService dubboAttributeService;


    @Override
    public JsonPage<SpuListItemVO> listSpuByCategoryId(Long categoryId, Integer page, Integer pageSize) {
        // IForFrontSpuService实现类中已经完成了分页查询的细节,我们直接调用即可
        JsonPage<SpuListItemVO> list=
                dubboSpuService.listSpuByCategoryId(categoryId,page,pageSize);
        // 返回 list!!!
        return list;
    }

    // 根据spuId查询Spu对象信息
    @Override
    public SpuStandardVO getFrontSpuById(Long id) {
        //SpuStandardVO是标准的查询spu的返回值
        SpuStandardVO spuStandardVO=dubboSpuService.getSpuById(id);
        return spuStandardVO;
    }
    // 根据SpuId查询sku列表
    @Override
    public List<SkuStandardVO> getFrontSkusBySpuId(Long spuId) {
        // SkuStandardVO是标准的查询sku的返回值
        List<SkuStandardVO> list=dubboSkuService.getSkusBySpuId(spuId);
        return list;
    }

    // 根据spuId查询spuDetail对象
    @Override
    public SpuDetailStandardVO getSpuDetail(Long spuId) {
        SpuDetailStandardVO spuDetailStandardVO=
                            dubboSpuService.getSpuDetailById(spuId);
        return spuDetailStandardVO;
    }

    // 根据spuId查询当前商品所有参数列表
    @Override
    public List<AttributeStandardVO> getSpuAttributesBySpuId(Long spuId) {
        List<AttributeStandardVO> list=dubboAttributeService
                            .getSpuAttributesBySpuId(spuId);
        return list;
    }
}

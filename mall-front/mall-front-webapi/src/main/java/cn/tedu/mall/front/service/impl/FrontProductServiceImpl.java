package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.*;
import cn.tedu.mall.product.service.front.IForFrontAttributeService;
import cn.tedu.mall.product.service.front.IForFrontSkuService;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FrontProductServiceImpl implements IFrontProductService {

    @DubboReference
    private IForFrontSpuService dubboSpuService;
    // 声明消费sku相关的业务逻辑
    @DubboReference
    private IForFrontSkuService dubboSkuService;
    // 声明消费商品参数选项(attribute)的业务逻辑
    @DubboReference
    private IForFrontAttributeService dubboAttributeService;


    // 根据分类id分页查询spu列表
    @Override
    public JsonPage<SpuListItemVO> listSpuByCategoryId(Long categoryId,
                                                       Integer page, Integer pageSize) {
        // IForFrontSpuService实现类中完成的就是分页查询,所以我们直接调用即可
        JsonPage<SpuListItemVO> list=
            dubboSpuService.listSpuByCategoryId(categoryId,page,pageSize);
        // 千万别忘了返回list
        return list;
    }

    // 根据spuId查询Spu信息
    @Override
    public SpuStandardVO getFrontSpuById(Long id) {
        // dubbo调用spuService提供的方法即可
        SpuStandardVO spuStandardVO=dubboSpuService.getSpuById(id);
        // 返回查询到的对象spuStandardVO
        return spuStandardVO;
    }
    // 根据spuId查询sku列表
    @Override
    public List<SkuStandardVO> getFrontSkusBySpuId(Long spuId) {
        // dubbo 调用 skuService的方法实现功能
        List<SkuStandardVO> list=dubboSkuService.getSkusBySpuId(spuId);
        return list;
    }
    // 根据spuId查询spuDetail(Detail:详情)
    @Override
    public SpuDetailStandardVO getSpuDetail(Long spuId) {
        SpuDetailStandardVO spuDetailStandardVO=
                dubboSpuService.getSpuDetailById(spuId);
        return spuDetailStandardVO;
    }
    // 根据spuId查询当前商品所有的参数选项
    @Override
    public List<AttributeStandardVO> getSpuAttributesBySpuId(Long spuId) {
        return null;
    }
}

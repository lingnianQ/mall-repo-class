package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import cn.tedu.mall.pojo.product.vo.CategoryStandardVO;
import cn.tedu.mall.product.service.front.IForFrontCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 三级分类列表
 *
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/1 19:39
 */
@Slf4j
@Service
@DubboService
public class FrontCategoryServiceImpl implements IFrontCategoryService {

    @DubboReference
    private IForFrontCategoryService dubboCategoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    public static final String CATEGORY_TREE_KEY = "category_tree";


    @Override
    public FrontCategoryTreeVO categoryTree() {

        //1.先尝试从redis中获取数据
        if (redisTemplate.hasKey(CATEGORY_TREE_KEY)) {
            FrontCategoryTreeVO<FrontCategoryEntity> treeVO =
                    (FrontCategoryTreeVO<FrontCategoryEntity>) redisTemplate.boundValueOps(CATEGORY_TREE_KEY).get();
            return treeVO;
        }

        //2.若redis没有数据,则表示此次为首次访问数据库
        List<CategoryStandardVO> categoryStandardVOS = dubboCategoryService.getCategoryList();

        //3.CategoryStandardVO 转换为 带有children属性的 FrontCategoryEntity
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO = initTree(categoryStandardVOS);

        //4.保存到redis
        redisTemplate.boundValueOps(CATEGORY_TREE_KEY).set(treeVO, 1, TimeUnit.MINUTES);

        //5.最后返回treeVO
        return treeVO;
    }

    private FrontCategoryTreeVO<FrontCategoryEntity> initTree(List<CategoryStandardVO> categoryStandardVOS) {

        //一. 确定所有分类的父分类id
        Map<Long, List<FrontCategoryEntity>> map = new HashMap<>();
        log.info("当前分类对象总数量:{}",categoryStandardVOS.size());

        return null;
    }
}














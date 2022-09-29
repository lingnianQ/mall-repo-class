package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import cn.tedu.mall.product.service.front.IForFrontCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@DubboService
@Service
@Slf4j
public class FrontCategoryServiceImpl implements IFrontCategoryService {

    // 装配操作Redis的对象
    @Autowired
    private RedisTemplate redisTemplate;
    // 当前front模块没有连接数据库的操作,所有数据均来自于Dubbo调用product模块
    // 这里是消费product模块查询所有分类数据的功能
    @DubboReference
    private IForFrontCategoryService dubboCategoryService;

    // 开发过程中使用Redis的规范:为了降低Redis使用Key拼写错误的情况,我们会定义常量
    public static final String CATEGORY_TREE_KEY="category_tree";

    @Override
    public FrontCategoryTreeVO categoryTree() {
        // 我们先检查Redis中是否已经保存了包含所有分类的三级分类树对象
        if(redisTemplate.hasKey(CATEGORY_TREE_KEY)){
            // redis中已经包含了三级分类树对象,获取后直接返回即可
            FrontCategoryTreeVO<FrontCategoryEntity> treeVO=
                    (FrontCategoryTreeVO<FrontCategoryEntity>)
                    redisTemplate.boundValueOps(CATEGORY_TREE_KEY).get();
            // 将从redis中获得的treeVO返回
            return treeVO;
        }


        return null;
    }
}

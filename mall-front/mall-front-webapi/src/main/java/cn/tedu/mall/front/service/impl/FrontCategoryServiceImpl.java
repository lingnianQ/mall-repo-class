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

import java.util.List;

@DubboService
@Service
@Slf4j
public class FrontCategoryServiceImpl implements IFrontCategoryService {

    // 开过程中使用Redis的规范:为了降低Redis的Key拼写错误的风险,我们会定义常量使用
    public static final String CATEGORY_TREE_KEY="category_tree";

    // 当前front模块没有连接数据库的操作,所有数据均来源于Dubbo调用product模块
    // 所有要消费product模块具备对应功能的接口
    @DubboReference
    private IForFrontCategoryService dubboCategoryService;
    // 操作Redis的对象
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public FrontCategoryTreeVO categoryTree() {
        // 我们先检查Redis中是否包含已经查询出的三级分类树
        if(redisTemplate.hasKey(CATEGORY_TREE_KEY)){
            // redis中已经包含了三级分类树,直接获取后返回即可
            FrontCategoryTreeVO<FrontCategoryEntity> treeVO=
                    (FrontCategoryTreeVO<FrontCategoryEntity>)
                        redisTemplate.boundValueOps(CATEGORY_TREE_KEY).get();
            // 千万别忘了返回!
            return treeVO;
        }
        // Redis中没有三级分类树信息,表示当前请求可能是第一次请求
        // dubbo调用查询pms数据库所有分类对象的方法
        List<CategoryStandardVO> categoryStandardVOs=
                            dubboCategoryService.getCategoryList();
        // 需要将没有关联关系的分类列表CategoryStandardVO类型
        // 转换为具备关联关系的分类树列表FrontCategoryEntity类型



        return null;
    }
}

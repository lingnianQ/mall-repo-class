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

    // 开发规范标准:为了降低Redis的Key拼写错误的风险,我们都会定义常量
    public static final String CATEGORY_TREE_KEY="category_tree";
    // 当前模块查询所有分类信息对象要依靠product模块,所以需要dubbo调用product模块的查询数据表中所有分类的方法
    @DubboReference
    private IForFrontCategoryService dubboCategoryService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public FrontCategoryTreeVO categoryTree() {
        // 我们会将查询到的三级分类树保存在Redis中,所有先检查Redis中是否包含上面定义的key
        if(redisTemplate.hasKey(CATEGORY_TREE_KEY)){
            // 如果判断Redis中已经包含分类树信息,直接从Redis中获得返回即可
            FrontCategoryTreeVO<FrontCategoryEntity> treeVO=
                    (FrontCategoryTreeVO<FrontCategoryEntity>)redisTemplate
                            .boundValueOps(CATEGORY_TREE_KEY).get();
            // 别忘了返回!
            return treeVO;
        }
        // Redis中没有三级分类树信息,当前请求是第一个运行该方法的请求
        // dubbo调用查询数据库中所有分类信息对象
        List<CategoryStandardVO> categoryStandardVOs=dubboCategoryService.getCategoryList();

        return null;
    }
}

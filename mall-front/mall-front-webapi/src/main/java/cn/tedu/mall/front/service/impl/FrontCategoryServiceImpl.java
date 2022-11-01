package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import cn.tedu.mall.pojo.product.vo.CategoryStandardVO;
import cn.tedu.mall.product.service.front.IForFrontCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        log.info("当前分类对象总数量:{}", categoryStandardVOS.size());
        //1. 遍历数据库查询出来的所有分类对象集合
        for (CategoryStandardVO categoryStandardVO : categoryStandardVOS) {
            FrontCategoryEntity frontCategoryEntity = new FrontCategoryEntity();
            BeanUtils.copyProperties(categoryStandardVO, frontCategoryEntity);
            //获取父分类id---此时 FrontCategoryEntity 的children为null
            Long parentId = frontCategoryEntity.getParentId();
            //判断父分类id是否存在于map
            if (map.containsKey(parentId)) {
                // 如果当前map已经存在这个key,直接将当前分类对象添加到value的集合中即可
                map.get(parentId).add(frontCategoryEntity);
            } else {
                //map不存在父分类id,将分类集合添加到value(list),--,parent_id=0
                List<FrontCategoryEntity> value = new ArrayList<>();
                value.add(frontCategoryEntity);
                map.put(parentId, value);
            }
        }

        //二. 将子分类对象关联到对应的父分类的children属性中
        List<FrontCategoryEntity> firstLevels = map.get(0L);
        if (firstLevels == null || firstLevels.isEmpty()) {
            throw new CoolSharkServiceException(ResponseCode.INTERNAL_SERVER_ERROR, "缺失一级分类(parent_id=0,第一级)对象!");
        }
        //遍历一级分类集合
        for (FrontCategoryEntity oneLevel : firstLevels) {
            //获取一级分类对象的id
            Long secondLevelParentId = oneLevel.getId();
            //根据上面一级分类的id,获得对应的二级分类集合
            List<FrontCategoryEntity> secondLevels = map.get(secondLevelParentId);
            if (secondLevels == null || secondLevels.isEmpty()) {
                log.warn("当前分类没有二级分类内容:{}", secondLevelParentId);
                // 跳过本次循环,继续下次循环
                continue;
            }
            //确定二级分类对象后,遍历二级分类对象集合
            for (FrontCategoryEntity twoLevel : secondLevels) {
                // 获取当前二级分类的id(三级分类的父id)
                Long thirdLevelParentId = twoLevel.getId();
                // 根据二级分类的id获取对应的三级分类对象集合
                List<FrontCategoryEntity> thirdLevels = map.get(thirdLevelParentId);
                // 判断三级分类对象集合是否为null
                if (thirdLevels == null || thirdLevels.isEmpty()) {
                    log.warn("当前二级分类对象没有三级分类内容:{}", thirdLevelParentId);
                    continue;
                }
                // 将三级分类对象集合,添加到当前二级分类对象的children属性中
                twoLevel.setChildrens(thirdLevels);
            }
            // 将二级分类对象集合(已经赋好值的对象集合),添加到一级分类对象的children属性中
            oneLevel.setChildrens(secondLevels);
        }

        // 到此为止,所有的分类对象,都应该正确保存到了自己对应的父分类对象的children属性中
        // 但是最后要将一级分类的集合firstLevels,赋值给FrontCategoryTreeVO<FrontCategoryEntity>
        // 所以要先实例化它,再给它赋值,返回
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO = new FrontCategoryTreeVO<>();
        treeVO.setCategories(firstLevels);
        //返回treeVO
        return treeVO;
    }
}














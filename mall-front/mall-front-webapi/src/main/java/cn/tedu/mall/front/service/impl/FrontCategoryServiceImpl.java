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
        // Redis中没有三级分类树信息,表示本次请求可能是首次访问
        // dubbo调用查询所有分类对象的方法
        List<CategoryStandardVO> categoryStandardVOs=
                        dubboCategoryService.getCategoryList();
        // 我们需要将没有关联子分类能力的CategoryStandardVO类型
        // 转换为具备关联子分类能力的FrontCategoryEntity类型
        // 并将正确的父子分类关系保存构建起来,最好编写一个单独的方法
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO=
                            initTree(categoryStandardVOs);
        // 上面已经完成了三级分类树的构建,下面要将返回值treeVO
        // 保存在redis中,方便后面请求的访问
        redisTemplate.boundValueOps(CATEGORY_TREE_KEY)
                .set(treeVO,1, TimeUnit.MINUTES);
        // 上面时间是针对学习测试而定的,实际开发中会设置比较长的时间,比如24小时
        // 最后别忘了返回!!!!
        return treeVO;
    }

    // 将从数据库中查询到的分类对象转换为三级分类树的方法
    private FrontCategoryTreeVO<FrontCategoryEntity> initTree(
                        List<CategoryStandardVO> categoryStandardVOs) {
        // 第一步:
        // 确实所有分类对象的父分类id
        // 以父分类id为key,将相同父分类的子分类对象保存到同一个Map的key中
        // 一个父分类可能包含多个子分类,所有这个Map的value是一个List
        Map<Long,List<FrontCategoryEntity>> map=new HashMap<>();
        log.info("当前分类对象总数:{}",categoryStandardVOs.size());
        // 遍历所有分类对象的集合
        for(CategoryStandardVO categoryStandardVO: categoryStandardVOs){
            // CategoryStandardVO是没有children熟悉的不能保存分类父子关系
            // 所有我们要先将其中的对象转换为能够保存父子分类关系的FrontCategoryEntity
            FrontCategoryEntity frontCategoryEntity=new FrontCategoryEntity();
            // 将同名属性赋值到frontCategoryEntity对象中
            BeanUtils.copyProperties(categoryStandardVO,frontCategoryEntity);
            // 获取当前元素对应的父分类id(父分类id为0表示当前分类对象为一级分类)
            // 后面反复使用这个父分类id,所以最好取出
            Long parentId=frontCategoryEntity.getParentId();
            // 这个父分类id要作为key保存到map中,所有要先判断map中是否已经包含这个key
            if(map.containsKey(parentId)){
                // 运行到这表示当前map已经包含了这个父分类的key
                // 将当前分类追加到map元素的value中
                map.get(parentId).add(frontCategoryEntity);
            }else {
                // 运行到这表示当前map还没有这个父分类id的key对应的元素
                // 先实例化一个list对象当做map的value,并添加当前分类对象
                List<FrontCategoryEntity> value=new ArrayList<>();
                value.add(frontCategoryEntity);
                // 在map中创建一个以父分类id为key的元素(使用put方法)
                map.put(parentId,value);
            }
        }
        // 第二步:
        // 将子分类对象关联到父分类对象的children属性中
        // 下面操作应该从一级分类开始,我们先通过0作为父分类id获得所有一级分类
        List<FrontCategoryEntity> firstLevels=map.get(0L);
        // 判断一级分类集合是否为空,防止后续出现空指针
        if(firstLevels==null || firstLevels.isEmpty()){
            throw new CoolSharkServiceException(
                    ResponseCode.INTERNAL_SERVER_ERROR,"没有一级分类,运行结束");
        }
        // 遍历所有一级分类对象
        for(FrontCategoryEntity oneLevel : firstLevels){
            // 获得当前一级分类的id
            Long secondLevelParentId=oneLevel.getId();
            // 获取当前一级分类对象包含的二级分类集合
            List<FrontCategoryEntity> secondLevels=map.get(secondLevelParentId);
            if(secondLevels==null || secondLevels.isEmpty()){
                log.warn("当前分类没有二级分类内容:{}",secondLevelParentId);
                // 如果当前一级分类没有二级分类,跳过本次循环,继续下次循环
                continue;
            }
            // 遍历二级分类对象集合
            for(FrontCategoryEntity twoLevel : secondLevels){
                // 获得二级分类的id(三级分类的父id)
                //                               ↓↓↓↓↓↓
                Long thirdLevelParentId=twoLevel.getId();
                // 根据二级分类id获得对应当前二级分类对象的所有三级分类元素集合
                List<FrontCategoryEntity> thirdLevels=map.get(thirdLevelParentId);
                // 判断当前三级分类是否为空
                if(thirdLevels==null || thirdLevels.isEmpty()){
                    log.warn("当前二级分类对象没有三级分类内容:{}",thirdLevelParentId);
                    continue;
                }
                // 将三级分类对象集合,保存到二级分类对象的children属性中
                twoLevel.setChildrens(thirdLevels);
            }
            // 将二级分类对象集合,保存到一级分类对象的children属性中
            oneLevel.setChildrens(secondLevels);
        }
        // 到此为止,所有的分类对象都保存到了正确的父分类对象的children属性中
        // 最终包含它们的是firstLevels集合,但是我们的返回值是FrontCategoryTreeVO<FrontCategoryEntity>
        // 所以我们要实例化这个类型对象,并将firstLevels赋值到其中,然后返回
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO=
                new FrontCategoryTreeVO<>();
        treeVO.setCategories(firstLevels);
        // 千万记得返回treeVO对象!!!!
        return treeVO;
    }
}

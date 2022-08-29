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
        // 自定义一个转换三级分类树的方法,减少当前业务代码的冗余度
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO=
                initTree(categoryStandardVOs);



        return null;
    }

    private FrontCategoryTreeVO<FrontCategoryEntity> initTree(List<CategoryStandardVO> categoryStandardVOs) {
        // 第一部分,确定所有分类对象的父分类
        // 声明一个Map,这个Map的key是分类对象的父Id,value是这个父分类Id下的所有子分类对象
        // 一个父分类Id可以包含多个子分类对象,所以value是个list
        Map<Long,List<FrontCategoryEntity>> map=new HashMap<>();
        log.info("当前分类对象总数为:{}",categoryStandardVOs.size());
        // 遍历categoryStandardVOs
        for(CategoryStandardVO categoryStandardVO: categoryStandardVOs){
            // categoryStandardVO是没有children属性的,不能保存分类关系
            // 所以我们先要把它转换为能够保存父子关系的FrontCategoryEntity
            FrontCategoryEntity frontCategoryEntity=new FrontCategoryEntity();
            // 将同名属性赋值到frontCategoryEntity对象中
            BeanUtils.copyProperties(categoryStandardVO,frontCategoryEntity);
            // 因为后面会频繁使用父分类id,所以现在取出
            Long parentId=frontCategoryEntity.getParentId();
            // 向map中新增当前分类对象到对应的map的key中
            // 要先判断这个key是否已经存在
            if(map.containsKey(parentId)){
                // 如果已经存在这个key,就只需要将新对象保存在这个key对应的value的list中
                map.get(parentId).add(frontCategoryEntity);
            }else{
                // 如果当前map没有这个key
                // 我们要创建一个List对象,保存当前分类对象后,最后当做map的value
                List<FrontCategoryEntity> value=new ArrayList<>();
                value.add(frontCategoryEntity);
                // map中使用put方法新增元素,parentId为key,list为值
                map.put(parentId,value);
            }
        }
        // 第二部分,将子分类对象关联到父分类对象的children属性中
        // 第一部分中,我们确定了每个父分类下的所有子分类
        // 下面我们从一级分类来开始,将所有对应的子分类赋值到当前父分类的children属性中
        // 我们项目设计数据库中父分类id为0,是一级分类,所以我们先获得所有一级分类对象
        List<FrontCategoryEntity> firstLevels=map.get(0L);
        // 判断当前一级分类是否为null
        if (firstLevels==null || firstLevels.isEmpty()){
            throw new CoolSharkServiceException(
                    ResponseCode.INTERNAL_SERVER_ERROR,"当前数据没有根分类");
        }
        //遍历所有一级分类对象
        for(FrontCategoryEntity oneLevel:firstLevels){
            // 确定了一级分类对象,可以根据当前一级分类的id,获得它包含的二级分类
            Long secondLevelParentId=oneLevel.getId();
        }
        return null;
    }
}

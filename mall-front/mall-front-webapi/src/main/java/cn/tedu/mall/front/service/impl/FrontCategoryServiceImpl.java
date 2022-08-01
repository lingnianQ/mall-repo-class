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
        // 调用将数据库中查询出的所有分类信息转换成三级分类树的方法
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO= initTree(categoryStandardVOs);
        // 将转换完成的treeVO保存到Redis中,以备后续请求直接从Redis中获取
        redisTemplate.boundValueOps(CATEGORY_TREE_KEY).set(treeVO,24, TimeUnit.HOURS);
        // 千万别忘了返回
        return treeVO;
    }

    private FrontCategoryTreeVO<FrontCategoryEntity> initTree(List<CategoryStandardVO> categoryStandardVOs) {
        // 第一部分,确定所有分类对象的父分类
        // 声明一个Map,使用父分类id做这个map的key,使用当前分类对象集合对这个map的value
        // 将所有相同父分类的对象添加到正确的集合中
        Map<Long,List<FrontCategoryEntity>> map=new HashMap<>();
        log.info("当前分类对象总数为:{}",categoryStandardVOs.size());
        // 遍历categoryStandardVOs,进行下一步操作
        for(CategoryStandardVO categoryStandardVO:categoryStandardVOs){
            // CategoryStandardVO没有children属性,不能保存子分类
            // 所以我们要先将它转换为能保存子分类的FrontCategoryEntity
            FrontCategoryEntity frontCategoryEntity=new FrontCategoryEntity();
            // 利用赋值工具类BeanUtils将同名属性赋值到frontCategoryEntity
            BeanUtils.copyProperties(categoryStandardVO,frontCategoryEntity);
            // 因为后面要反复使用当前分类对象的父分类id所以最好给它取出来
            Long parentId=frontCategoryEntity.getParentId();
            // 根据当前分类对象的父分类id向Map添加元素,但是要先判断是否已经存在这个Key
            if(map.containsKey(parentId)){
                //如果有这个key,我们直接将当前分类对象添加到map的value的List中
                map.get(parentId).add(frontCategoryEntity);
            }else{
                // 如果当前map没有这个key
                // 我们要创建List对象,将分类对象保存在这个List中
                List<FrontCategoryEntity> value=new ArrayList<>();
                value.add(frontCategoryEntity);
                // 使用当前parentId做key,上面实例化的list做value保存到map中
                map.put(parentId,value);
            }
        }
        // 第二部分,将子分类对象关联到父分类对象的children属性中
        // 第一部分中获得Map已经包含了所有父分类包含的子分类对象
        // 下面我们就可以从根分类开始,通过循环遍历每个级别的分类对象,添加到对应级别的children属性中
        // 一级分类的父id在数据库中设计为0,所以我们代码直接写死获得父分类id为0的所有子分类
        List<FrontCategoryEntity> firstLevels=map.get(0L);
        // 判断firstLevels是否为空
        if(firstLevels==null){
            throw new CoolSharkServiceException(
                    ResponseCode.INTERNAL_SERVER_ERROR,"当前项目没有根分类");
        }
        //遍历所有一级分类对象
        for(FrontCategoryEntity oneLevel : firstLevels){
            // 获得当前一级分类的id
            //          检查这里是不是getId     ↓↓↓↓↓↓
            Long secondLevelParentId=oneLevel.getId();
            // 获得当前分类对象的所有子分类(获得二级分类集合)
            List<FrontCategoryEntity> secondLevels=map.get(secondLevelParentId);
            // 判断是否包含二级分类对象
            if(secondLevels==null){
                log.warn("当前分类缺少二级分类内容:{}",secondLevelParentId);
                // 终止(跳过)本次循环,继续下次循环
                continue;
            }
            // 遍历二级分类集合
            for(FrontCategoryEntity twoLevel : secondLevels){
                // 获得当前二级分类对象的id,既三级分类对象的父id
                //          检查这里是不是getId    ↓↓↓↓↓↓
                Long thirdLevelParentId=twoLevel.getId();
                List<FrontCategoryEntity> thirdLevels=map.get(thirdLevelParentId);
                if(thirdLevels==null){
                    log.warn("当前分类缺少三级分类内容:{}",thirdLevelParentId);
                    continue;
                }
                // 将三级分类集合添加到二级分类对象的children属性中
                twoLevel.setChildrens(thirdLevels);
            }
            // 将二级分类对象集合添加到一级分类对象的children属性中
            oneLevel.setChildrens(secondLevels);
        }
        // 到此为止,所有分类对象的父子关系都已经构建完成
        // 但是本方法的返回值FrontCategoryTreeVO<FrontCategoryEntity>对象还没有创建
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO=new FrontCategoryTreeVO<>();
        // 将一级分类的集合赋值给该对象的属性
        treeVO.setCategories(firstLevels);
        // 千万别忘了返回!!!!
        return treeVO;
    }
}

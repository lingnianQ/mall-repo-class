package cn.tedu.mall.search.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.product.model.Spu;
import cn.tedu.mall.pojo.search.entity.SpuEntity;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import cn.tedu.mall.search.repository.SpuForElasticRepository;
import cn.tedu.mall.search.service.ISearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SearchServiceImpl implements ISearchService {

    // dubbo调用product模块查询所有spu
    @DubboReference
    private IForFrontSpuService dubboSpuService;
    // 将查询出的spu对象新增到ES中
    @Autowired
    private SpuForElasticRepository spuRepository;
    // 先循环调用dubbo分页查询数据库
    // 将分页查询出的spu对象新增到ES

    @Override
    public void loadSpuByPage() {
        // 我们并不知道本次分页从查询的总页数
        // 所以是典型的先执行后判断,推荐使用do-while
        int i=1;      // 循环次数,从1开始,同时代表页码
        int pages=0;  // 总页数,在第一次运行之后才能知道具体值,默认值赋0即可(不赋值也可以)
        do{
            // dubbo 分页查询页码指定的数据
            JsonPage<Spu> spus=dubboSpuService.getSpuByPage(i,2);
            // 需要将JsonPage类型中的数据转换为List<SpuForElastic>,以便新增到ES
            List<SpuForElastic> esSpus=new ArrayList<>();
            // 遍历spus,进行转换并新增到esSpus
            for(Spu spu : spus.getList()){
                SpuForElastic esSpu=new SpuForElastic();
                BeanUtils.copyProperties(spu,esSpu);
                // 将赋值好的esSpu添加到esSpus中
                esSpus.add(esSpu);
            }
            // esSpus集合中已经添加了当前页数据,执行新增到ES
            spuRepository.saveAll(esSpus);
            log.info("成功加载第{}页数据",i);
            // 为下次循环准备
            i++;
            pages=spus.getTotalPage();
        }while(i<=pages);
    }


    @Override
    public JsonPage<SpuForElastic> search(String keyword, Integer page, Integer pageSize) {
        return null;
    }
}

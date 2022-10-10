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
    // 我们创建的可以批量新增到ES的持久层对象
    @Autowired
    private SpuForElasticRepository spuRepository;

    @Override
    public void loadSpuByPage() {
        // 循环完成分页查询所有数据
        // 在循环的过程中,查询一页信息就新增到ES中,直到最后一页
        int i=1;        // 循环次数变量i 从1开始 同时代表页码
        int pages=0;    // 总页数,在第一次循环运行后才能知道具体值,默认赋值0(或者只声明不赋值)即可
        do{
            // dubbo调用查询当前页的spu数据
            JsonPage<Spu> spus=dubboSpuService.getSpuByPage(i,2);
            // 需要将JsonPage类型中的数转换为List<SpuForElastic>
            List<SpuForElastic> esSpus=new ArrayList<>();
            // 遍历spus集合 进行转换,并新增到esSpus
            for(Spu spu : spus.getList()){

            }
        }while(i<=pages);


    }


    @Override
    public JsonPage<SpuEntity> search(String keyword, Integer page, Integer pageSize) {
        return null;
    }


}

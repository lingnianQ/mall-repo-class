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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

//@Service
@Slf4j
@Deprecated
public class SearchServiceImpl //implements ISearchService
                    {
    /*
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
                SpuForElastic esSpu=new SpuForElastic();
                BeanUtils.copyProperties(spu,esSpu);
                // 将转换完成的对象添加到esSpus集合中
                esSpus.add(esSpu);
            }
            // esSpus集合中已经包含了本页的数据,可以执行批量新增到ES
            spuRepository.saveAll(esSpus);
            log.info("成功加载了第{}页数据",i);
            // 为下次循环做准备
            i++;
            // 确定总页数
            pages=spus.getTotalPage();
        }while(i<=pages);


    }


    @Override
    public JsonPage<SpuForElastic> search(String keyword, Integer page, Integer pageSize) {

        // 根据参数中的分页数据,执行分页查询,注意SpringData框架分页页码从0开始
        Page<SpuForElastic> spus=spuRepository.querySearch(keyword,
                                                PageRequest.of(page-1,pageSize));
        // 当前业务逻辑层方法要求返回JsonPage类型,但是我们查询结果是Page类型,需要转换
        // 转换的实现可以通过在JsonPage类中编写方法实现,也可以在当前位置手动转换实现
        JsonPage<SpuForElastic> jsonPage=new JsonPage<>();
        // 赋值分页信息
        jsonPage.setPage(page);
        jsonPage.setPageSize(pageSize);
        // 赋值总页数和总条数
        jsonPage.setTotal(spus.getTotalElements());
        jsonPage.setTotalPage(spus.getTotalPages());
        // 赋值分页数据
        jsonPage.setList(spus.getContent());
        // 最后别忘了返回!!!!
        return jsonPage;
    }
*/

}

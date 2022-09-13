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
//@Slf4j
@Deprecated
public class SearchServiceImpl  {
//
//    // dubbo调用product模块查询所有spu
//    @DubboReference
//    private IForFrontSpuService dubboSpuService;
//    // 将查询出的spu对象新增到ES中
//    @Autowired
//    private SpuForElasticRepository spuRepository;
//    // 先循环调用dubbo分页查询数据库
//    // 将分页查询出的spu对象新增到ES
//
//    @Override
//    public void loadSpuByPage() {
//        // 我们并不知道本次分页从查询的总页数
//        // 所以是典型的先执行后判断,推荐使用do-while
//        int i=1;      // 循环次数,从1开始,同时代表页码
//        int pages=0;  // 总页数,在第一次运行之后才能知道具体值,默认值赋0即可(不赋值也可以)
//        do{
//            // dubbo 分页查询页码指定的数据
//            JsonPage<Spu> spus=dubboSpuService.getSpuByPage(i,2);
//            // 需要将JsonPage类型中的数据转换为List<SpuForElastic>,以便新增到ES
//            List<SpuForElastic> esSpus=new ArrayList<>();
//            // 遍历spus,进行转换并新增到esSpus
//            for(Spu spu : spus.getList()){
//                SpuForElastic esSpu=new SpuForElastic();
//                BeanUtils.copyProperties(spu,esSpu);
//                // 将赋值好的esSpu添加到esSpus中
//                esSpus.add(esSpu);
//            }
//            // esSpus集合中已经添加了当前页数据,执行新增到ES
//            spuRepository.saveAll(esSpus);
//            log.info("成功加载第{}页数据",i);
//            // 为下次循环准备
//            i++;
//            pages=spus.getTotalPage();
//        }while(i<=pages);
//    }
//
//
//    @Override
//    public JsonPage<SpuForElastic> search(
//            String keyword, Integer page, Integer pageSize) {
//        // 根据参数中分页的数据,执行分页,注意SpringData分页参数设置0是第一页,所以要减1
//        Page<SpuForElastic> spus=spuRepository.querySearch(keyword,
//                                        PageRequest.of(page-1,pageSize));
//        // 当前方法需要返回JsonPage类型,我们需要将spus对象进行转换
//        // 我们使用就地转换的方式,也可以在JsonPage类中添加转换方法来调用
//        JsonPage<SpuForElastic> jsonPage=new JsonPage<>();
//        // 赋值相关分页信息
//        jsonPage.setPage(page);
//        jsonPage.setPageSize(pageSize);
//        //总条数和总页数
//        jsonPage.setTotal(spus.getTotalElements());
//        jsonPage.setTotalPage(spus.getTotalPages());
//        // 将查询到的数据赋值到jsonPage
//        jsonPage.setList(spus.getContent());
//        // 最后别忘了返回
//        return jsonPage;
//    }
}

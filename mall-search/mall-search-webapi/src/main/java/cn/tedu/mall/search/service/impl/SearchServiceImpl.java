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
import springfox.documentation.spring.web.json.Json;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SearchServiceImpl implements ISearchService {

    // dubbo调用product模块
    @DubboReference
    private IForFrontSpuService dubboSpuService;
    @Autowired
    private SpuForElasticRepository spuRepository;

    // 先利用dubbo从数据库中查Spu,再将查出的spu新增到ES中
    // 而且整体是循环执行
    @Override
    public void loadSpuByPage() {
        // 先查询一次,这样就可以根据查询出的分页信息知道总页数了
        // 典型的先运行,后判断,推荐使用do-while
        int i=1;     // 循环次数,0也是页码
        int pages=0; // 总页数,初始赋值为0(不赋值也可以)
        do{
            // Dubbo调用分页查询,查询当前第i页的数据
            JsonPage<Spu> spus=dubboSpuService.getSpuByPage(i,2);
            // 需要将上面查询到的Spu类型实体类转换成SpuForElastic,才能新增到ES
            List<SpuForElastic> esSpus=new ArrayList<>();
            // 遍历spus集合,将其中对象转换为SpuForElastic类型,并添加到esSpus集合中
            for(Spu spu: spus.getList()){
                SpuForElastic esSpu=new SpuForElastic();
                BeanUtils.copyProperties(spu,esSpu);
                // 将被赋好值的esSpu对象添加到esSpus集合中
                esSpus.add(esSpu);
            }
            // 执行SpringDataElasticsearch提供的批量新增的方法,执行新增到ES的操作
            spuRepository.saveAll(esSpus);
            log.info("成功加载第{}页数据",i);
            // 为下次循环准备
            pages=spus.getTotalPage();
            i++;
        }while(i<=pages); // 循环条件限制循环次数

    }

    @Override
    public JsonPage<SpuForElastic> search(String keyword, Integer page, Integer pageSize) {
        // SpringData分页参数page如果是0表示第一页,所以参数赋值时要减1
        Page<SpuForElastic> spus=spuRepository.querySearch(keyword,
                                                 PageRequest.of(page-1,pageSize));
        // 业务逻辑层返回JsonPage类型,需要我们将Page转换为JsonPage
        // 最简单的办法就是就地实例化其对象,并为它赋值
        JsonPage<SpuForElastic> jsonPage=new JsonPage<>();
        // 当前页码
        jsonPage.setPage(page+1);
        // 每页条数
        jsonPage.setPageSize(pageSize);
        // 总条数和总页数
        jsonPage.setTotal(spus.getTotalElements());
        jsonPage.setTotalPage(spus.getTotalPages());
        // 将查询到的数据赋值为jsonPage保存
        jsonPage.setList(spus.getContent());
        // 别忘了返回jsonPage对象
        return jsonPage;
    }
}

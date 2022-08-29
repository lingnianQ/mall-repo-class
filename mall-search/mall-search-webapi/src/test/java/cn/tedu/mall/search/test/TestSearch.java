package cn.tedu.mall.search.test;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.search.repository.SpuForElasticRepository;
import cn.tedu.mall.search.service.ISearchService;
import org.elasticsearch.search.aggregations.metrics.InternalHDRPercentiles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestSearch {

    @Autowired
    private ISearchService searchService;

    @Autowired
    private SpuForElasticRepository elasticRepository;

    @Test
    void showAll(){
        Iterable<SpuForElastic> es=elasticRepository.findAll();
        es.forEach(e-> System.out.println(e));
    }

    @Test
    void loadData(){
        searchService.loadSpuByPage();
        System.out.println("ok");
    }

    @Test
    void getSpuByTitle(){
        Iterable<SpuForElastic> it=elasticRepository.querySpuForElasticsByTitleMatches("手机");
        it.forEach(e -> System.out.println(e));
    }

    /*@Test
    void getSpuByKeyword(){
        Iterable<SpuForElastic> it=elasticRepository.querySearch("手机");
        it.forEach(e-> System.out.println(e));
    }*/





}

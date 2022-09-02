package cn.tedu.mall.search.test;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.search.repository.SpuForElasticRepository;
import cn.tedu.mall.search.service.ISearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LoadTest {

    @Autowired
    private ISearchService searchService;

    @Test
    void loadData(){
        searchService.loadSpuByPage();
        System.out.println("ok");
    }


    @Autowired
    private SpuForElasticRepository repository;
    @Test
    void showData(){
        Iterable<SpuForElastic> spus=repository.findAll();
        spus.forEach(spu -> System.out.println(spu));
    }

    // 根据title查询数据
    @Test
    void getSpuByTitle(){
        Iterable<SpuForElastic> it=
                repository.querySpuForElasticsByTitleMatches("手机华为小米");
        it.forEach(e -> System.out.println(e));
    }




}

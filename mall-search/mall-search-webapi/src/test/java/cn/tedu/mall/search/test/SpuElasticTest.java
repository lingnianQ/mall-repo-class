package cn.tedu.mall.search.test;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.search.repository.SpuForElasticRepository;
import cn.tedu.mall.search.service.ISearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 下面注解必须加!!!!!!
@SpringBootTest
public class SpuElasticTest {

    @Autowired
    private ISearchService searchService;

    @Autowired
    private SpuForElasticRepository spuForElasticRepository;

    @Test
    void loadData() {
        searchService.loadSpuByPage();
        System.out.println("ESspu加载完成");

    }

    @Test
    void showData() {
        Iterable<SpuForElastic> spus = spuForElasticRepository.findAll();
        spus.forEach(System.out::println);
    }

    @Test
    void getSpuByTitle() {
        Iterable<SpuForElastic> spus = spuForElasticRepository.querySpuForElasticByTitleMatches("华为");
        spus.forEach(System.out::println);
    }

    @Test
    void getSpuByQuery() {
        // 调用查询四个字段包含指定关键字数据的方法
        Iterable<SpuForElastic> spus =
                spuForElasticRepository.querySearch("华为手机");
        spus.forEach(System.out::println);
    }

}

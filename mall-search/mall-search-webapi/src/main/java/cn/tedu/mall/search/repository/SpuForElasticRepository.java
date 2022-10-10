package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

// SpuForElastic实体类操作ES的持久层接口
// 继承了父接口之后,就具备了对ES进行基本增删改查的方法(我们使用的是批量增)
@Repository
public interface SpuForElasticRepository extends
            ElasticsearchRepository<SpuForElastic,Long> {

    // 查询title字段中包含指定关键字的spu数据
    Iterable<SpuForElastic> querySpuForElasticsByTitleMatches(String title);

    @Query("{\n" +
            "    \"bool\": {\n" +
            "      \"should\": [\n" +
            "        { \"match\": { \"name\": \"?0\"}},\n" +
            "        { \"match\": { \"title\": \"?0\"}},\n" +
            "        { \"match\": { \"description\": \"?0\"}},\n" +
            "        { \"match\": { \"category_name\": \"?0\"}}\n" +
            "        ]\n" +
            "     }\n" +
            "}")
    // 上面指定了查询语句,这个方法的方法名就随意定义了
    Page<SpuForElastic> querySearch(String keyword, Pageable pageable);



}

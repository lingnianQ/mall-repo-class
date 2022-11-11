package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * SpuForElastic实体类操作ES的持久层接口
 * 需要继承SpringData给定的父接口,继承之后可以直接使用提供的基本增删改查方法
 *
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/11 3:39
 */
@Repository
public interface SpuForElasticRepository extends ElasticsearchRepository<SpuForElastic, Long> {
    /**
     * 根据标题(title)查找spu数据
     *
     * @param title
     * @return
     */
    Iterable<SpuForElastic> querySpuForElasticByTitleMatches(String title);

    /**
     * 下面指定查询语句的情况下,方法的方法名就可以随意起名了,参数对应查询语句中的"?0"
     *
     * @param keyword
     * @param pageable
     * @return
     */
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
    Page<SpuForElastic> querySearch(String keyword, Pageable pageable);

}

package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.product.model.Spu;
import cn.tedu.mall.pojo.search.entity.SpuEntity;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpuEntityRepository extends
                            ElasticsearchRepository<SpuEntity,Long> {
    // 根据用户输入的关键字,查询ES中匹配的数据
    // Logstash将所有参与查询的字段拼接成了一个字段search_text,SpuEntity中并不存在
    // 所以我们只能编写查询语句,从ES中搜索search_text匹配的数据
    @Query("{\"match\":{\"search_text\":{\"query\":\"?0\"}}}")
    Page<SpuEntity> querySearchByText(String keyword, Pageable pageable);

}

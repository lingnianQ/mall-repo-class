package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.search.entity.SpuEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpuEntityRepository extends
                                ElasticsearchRepository<SpuEntity,Long> {
    // 根据用户输入的关键字查询ES中匹配的数据
    // Logstash将所有查询字段拼接成了search_text字段,SpuEntity并不存在
    // 所以我们的查询直接搜索search_text,而且不能利用SpringData给的定义方法名查询的功能
    @Query("{\"match\":{\"search_text\":{\"query\":\"?0\"}}}")
    Page<SpuEntity> querySearchByText(String keyword, Pageable pageable);


}

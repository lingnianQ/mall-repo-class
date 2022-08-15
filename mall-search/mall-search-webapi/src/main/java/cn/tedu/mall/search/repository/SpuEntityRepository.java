package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.search.entity.SpuEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;

public interface SpuEntityRepository {

    // 根据用户输入的关键字查询Es中匹配的数据
    // 因为我们要查询的search_text字段,并没有在SpuEntity中声明
    // 所以这里要对search_text字段进行查询的话,只能使用查询语句
    @Query("{\"match\":{\"search_text\":{\"query\":\"?0\"}}}")
    Page<SpuEntity> querySearchByText(String keyword, Pageable pageable);
}

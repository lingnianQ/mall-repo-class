package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

// SpuForElastic实体类操作Es的持久层方法
// 继承父接口,当前接口实现类具备基本增删改查功能
public interface SpuForElasticRepository extends
                                ElasticsearchRepository<SpuForElastic,Long> {

}

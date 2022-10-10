package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

// SpuForElastic实体类操作ES的持久层接口
// 继承了父接口之后,就具备了对ES进行基本增删改查的方法(我们使用的是批量增)
@Repository
public interface SpuForElasticRepository extends
            ElasticsearchRepository<SpuForElastic,Long> {

    // 查询title字段中包含指定关键字的spu数据
    Iterable<SpuForElastic> querySpuForElasticsByTitleMatches(String title);


}

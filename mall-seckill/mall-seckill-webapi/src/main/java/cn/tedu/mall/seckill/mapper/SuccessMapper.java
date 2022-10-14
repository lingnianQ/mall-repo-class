package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.Success;
import org.springframework.stereotype.Repository;

@Repository
public interface SuccessMapper {

    // 新增Success对象的数据库方法
    int saveSuccess(Success success);

}

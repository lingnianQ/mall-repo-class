package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.Success;
import org.springframework.stereotype.Repository;

/**
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/17 14:17
 */
@Repository
public interface SuccessMapper {
    // 新增Success对象到数据库的方法
    int saveSuccess(Success success);
}

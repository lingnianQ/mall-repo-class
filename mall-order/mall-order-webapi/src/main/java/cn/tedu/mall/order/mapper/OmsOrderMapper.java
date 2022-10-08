package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsOrder;
import org.springframework.stereotype.Repository;

@Repository
public interface OmsOrderMapper {

    // 新增订单的方法
    int insertOrder(OmsOrder order);

}

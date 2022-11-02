package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsOrder;
import org.springframework.stereotype.Repository;

/**
 * @author sytsn
 */
@Repository
public interface OmsOrderMapper {

    /**
     * 新增订单的方法
     *
     * @param omsOrder
     * @return
     */
    int insertOrder(OmsOrder omsOrder);

}

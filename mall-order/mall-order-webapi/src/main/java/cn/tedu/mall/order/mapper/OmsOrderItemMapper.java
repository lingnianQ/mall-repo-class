package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsOrderItemMapper {

    // 新增订单项(order_item)的方法
    // 一个订单可以包含多个订单项,为了减少连接数据库新增订单项的次数
    // 我们新增订单项方法的参数可以是List
    int insertOrderItems(List<OmsOrderItem> omsOrderItems);

}

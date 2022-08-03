package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsOrderItemMapper {

    // 新增订单项的方法
    // 一个订单可以包含多个订单项,所以我们在设计新增时,将这个方法的参数设计为List
    // 参数是List需要在xmlsql语句中,使用foreach遍历
    void insertOrderItems(List<OmsOrderItem> omsOrderItems);

}

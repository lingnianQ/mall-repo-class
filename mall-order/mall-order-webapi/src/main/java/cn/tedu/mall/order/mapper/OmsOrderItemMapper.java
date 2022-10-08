package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsOrderItemMapper {

    // 新增订单项(order_item)的方法
    // 一个订单可能包含多个订单项,如果循环遍历新增每一个订单项,连库次数多,效率降低
    // 我们采用一次连库新增多条订单项的方法,完成这个业务,来提高数据库操作效率
    // 也就是进行批量新增,这个方法的参数就是一个List集合了
    int insertOrderItemList(List<OmsOrderItem> omsOrderItems);


}

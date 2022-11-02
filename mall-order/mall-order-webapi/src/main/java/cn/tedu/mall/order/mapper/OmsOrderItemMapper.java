package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author sytsn
 */
@Repository
public interface OmsOrderItemMapper {


    /**
     * 新增订单项(order_item)的方法
     * 一个订单可能包含多件商品,如果每件商品都单独新增到数据库,会造成连库次数多,效率低
     * 我们采用一次连库增加多条订单项的方式,提升连接\操作数据库的效率
     * 所以参数就是一个List<OmsOrderItem>类型了
     *
     * @param omsOrderItems order_item
     * @return rows
     */
    int insertOrderItemList(List<OmsOrderItem> omsOrderItems);
}

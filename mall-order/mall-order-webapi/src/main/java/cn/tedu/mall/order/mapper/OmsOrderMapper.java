package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    /**
     * 查询当前用户指定时间范围内的所有订单信息(包含订单项)
     *
     * @param orderListTimeDTO
     * @return
     */
    List<OrderListVO> selectOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO);

    /**
     * 利用动态sql,实现对订单内容的修改
     * 参数是OmsOrder类型,且必须包含id值,id不可修改,其他属性如果不为空,就修改为当前值
     *
     * @param order
     * @return
     */
    int updateOrderById(OmsOrder order);

}

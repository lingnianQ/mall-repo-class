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

}

package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsOrderMapper {

    // 新增订单的mapper方法
    int insertOrder(OmsOrder order);

    // 查询当前用户指定时间范围的所有订单
    List<OrderListVO> selectOrdersBetweenTimes(
                            OrderListTimeDTO orderListTimeDTO);


}

package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsOrderMapper {

    // 新增订单的方法
    int insertOrder(OmsOrder order);

    // 查询当前用户指定时间范围内的所有订单信息(包含订单项)
    List<OrderListVO> selectOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO);

    // 动态修改订单,参数是omsOrder对象
    // 参数对象中必须有id,id不可修改,其他属性不为空就修改为当前属性值
    int updateOrderById(OmsOrder order);


}

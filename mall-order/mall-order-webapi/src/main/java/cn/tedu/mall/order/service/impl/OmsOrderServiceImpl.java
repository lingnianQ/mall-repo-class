package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.order.mapper.OmsOrderItemMapper;
import cn.tedu.mall.order.mapper.OmsOrderMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.order.utils.IdGeneratorUtils;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.dto.OrderStateUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.order.vo.OrderDetailVO;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import cn.tedu.mall.product.service.order.IForOrderSkuService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 后期秒杀功能会调用这个生成订单的方法
@DubboService
@Service
@Slf4j
public class OmsOrderServiceImpl implements IOmsOrderService {

    @Autowired
    private OmsOrderMapper omsOrderMapper;
    @Autowired
    private OmsOrderItemMapper omsOrderItemMapper;
    @DubboReference
    private IForOrderSkuService dubboSkuService;
    @Autowired
    private IOmsCartService omsCartService;

    // 当前方法调用了product模块的数据库操作
    // 当有运行异常时,所有数据库操作都需要回滚,保证事务的原子性
    // 所以要激活分布式事务Seata的功能
    @GlobalTransactional
    @Override
    public OrderAddVO addOrder(OrderAddDTO orderAddDTO) {
        // 第一部分:收集信息,准备数据
        // OrderAddDTO对象中包含订单和订单项的集合
        // 我们要先获得其中订单的信息,做订单的新增
        OmsOrder order=new OmsOrder();
        // 同名属性赋值过去
        BeanUtils.copyProperties(orderAddDTO,order);
        // 可以对比OrderAddDTO和OmsOrder类的属性,发现若干属性需要我们来手动赋值
        // 以为赋值属性较多,建议单独编写一个方法处理
        loadOrder(order);

        // 第二部分:执行数据库操作
        // 第三部分:收集返回值信息最终返回
        return null;
    }

    private void loadOrder(OmsOrder order) {
        // 针对order对象为空的属性,来赋值,保证新增成功
        // 给id赋值,我们使用Leaf分布式方式来赋值
        Long id= IdGeneratorUtils.getDistributeId("order");
        order.setId(id);

    }

    @Override
    public void updateOrderState(OrderStateUpdateDTO orderStateUpdateDTO) {

    }

    @Override
    public JsonPage<OrderListVO> listOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO) {
        return null;
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        return null;
    }
}

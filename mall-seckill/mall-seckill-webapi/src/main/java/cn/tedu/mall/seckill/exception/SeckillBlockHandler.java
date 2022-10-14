package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;

// 秒杀业务的限流处理类
@Slf4j
public class SeckillBlockHandler {

    // 声明限流的方法,返回值必须和限流的控制器方法一致
    // 参数是包含所有控制器的方法,除此之外还要声明一个BlockException异常类参数
    // 如果这个方法是实例方法,那么调用的话就要实例化对象后才能调用
    // 设置为静态方法的话,可以直接通过类名调用
    public static JsonResult seckillBlock(String randCode,
                                   SeckillOrderAddDTO seckillOrderAddDTO,
                                   BlockException e){
        log.error("一个请求被限流了!");
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,
                "服务器忙,请稍后再试");
    }
}

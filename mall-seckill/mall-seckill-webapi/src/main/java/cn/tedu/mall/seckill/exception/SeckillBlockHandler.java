package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;

//  秒杀业务的限流处理类
@Slf4j
public class SeckillBlockHandler {
    // 声明限流的方法,返回值必须和控制器一致
    // 参数要保护控制器方法的参数,还要添加一个BlockException的异常类型参数
    // 这个方法定义成静态的,可以直接被使用类名调用到
    public static JsonResult seckillBlock(String randCode,
                                          SeckillOrderAddDTO seckillOrderAddDTO,
                                          BlockException e){
        log.error("一个请求被限流了");
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,
                "对不起,服务器忙,请稍候再试");
    }



}

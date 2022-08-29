package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;

// 秒杀业务限流异常处理类
@Slf4j
public class SeckillBlockHandler {
    // 声明限流的方法,返回值必须和控制器一致
    // 参数要包含控制器的参数,最后再添加一个BlockException异常类型参数
    // 这个方法我们定义为静态方法,方法调用者不需要实例化对象,就能直接使用
    public static JsonResult seckillBlock(String randCode,
                                          SeckillOrderAddDTO seckillOrderAddDTO,
                                          BlockException e){
        log.error("一个请被限流了!");
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,"服务器忙!");
    }

}








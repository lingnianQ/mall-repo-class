package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import lombok.extern.slf4j.Slf4j;

// 秒杀业务的降级处理类
@Slf4j
public class SeckillFallback {

    // 降级方法参数可以和控制器完全一致,也可以添加一个Throwable类型的参数
    // 如果想知道是什么异常导致了服务降级,最后声明一下这个参数,输出它的信息
    public static JsonResult seckillFallback(String randCode,
                                             SeckillOrderAddDTO seckillOrderAddDTO,
                                             Throwable throwable){
        log.error("一个请求被降级了");
        throwable.printStackTrace();
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,
                "发生异常!异常信息为:"+throwable.getMessage());

    }
}

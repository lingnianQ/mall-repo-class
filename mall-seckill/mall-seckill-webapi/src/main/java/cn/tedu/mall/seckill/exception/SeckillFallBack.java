package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import lombok.extern.slf4j.Slf4j;
import springfox.documentation.spring.web.json.Json;

// Sentinel秒杀降级方法
@Slf4j
public class SeckillFallBack {

    // 返回值必须和控制器方法一致
    // 方法参数也是包含控制层方法参数,可以不写其它参数,也可以添加Throwable类型的参数
    // Throwable类型的参数就是触发这次降级的原因
    public static JsonResult seckillFall(String randCode,
                                         SeckillOrderAddDTO seckillOrderAddDTO,
                                         Throwable throwable){
        log.error("一个请求降级了");
        throwable.printStackTrace();
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,throwable.getMessage());
    }

}

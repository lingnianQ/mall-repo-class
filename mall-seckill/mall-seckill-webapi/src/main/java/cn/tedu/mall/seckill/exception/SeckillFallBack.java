package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import com.baomidou.mybatisplus.extension.api.R;
import lombok.extern.slf4j.Slf4j;

// 秒杀业务的降级处理类
@Slf4j
public class SeckillFallBack {

    // 返回值和控制器方法一致
    // 参数也是包含控制器的所有方法,可以不写其它参数,也可以添加一个Throwable类型的参数
    // Throwable类型的参数就是触发降级的异常对象
    public static JsonResult seckillFallback(String randCode,
                                             SeckillOrderAddDTO seckillOrderAddDTO,
                                             Throwable throwable){
        log.error("一个请求被降级了");
        throwable.printStackTrace();
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,
                throwable.getMessage());
    }

}

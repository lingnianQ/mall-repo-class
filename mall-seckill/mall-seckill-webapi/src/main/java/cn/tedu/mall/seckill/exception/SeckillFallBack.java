package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * 秒杀降级处理器
 *
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/16 15:51
 */
@Slf4j
public class SeckillFallBack {

    /**
     * 降级方法参数和返回值与限流方法基本一致,只是异常类型是Throwable类型
     *
     * @param randCode
     * @param seckillOrderAddDTO
     * @param e
     * @return
     */
    public static JsonResult seckillFallback(String randCode,
                                             SeckillOrderAddDTO seckillOrderAddDTO,
                                             Throwable e) {
        log.error("一个请求被降级了");
        // 输出异常信息
        e.printStackTrace();
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,
                "发生异常,异常信息为:" + e.getMessage());
    }
}

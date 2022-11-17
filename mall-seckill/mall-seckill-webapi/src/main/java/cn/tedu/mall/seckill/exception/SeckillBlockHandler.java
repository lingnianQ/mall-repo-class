package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;

/**
 * 秒杀限流异常处理类
 *
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/16 15:51
 */
@Slf4j
public class SeckillBlockHandler {

    /**
     * 声明限流的方法,返回值必须和被限流的控制方法一致
     * 参数要包含所有被限流的控制器方法的参数,还可以额外声明一个BlockException
     * 在其他类中声明的限流方法,在控制器中调用的,要声明为静态,否则无法实现
     *
     * @param randCode
     * @param seckillOrderAddDTO
     * @param e
     * @return
     */
    public static JsonResult seckillBlock(String randCode,
                                          SeckillOrderAddDTO seckillOrderAddDTO,
                                          BlockException e) {
        log.error("一个请求被限流了");
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,
                "服务器忙请稍后再试!");
    }

}

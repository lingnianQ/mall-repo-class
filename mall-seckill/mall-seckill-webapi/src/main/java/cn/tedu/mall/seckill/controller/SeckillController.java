package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.exception.SeckillBlockHandler;
import cn.tedu.mall.seckill.exception.SeckillFallBack;
import cn.tedu.mall.seckill.service.ISeckillService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckill")
@Api(tags = "执行秒杀操作")
public class SeckillController {

    @Autowired
    private ISeckillService service;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/{randCode}")
    @ApiOperation("随机码验证并提交订单")
    @ApiImplicitParam(value = "随机码",name="randCode",required = true,
                        dataType = "string")
    @PreAuthorize("hasRole('user')")
    // Sentinel限流和降级配置
    @SentinelResource(value = "seckill",
      blockHandlerClass = SeckillBlockHandler.class,blockHandler = "seckillBlock",
      fallbackClass = SeckillFallBack.class,fallback = "seckillFallback")
    public JsonResult<SeckillCommitVO> commitSeckill(){
        return null;
    }

}

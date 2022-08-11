package cn.tedu.mall.seckill.controller;

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
@RequestMapping("/skill")
@Api(tags = "提交秒杀订单")
public class SeckillController {

    @Autowired
    private ISeckillService seckillService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/{randCode}")
    @ApiOperation("随机码验证并提交订单")
    @ApiImplicitParam(value = "随机码",name="randCode",required = true,dataType = "string")
    @PreAuthorize("hasRole('user')")
    // sentinel限流和降级配置
    @SentinelResource()


}

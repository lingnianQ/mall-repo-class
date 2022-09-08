package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.exception.SeckillBlockHandler;
import cn.tedu.mall.seckill.exception.SeckillFallBack;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.apache.http.ContentTooLongException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
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
    public JsonResult<SeckillCommitVO> commitSeckill(
            @PathVariable String randCode,
            @Validated SeckillOrderAddDTO seckillOrderAddDTO){
        // 获得spuId
        Long spuId=seckillOrderAddDTO.getSpuId();
        // 获得当前spuId对应的随机码的key
        // mall:seckill:spu:url:rand:code:2
        String randCodeKey= SeckillCacheUtils.getRandCodeKey(spuId);
        // 判断这个Key是否存在
        if(redisTemplate.hasKey(randCodeKey)){
            // 如果Key存在,取出这个key的值
            String redisRandCode=
                    redisTemplate.boundValueOps(randCodeKey).get()+"";
            // 判断前端传统的随机码和redis保存的随机码是否一致
            if(!redisRandCode.equals(randCode)){
                // 两个随机码不一致,不允许执行购买业务
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                        "没有指定商品");
            }
            // 执行购买操作
            SeckillCommitVO commitVO=service.commitSeckill(seckillOrderAddDTO);
            return JsonResult.ok(commitVO);
        }else{
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                    "没有指定商品");
        }
    }

}

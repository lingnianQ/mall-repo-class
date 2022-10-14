package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.exception.SeckillBlockHandler;
import cn.tedu.mall.seckill.exception.SeckillFallback;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
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
    private ISeckillService seckillService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/{randCode}")
    @ApiOperation("随机码验证并提交订单")
    @ApiImplicitParam(value = "随机码",name="randCode",required = true)
    @PreAuthorize("hasRole('user')")
    // Sentinel配置限流和降级
    @SentinelResource(value = "seckill",
      blockHandlerClass = SeckillBlockHandler.class,blockHandler = "seckillBlock",
      fallbackClass = SeckillFallback.class,fallback = "seckillFallback")
    public JsonResult<SeckillCommitVO> commitSeckill(
            @PathVariable String randCode,
            @Validated SeckillOrderAddDTO seckillOrderAddDTO){
        // 获得spuId
        Long spuId=seckillOrderAddDTO.getSpuId();
        // 从Redis中获取spuId对应的随机码
        // 先获得Key mall:seckill:spu:url:rand:code:10
        String randCodeKey= SeckillCacheUtils.getRandCodeKey(spuId);
        // 判断redis中是否有这个key
        if(redisTemplate.hasKey(randCodeKey)){
            // 如果redis中有这个key,取出这个key的值
            String redisRandCode=
                    redisTemplate.boundValueOps(randCodeKey).get()+"";
            // 判断如果redis中的随机码和控制器方法参数接收的随机码不同
            if(!redisRandCode.equals(randCode)){
                //  两个随机码不一致,不允许运行购买业务
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                        "没有找到指定商品");
            }
            // 运行到此处表示redis中的随机码和传入的随机码一致,可以执行购买
            SeckillCommitVO commitVO=
                        seckillService.commitSeckill(seckillOrderAddDTO);
            return JsonResult.ok(commitVO);
        }else{
            // 当redis中没有保存这个spuId商品的信息时,也是抛出异常
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                    "没有找到指定商品");
        }

    }


}

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
@Api(tags = "执行秒杀模块")
public class SeckillController {

    @Autowired
    private ISeckillService seckillService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/{randCode}")
    @ApiOperation("验证随机码并提交秒杀订单")
    @ApiImplicitParam(value = "随机码", name = "randCode", required = true)
    @PreAuthorize("hasRole('user')")
    @SentinelResource(value = "seckill",
            blockHandlerClass = SeckillBlockHandler.class, blockHandler = "seckillBlock",
            fallbackClass = SeckillFallBack.class, fallback = "seckillFallback")
    public JsonResult<SeckillCommitVO> commitSeckill(
            @PathVariable String randCode,
            @Validated SeckillOrderAddDTO seckillOrderAddDTO) {
        // 获取spuId
        Long spuId = seckillOrderAddDTO.getSpuId();
        // 声明根据spuId获取随机码的Key
        String randCodeKey = SeckillCacheUtils.getRandCodeKey(spuId);
        // 判断Redis中是否有这个key
        if (redisTemplate.hasKey(randCodeKey)) {
            // redis中有这个key,将它的value(随机码)取出
            String redisRandCode = redisTemplate.boundValueOps(randCodeKey).get() + "";
            // 判断前端发来的随机码和redis保存的随机码是否一致
            if (!redisRandCode.equals(randCode)) {
                // 随机码不一致,抛出异常,终止业务
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                        "没有找到指定商品(随机码不正确)");
            }
            // 运行到此处,表示随机码匹配,可以执行购买
            SeckillCommitVO commitVO =
                    seckillService.commitSeckill(seckillOrderAddDTO);
            return JsonResult.ok(commitVO);

        } else {
            // 当redis中没有保存这个商品的SpuId对应的随机码时,抛出异常
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                    "没有找到指定商品");
        }

    }


}

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckill")
@Api(tags = "提交秒杀订单")
public class SeckillController {

    @Autowired
    private ISeckillService seckillService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/{randCode}")
    @ApiOperation("随机码验证并提交订单")
    @ApiImplicitParam(value = "随机码",name="randCode",required = true
                                                            ,dataType = "string")
    @PreAuthorize("hasRole('user')")
    // sentinel限流和降级配置
    @SentinelResource(value = "seckill",
         blockHandlerClass = SeckillBlockHandler.class,blockHandler = "seckillBlock",
         fallbackClass = SeckillFallBack.class,fallback = "seckillFall")
    public JsonResult<SeckillCommitVO> commitSeckill(
            @PathVariable String randCode, SeckillOrderAddDTO seckillOrderAddDTO){
        // 获得SpuId,用于后面的使用
        Long spuId=seckillOrderAddDTO.getSpuId();
        // 获得当前SpuId对应的随机码的key
        //  mall:seckill:spu:url:rand:code:1
        String randCodeKey= SeckillCacheUtils.getRandCodeKey(spuId);
        // 判断redis中是否包含这个key
        if(redisTemplate.hasKey(randCodeKey)){
            // 如果这个key存在,获得这个Key判断是否正确
            String redisRandCode=redisTemplate.boundValueOps(randCodeKey).get()+"";
            // 为了防止Redis信息丢失,我们可以判断一下redisRandCode的存在
            if(redisRandCode==null){
                // Redis信息丢失
                throw new CoolSharkServiceException(
                        ResponseCode.INTERNAL_SERVER_ERROR,"服务器内部错误,请联系客服");
            }
            // 判断Redis中的随机码和控制器收到的随机码是否一致,防止投机购买
            if(!redisRandCode.equals(randCode)){
                // 如果不一致,判定为投机购买,抛出异常
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                        "没有指定商品");
            }
            // 执行购买操作
            SeckillCommitVO commitVO=seckillService.commitSeckill(seckillOrderAddDTO);
            return JsonResult.ok(commitVO);

        }else{
            // 如果redis中没有这个随机码的key值,直接发送异常,提示没有改商品
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                    "没有指定商品");
        }
    }
}

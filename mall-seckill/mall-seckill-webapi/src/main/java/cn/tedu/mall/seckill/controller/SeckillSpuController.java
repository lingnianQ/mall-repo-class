package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.spring.web.json.Json;

@RestController
@RequestMapping("/seckill/spu")
@Api(tags = "秒杀spu模块")
public class SeckillSpuController {

    @Autowired
    private ISeckillSpuService seckillSpuService;

    @GetMapping("/list")
    @ApiOperation("分页查询秒杀spu商品列表")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "页码",name="page",example = "1"),
            @ApiImplicitParam(value = "每页条数",name="pageSize",example = "10")
    })
    public JsonResult<JsonPage<SeckillSpuVO>> listSeckillSpus(
            Integer page,Integer pageSize){
        JsonPage<SeckillSpuVO> jsonPage=seckillSpuService
                .listSeckillSpus(page,pageSize);
        return JsonResult.ok(jsonPage);
    }

    // localhost:10007/seckill/spu/2/detail
    @GetMapping("/{spuId}/detail")
    @ApiOperation("根据spuId查询detail详情")
    @ApiImplicitParam(value = "spuId",name="spuId",example = "2")
    public JsonResult<SeckillSpuDetailSimpleVO> getSeckillDetail(
            @PathVariable Long spuId){
        SeckillSpuDetailSimpleVO simpleVO=
                seckillSpuService.getSeckillSpuDetail(spuId);
        return JsonResult.ok(simpleVO);
    }


}

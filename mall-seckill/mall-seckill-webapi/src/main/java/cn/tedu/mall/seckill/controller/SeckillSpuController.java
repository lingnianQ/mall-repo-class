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

@RestController
@RequestMapping("/seckill/spu")
@Api(tags = "秒杀spu模块")
public class SeckillSpuController {

    @Autowired
    private ISeckillSpuService seckillSpuService;

    @GetMapping("/list")
    @ApiOperation("分页查询秒杀spu列表")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "页码",name = "page",
                                                required = true,dataType = "int"),
            @ApiImplicitParam(value = "每页条数",name = "pageSize",
                                                required = true,dataType = "int")
    })
    public JsonResult<JsonPage<SeckillSpuVO>> listSeckillSpus(
                                                    Integer page,Integer pageSize){
        JsonPage<SeckillSpuVO> list=seckillSpuService.listSeckillSpus(page,pageSize);
        return JsonResult.ok(list);
    }

    //  localhost:10007/seckill/spu/1/detail
    @GetMapping("/{spuId}/detail")
    @ApiOperation("根据SpuId查询Detail信息")
    @ApiImplicitParam(value = "spuId",name="spuId",required = true,
                                            dataType = "long",example ="1")
    public JsonResult<SeckillSpuDetailSimpleVO> getSeckillDetail(
            @PathVariable Long spuId){
        SeckillSpuDetailSimpleVO simpleVO=seckillSpuService.getSeckillSpuDetail(spuId);
        return JsonResult.ok(simpleVO);

    }

    @GetMapping("/{spuId}")
    @ApiOperation("根据spuId查询秒杀spu详情")
    @ApiImplicitParam(value = "spuId",name = "spuId",required = true,
                                    dataType = "long",example = "2")
    public JsonResult<SeckillSpuVO> getSeckillSpuVO(
            @PathVariable Long spuId){
        SeckillSpuVO seckillSpuVO=seckillSpuService.getSeckillSpu(spuId);
        return JsonResult.ok(seckillSpuVO);
    }





}

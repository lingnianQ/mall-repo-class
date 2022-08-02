package cn.tedu.mall.front.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.SpuListItemVO;
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
@RequestMapping("/front/spu")
@Api(tags = "前台商品spu模块")
public class FrontSpuController {

    @Autowired
    private IFrontProductService frontProductService;

    // localhost:10004/front/list/3
    @GetMapping("/list/{categoryId}")
    @ApiOperation("根据分类id分页查询spu列表")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "分类id",name="categoryId",example = "3"
                    ,required = true,dataType = "long"),
            @ApiImplicitParam(value = "页码",name="page",example = "1"
                    ,required = true,dataType = "int"),
            @ApiImplicitParam(value = "每页条数",name="pageSize",example = "2"
                    ,required = true,dataType = "int"),
    })
    public JsonResult<JsonPage<SpuListItemVO>> listSpuByPage(
            @PathVariable Long categoryId,Integer page,Integer pageSize){
        JsonPage<SpuListItemVO> jsonPage=
                frontProductService.listSpuByCategoryId(categoryId,page,pageSize);
        return JsonResult.ok(jsonPage);

    }


}

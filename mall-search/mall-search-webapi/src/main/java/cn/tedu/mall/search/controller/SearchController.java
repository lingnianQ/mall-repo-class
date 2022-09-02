package cn.tedu.mall.search.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@Api(tags = "搜索模块")
public class SearchController {
    @Autowired
    private ISearchService searchService;

    // 搜索模块的功能非常直接,就是搜索
    // 所以它的url路径可以尽量短
    // @GetMapping后面不写任何内容,意思就是只适用类上定义的路径即可访问
    // 访问路径:localhost:10008/search
    @GetMapping
    @ApiOperation("根据用户输入的关键字分页查询spu")
    @ApiImplicitParams({
         @ApiImplicitParam(value = "搜索关键字",name="keyword", dataType="string"),
         @ApiImplicitParam(value = "页码",name="page", dataType="int"),
         @ApiImplicitParam(value = "每页条数",name="pageSize", dataType="int")
    })
    public JsonResult<JsonPage<SpuForElastic>> searchByKeyword(
            String keyword,Integer page, Integer pageSize){
        JsonPage<SpuForElastic> jsonPage=
                searchService.search(keyword,page,pageSize);
        return JsonResult.ok(jsonPage);
    }

}

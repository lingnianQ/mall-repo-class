package cn.tedu.mall.search.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.pojo.search.entity.SpuEntity;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@Api(tags="搜索模块")
public class SearchController {
    @Autowired
    private ISearchService searchService;

    // 搜索模块最核心的功能就是搜索
    // 所以路径可以尽量短
    // 所以我们的GetMapping可以不写任何内容
    // 路径就是控制器类上面定义的内容了:localhost:10008/search
    @GetMapping
    @ApiOperation("根据用户输入关键字分页查询商品spu")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "搜索关键字",name = "keyword",dataType = "string"),
            @ApiImplicitParam(value = "页码",name = "page",dataType = "int"),
            @ApiImplicitParam(value = "每页条数",name = "pageSize",dataType = "int")
    })
    public JsonResult<JsonPage<SpuEntity>> searchByKeyword(String keyword,
                                                           Integer page, Integer pageSize){
        JsonPage<SpuEntity> list=searchService.search(keyword,page,pageSize);
        return JsonResult.ok(list);
    }


}

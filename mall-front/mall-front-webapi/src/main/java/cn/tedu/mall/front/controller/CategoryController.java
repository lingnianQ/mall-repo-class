package cn.tedu.mall.front.controller;


import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 三级分类列表
 *
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/1 19:39
 */
@RestController
@RequestMapping("/front/category")
@Api(tags = "前台分类查询")
public class CategoryController {
    @Autowired
    private IFrontCategoryService categoryService;

    @GetMapping("/all")
    @ApiOperation("查询获得三级分类树对象")
    public JsonResult<FrontCategoryTreeVO<FrontCategoryEntity>> getTreeVO() {
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO =
                categoryService.categoryTree();
        return JsonResult.ok(treeVO);
    }


}

package cn.tedu.mall.order.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.utils.WebConsts;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oms/cart")
@Api(tags = "购物车管理模块")
public class OmsCartController {

    @Autowired
    private IOmsCartService omsCartService;

    @PostMapping("/add")
    @ApiOperation("新增购物车信息")
    // 判断过滤器中对JWT解析出来,并保存在SpringSecurity上下文中的用户
    // 是否具备指定的权限
    // 如果具备,表示当前用户已经登录
    // sso服务器用户登录时,代码中已经写好会保存一个ROLE_user的权限
    @PreAuthorize("hasAuthority('ROLE_user')")
    // @Validated注解是激活SpringValidation框架的验证功能
    // 如果cartAddDTO参数有不符合的值,会抛出BindException异常
    // 这个异常会在统一异常处理类中处理
    public JsonResult addCart(@Validated CartAddDTO cartAddDTO){
        omsCartService.addCart(cartAddDTO);
        return JsonResult.ok("新增sku到购物车完成!");
    }

    // 根据用户Id分页查询购物车sku列表
    @GetMapping("/list")
    @ApiOperation("根据用户Id分页查询购物车sku列表")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "页码",name="page",dataType = "int"),
            @ApiImplicitParam(value = "每页条数",name="pageSize",dataType = "int")
    })
    @PreAuthorize("hasAuthority('ROLE_user')")
    public JsonResult<JsonPage<CartStandardVO>> listCartByPage(
        // 在控制器参数位置添加@RequestParam注解
        // 可以设置当前参数为空时的默认值,WebConsts.DEFAULT_PAGE是事先定义好的常量
        @RequestParam(required = false,defaultValue = WebConsts.DEFAULT_PAGE)
                    Integer page,
        @RequestParam(required = false,defaultValue = WebConsts.DEFAULT_PAGE_SIZE)
                    Integer pageSize){
        // 常规调用业务逻辑层并返回
        JsonPage<CartStandardVO>
                jsonPage=omsCartService.listCarts(page,pageSize);
        return JsonResult.ok(jsonPage);
    }

    @PostMapping("/delete")
    @ApiOperation("根据id数组删除购物车中的sku信息")
    @ApiImplicitParam(value = "要删除的id数组",name="ids",
                        required = true,dataType = "array")
    // 当@PreAuthorize注解后括号中判断参数为hasRole时
    // 相当于在做针对角色(role)的判断,这个写法的效果是对判断内容前(左侧)自动添加"ROLE_"
    // 既@PreAuthorize("hasRole('user')") 写法的最终效果就等价于
    // @PreAuthorize("hasAuthority('ROLE_user')")
    @PreAuthorize("hasRole('user')")
    public JsonResult removeCartsByIds(Long[] ids){
        omsCartService.removeCart(ids);
        return JsonResult.ok("运行了删除功能!");
    }

    // 清空当前用户的购物车
    @PostMapping("/delete/all")
    @ApiOperation("清空当前用户的购物车")
    @PreAuthorize("hasRole('user')")
    public JsonResult removeCartsByUserId(){
        omsCartService.removeAllCarts();
        return JsonResult.ok("购物车已清空");
    }

    // 修改购物车数量
    @PostMapping("/update/quantity")
    @ApiOperation("修改购物车数量")
    @PreAuthorize("hasRole('user')")
    public JsonResult updateQuantity(@Validated CartUpdateDTO cartUpdateDTO){
        omsCartService.updateQuantity(cartUpdateDTO);
        return JsonResult.ok("修改完成!");
    }


}

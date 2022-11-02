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
    // 在程序运行控制方法前,已经运行了过滤器中解析JWT的代码,
    // 解析正确的话,用户信息已经保存在了SpringSecurity上下文中
    // 酷鲨商城前台用户登录时,我们编写的代码会向用户信息中固定设置一个ROLE_user的角色
    // 下面的注解,主要目的是判断用户是否登录,权限统一都是ROLE_user
    // 如果用户没有登录,是无法运行控制方法的!401错误
    @PreAuthorize("hasAuthority('ROLE_user')")
    // @Validated注解是激活SpringValidation框架用的
    // 参数CartAddDTO中,各个属性设置了验证规则,如果有参数值不符合规则
    // 会抛出BindException异常,之后会运行统一异常处理类中专门的方法,控制器方法终止
    public JsonResult addCart(@Validated CartAddDTO cartAddDTO) {
        omsCartService.addCart(cartAddDTO);
        return JsonResult.ok("新增sku到购物车完成!");
    }

    @GetMapping("/list")
    @ApiOperation("根据用户Id分页查询购物车sku列表")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "页码", name = "page", example = "1"),
            @ApiImplicitParam(value = "每页条数", name = "pageSize", example = "10")
    })
    @PreAuthorize("hasAuthority('ROLE_user')")
    public JsonResult<JsonPage<CartStandardVO>> listCartsByPage(
            // 控制器方法中的参数可以使用@RequestParam注解来赋默认值
            // WebConsts是我们自己编写的常量类,DEFAULT_PAGE:1 DEFAULT_PAGE_SIZE:20
            @RequestParam(required = false, defaultValue = WebConsts.DEFAULT_PAGE)
                    Integer page,
            @RequestParam(required = false, defaultValue = WebConsts.DEFAULT_PAGE_SIZE)
                    Integer pageSize) {
        JsonPage<CartStandardVO> jsonPage =
                omsCartService.listCarts(page, pageSize);
        return JsonResult.ok(jsonPage);

    }

    @PostMapping("/delete")
    @ApiOperation("根据id数组删除购物车中的sku信息")
    @ApiImplicitParam(value = "包含要删除id的数组",name = "ids",
            required = true, dataType = "array")
    // 当@PreAuthorize注解后面要判断的权限内容以ROLE_开头时
    // 表示我们判断的内容是SpringSecurity框架约定的角色
    // 我们可以在@PreAuthorize注解()里使用hasRole来简化对角色的判断
    // hasRole('user')这样的判断会检查当前登录用户是否有ROLE_user这个角色
    // 也就是会自动在user前加ROLE_来判断
    // @PreAuthorize("hasAuthority('ROLE_user')")
    @PreAuthorize("hasRole('user')")
    public JsonResult removeCartsByIds(Long[] ids){
        omsCartService.removeCart(ids);
        return JsonResult.ok("删除完成!");
    }

    @PostMapping("/delete/all")
    @ApiOperation("清空当前登录用户购物车中商品")
    @PreAuthorize("hasRole('user')")
    public JsonResult removeCartsByUserId(){
        omsCartService.removeAllCarts();
        return JsonResult.ok("购物车已清空");
    }

    @PostMapping("/update/quantity")
    @ApiOperation("修改购物车中sku数量")
    @PreAuthorize("hasRole('user')")
    public JsonResult updateQuantity(@Validated CartUpdateDTO cartUpdateDTO){
        omsCartService.updateQuantity(cartUpdateDTO);
        return JsonResult.ok("修改完成!");
    }

}

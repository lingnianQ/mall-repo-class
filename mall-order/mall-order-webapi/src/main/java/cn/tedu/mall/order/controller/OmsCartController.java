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
    // 正常登录的用户在运行控制器方法前,已经在过滤器中将用户信息保存到了SpringSecurity
    // 下面我们要判断SpringSecurity中是否具备用户权限
    // sso服务器在用户登录时,会默认将用户权限设置为"ROLE_user"
    // 如果用户没有登录,就无法访问这个控制器方法
    @PreAuthorize("hasAuthority('ROLE_user')")
    // @Validated注解是激活SpringValidation框架用的
    // 参数CartAddDTO参数中,已经编写好一些验证规则,如果传入的参数有不符合这些规则的
    // 会抛出BindException异常,之后会由统一异常处理类处理,控制器方法终止
    public JsonResult addCart(@Validated CartAddDTO cartAddDTO){
        omsCartService.addCart(cartAddDTO);
        return JsonResult.ok("新增sku到购物车完成!!");
    }

    @GetMapping("/list")
    @ApiOperation("根据用户id分页查询购物车sku列表")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "页码",name="page",example = "1"),
            @ApiImplicitParam(value = "每页条数",name="pageSize", example = "3")
    })
    @PreAuthorize("hasAuthority('ROLE_user')")
    public JsonResult<JsonPage<CartStandardVO>> listCartsByPage(
            // 控制器方法参数可以添加@RequestParam注解来设置默认值
            // WebConsts.DEFAULT_PAGE是当page参数为null时,事先定义好的常量
            @RequestParam(required = false,defaultValue = WebConsts.DEFAULT_PAGE)
            Integer page,
            @RequestParam(required = false,defaultValue = WebConsts.DEFAULT_PAGE_SIZE)
            Integer pageSize){
        // 常规调用业务逻辑层方法即可
        JsonPage<CartStandardVO> jsonPage=omsCartService.listCarts(page,pageSize);
        return JsonResult.ok(jsonPage);
    }

    @PostMapping("/delete")
    @ApiOperation("根据id数组删除购物车中的sku信息")
    @ApiImplicitParam(value = "要删除的id数组",name="ids",
                            required = true,dataType = "array")
    // 当@PreAuthorize注解后括号里写hasRole来判断时
    // 相当于在针对用户的角色(ROLE)的判断,它的变化是会对判断内容前(左侧)自动添加"ROLE_"
    // 也就是会判断当前用户是否包含ROLE_user的权限,等价了之前的写法
    // @PreAuthorize("hasAuthority('ROLE_user')")
    @PreAuthorize("hasRole('user')")
    public JsonResult removeCartsByIds(Long[] ids){
        omsCartService.removeCart(ids);
        return JsonResult.ok("删除功能运行完毕");
    }

    @PostMapping("/delete/all")
    @ApiOperation("清空当前登录用户的购物车")
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

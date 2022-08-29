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
import springfox.documentation.spring.web.json.Json;

@RestController
@RequestMapping("/oms/cart")
@Api(tags = "购物车管理模块")
public class OmsCartController {
    @Autowired
    private IOmsCartService omsCartService;

    @PostMapping("/add")
    @ApiOperation("新增购物车信息")
    // 判断当前用户是否登录,并具备普通用户权限ROLE_user
    // 访问前台的普通用户,在sso服务器登录获得JWT时,就已经在权限列表中添加ROLE_user的权限了
    @PreAuthorize("hasAuthority('ROLE_user')")
    // 参数cartAddDTO类似,是需要经过SpringValidation框架验证的
    // @Validated注解能够在控制器方法参数中编写,并激活对应的类型的验证过程
    // 如果验证不通过会由我们编写的统一异常处理类中BindException异常处理
    public JsonResult addCart(@Validated CartAddDTO cartAddDTO){
        omsCartService.addCart(cartAddDTO);
        return JsonResult.ok("新增购物车sku完成");
    }

    // 分页查询用户购物车中商品信息
    @GetMapping("/list")
    @ApiOperation("分页查询用户购物车中商品信息")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "页码",name = "page",dataType = "int"),
            @ApiImplicitParam(value = "每页条数",name = "pageSize",dataType = "int")
    })
    // 当@PreAuthorize括号后判断参数为hasRole时
    // 是针对于角色(ROLE)的判断方式,它的效果是会自动在给定的角色名称前加"ROLE_"
    // 最终效果 @PreAuthorize("hasRole('user')") 等价于 @PreAuthorize("hasAuthority('ROLE_user')")
    @PreAuthorize("hasRole('user')")
    public JsonResult<JsonPage<CartStandardVO>> listCartByPage(
            // 控制器参数中,实际上也可以判断某个属性是否为空,并给定默认值
            @RequestParam(required = false,defaultValue = WebConsts.DEFAULT_PAGE)
            Integer page,
            @RequestParam(required = false,defaultValue = WebConsts.DEFAULT_PAGE_SIZE)
            Integer pageSize){
        // 正常调用业务逻辑层并返回
        JsonPage<CartStandardVO>
                jsonPage=omsCartService.listCarts(page,pageSize);
        return JsonResult.ok(jsonPage);

    }

    // 根据id的数组删除购物车中sku商品的方法
    @PostMapping("/delete")
    @ApiOperation("根据id的数组删除购物车中sku商品")
    @ApiImplicitParam(value = "要删除的购物车id数组",name = "ids",
                                required = true,dataType = "array")
    @PreAuthorize("hasRole('ROLE_user')")
    public JsonResult removeCartsByIds(Long[] ids){
        omsCartService.removeCart(ids);
        return JsonResult.ok();
    }

    // 清空当前登录用户的购物车
    @PostMapping("/delete/all")
    @ApiOperation("清空当前登录用户的购物车")
    @PreAuthorize("hasRole('user')")
    public JsonResult removeCartsByUserId(){
        omsCartService.removeAllCarts();
        return JsonResult.ok("购物车清空完成");
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

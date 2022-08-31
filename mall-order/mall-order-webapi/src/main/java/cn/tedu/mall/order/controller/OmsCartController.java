package cn.tedu.mall.order.controller;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}

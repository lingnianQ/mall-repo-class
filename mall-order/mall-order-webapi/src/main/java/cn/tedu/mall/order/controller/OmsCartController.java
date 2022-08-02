package cn.tedu.mall.order.controller;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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

}

package cn.tedu.mall.front.controller;

import cn.tedu.mall.front.service.IFrontProductService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/front/spu")
@Api(tags = "前台商品spu模块")
public class FrontSpuController {

    @Autowired
    private IFrontProductService frontProductService;


}

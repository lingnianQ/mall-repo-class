package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class OmsCartServiceImpl implements IOmsCartService {

    @Override
    public void addCart(CartAddDTO cartDTO) {

    }

    @Override
    public JsonPage<CartStandardVO> listCarts(Integer page, Integer pageSize) {
        return null;
    }

    @Override
    public void removeCart(Long[] ids) {

    }

    @Override
    public void removeAllCarts() {

    }

    @Override
    public void removeUserCarts(OmsCart omsCart) {

    }

    @Override
    public void updateQuantity(CartUpdateDTO cartUpdateDTO) {

    }

    // 业务逻辑层获得用户信息的方法,因为多个方法需要获得用户信息,所以单独编写一个方法
    // 这个方法的实现是SpringSecurity提供的登录用户的容器
    // 方法的目标是获得SpringSecurity用户容器,从容器中获得用户信息
    public CsmallAuthenticationInfo getUserInfo(){
        // 获得SpringSecurity上下文(容器)对象
        UsernamePasswordAuthenticationToken authenticationToken=
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext()
                        .getAuthentication();
        if(authenticationToken==null){
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED,"没有登录信息");
        }
        // 如果authenticationToken不为空,获得其中的用户信息
        CsmallAuthenticationInfo csmallAuthenticationInfo=
                (CsmallAuthenticationInfo) authenticationToken.getCredentials();
        // 返回登录用户信息
        return csmallAuthenticationInfo;
    }
    // 业务逻辑层大多数方法都是需要获得用户Id,所以我们编写一个方法,专门返回当前登录用户的id
    public Long getUserId(){
        return getUserInfo().getId();
    }



}

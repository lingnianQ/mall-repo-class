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
import lombok.extern.slf4j.Slf4j;
import cn.tedu.mall.order.mapper.OmsCartMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/2 15:46
 */
@Service
@Slf4j
public class OmsCartServiceImpl implements IOmsCartService {

    @Autowired
    private OmsCartMapper omsCartMapper;

    /**
     * 新增购物车
     *
     * @param cartDTO
     */
    @Override
    public void addCart(CartAddDTO cartDTO) {

    }

    /**
     * 查询我的购物车
     *
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public JsonPage<CartStandardVO> listCarts(Integer page, Integer pageSize) {
        return null;
    }

    /**
     * 批量删除购物车
     *
     * @param ids
     */
    @Override
    public void removeCart(Long[] ids) {

    }

    /**
     * 清空购物车
     */
    @Override
    public void removeAllCarts() {

    }

    /**
     * TODO 可以和removeAllCarts合并
     *
     * @param omsCart
     */
    @Override
    public void removeUserCarts(OmsCart omsCart) {

    }

    /**
     * 更新购物车商品数量
     *
     * @param cartUpdateDTO
     */
    @Override
    public void updateQuantity(CartUpdateDTO cartUpdateDTO) {

    }

    /**
     * 业务逻辑层中有获得当前登录用户信息的需求
     * 我们的项目会在控制器方法运行前运行的过滤器中,解析前端传入的JWT
     * 将解析获得的用户信息保存在SpringSecurity上下文中
     * 这里可以编写方法从SpringSecurity上下文中获得用户信息
     *
     * @return csmallAuthenticationInfo
     */
    public CsmallAuthenticationInfo getUserInfo() {
        // 编写SpringSecurity上下文中获得用户信息的代码
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
        // 为了逻辑严谨性,判断一下SpringSecurity上下文中的信息是不是null
        if (authenticationToken == null) {
            throw new CoolSharkServiceException(
                    ResponseCode.UNAUTHORIZED, "您没有登录!");
        }
        // 确定authenticationToken不为null
        // 就可以从中获得用户信息了
        CsmallAuthenticationInfo csmallAuthenticationInfo =
                (CsmallAuthenticationInfo) authenticationToken.getCredentials();
        // 别忘了返回
        return csmallAuthenticationInfo;
    }

    /**
     * 业务逻辑层中的方法实际上都只需要用户的id即可
     * 我们可以再编写一个方法,从用户对象中获得id
     *
     * @return getUserInfo().getId();
     */
    public Long getUserId() {
        return getUserInfo().getId();
    }

}

package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.mapper.OmsCartMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OmsCartServiceImpl implements IOmsCartService {

    // 装配Mapper操作数据
    @Autowired
    private OmsCartMapper omsCartMapper;

    @Override
    public void addCart(CartAddDTO cartDTO) {
        // 在查询购物车中是否有商品之前,必须先明确用户身份,也就是用户ID
        Long userId=getUserId();
        // 根据用户ID和skuId检查当前用户购物车中是否已经存在该商品
        OmsCart omsCart=omsCartMapper.selectExistsCart(userId,cartDTO.getSkuId());
        // 判断omsCart是不是null
        if(omsCart==null){
            // 如果omsCart为null,表示当前用户没有添加过这个商品,所以执行新增操作
            // 因为新增操作方法的参数是一个OmsCart类型的对象,所以我们要先实例化出来
            OmsCart newCart=new OmsCart();
            // 将参数CartAddDTO对象的同名属性赋值给newCart
            BeanUtils.copyProperties(cartDTO,newCart);
            // CartAddDTO没有userId属性,需要单独赋值
            newCart.setUserId(userId);
            // 执行新增
            omsCartMapper.saveCart(newCart);
        }else{
            // 如果omsCart不是null,表示当前用户之前已经将该sku添加到购物车中了
            // 这种情况下,我们需要做的就是修改购物车中该sku的数量即可
            // 是要在原有的商品数量基础上,再加本次新增商品的数量
            // mapper中直接将值赋给数据库,所有在java代码里要将这个数量计算完成
            omsCart.setQuantity(omsCart.getQuantity()+cartDTO.getQuantity());
            // 然后调用修改购物车数量的方法
            omsCartMapper.updateQuantityById(omsCart);
        }
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

    // 业务逻辑层中有获得当前登录用户信息的需求
    // 我们的程序在控制器方法运行前执行的过滤器,过滤器中解析了请求头中包含的JWT
    // 解析获得JWT的用户信息后保存到了SpringSecurity的上下文中
    // 所以我们可以从SpringSecurity的上下文中获得用户信息
    public CsmallAuthenticationInfo getUserInfo(){
        // 编码获得SpringSecurity上下文中保存的权限
        UsernamePasswordAuthenticationToken authenticationToken=
                (UsernamePasswordAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
        // 为了保险起见,判断一下从SpringSecurity中获得的信息是不是null
        if(authenticationToken == null){
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED,
                    "请您先登录!");
        }
        // 上下文信息确定存在后,获取其中的用户信息
        // 这个信息就是有JWT解析获得的
        CsmallAuthenticationInfo csmallAuthenticationInfo=
                (CsmallAuthenticationInfo) authenticationToken.getCredentials();
        // 返回登录信息
        return csmallAuthenticationInfo;
    }
    // 业务逻辑层大多数方法需要用户的信息实际上就是用户的ID,编写一个只返回用户ID的方法方便调用
    public Long getUserId(){
        return getUserInfo().getId();
    }






}

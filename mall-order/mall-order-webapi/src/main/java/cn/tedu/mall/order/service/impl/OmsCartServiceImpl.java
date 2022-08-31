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
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.omg.CORBA.CTX_RESTRICT_SCOPE;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OmsCartServiceImpl implements IOmsCartService {

    @Autowired
    private OmsCartMapper omsCartMapper;

    @Override
    public void addCart(CartAddDTO cartDTO) {
        // 获得当前登录用户id
        Long userId=getUserId();
        // 先检查要添加到购物车中的sku是否已经在当前用户的购物车中了
        OmsCart omsCart=omsCartMapper.selectExistsCart(userId,cartDTO.getSkuId());
        // 判断omsCart是否为null
        if(omsCart==null){
            // 如果oms为null,表示当前用户这个sku没有新增到购物车中,执行新增操作
            // 执行新增,新增方法的参数是omsCart,所以要实例化一个omsCart对象
            OmsCart newOmsCart=new OmsCart();
            // 将CartAddDTO对象的同名属性赋值到newOmsCart中
            BeanUtils.copyProperties(cartDTO,newOmsCart);
            // CartAddDTO对象是没有userId的,需要单独赋值
            newOmsCart.setUserId(userId);
            // 执行新增
            omsCartMapper.saveCart(newOmsCart);
        }else{
            // omsCart不是null,表示当前用户选择的sku已经在购物车中
            // 那么我们需要做的就是修改购物车中sku的数量
            // 因为我们编写的修改数量的方法是直接赋值给数据库,
            // 所以要赋的值需要在java代码中计算好
            omsCart.setQuantity(omsCart.getQuantity()+cartDTO.getQuantity());
            // 调用修改购物车数量的方法
            omsCartMapper.updateQuantityById(omsCart);
        }
    }

    // 根据用户id分页查询当前用户购物车sku商品列表
    @Override
    public JsonPage<CartStandardVO> listCarts(Integer page, Integer pageSize) {
        // 从SpringSecurity上下文中获得用户id
        Long userId=getUserId();
        // PageHelper框架设置分页条件
        PageHelper.startPage(page,pageSize);
        // 执行查询,会自动在查询sql语句末尾,添加limit关键字
        List<CartStandardVO> list=omsCartMapper.selectCartByUserId(userId);
        // 将分页结果返回,实例化PageInfo对象,转换为JsonPage
        return JsonPage.restPage(new PageInfo<>(list));
    }

    @Override
    public void removeCart(Long[] ids) {
        // 调用mapper删除的方法,完成按ids数组删除购物车的功能
        int rows=omsCartMapper.deleteCartsByIds(ids);
        if(rows==0){
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                    "您要删除的商品不存在");
        }
    }

    @Override
    public void removeAllCarts() {
        Long userId=getUserId();
        int rows=omsCartMapper.deleteCartsByUserId(userId);
        if(rows==0){
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                                        "您的购物车已经是空的了!");
        }
    }

    @Override
    public void removeUserCarts(OmsCart omsCart) {

    }

    @Override
    public void updateQuantity(CartUpdateDTO cartUpdateDTO) {

    }


    // 业务逻辑层中获得用户信息的方法
    // 目标是从SpringSecurity上下文中获取由JWT解析而来的对象
    public CsmallAuthenticationInfo getUserInfo(){
        // 声明SpringSecurity上下文对象
        UsernamePasswordAuthenticationToken authenticationToken=
                (UsernamePasswordAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
        // 为了保险起见,判断一下这个对象是否为空
        if(authenticationToken==null){
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED,"没有登录");
        }
        // 从上下文中获取登录用户的信息
        // 这个信息是由JWT解析获得的
        CsmallAuthenticationInfo csmallAuthenticationInfo=
                (CsmallAuthenticationInfo) authenticationToken.getCredentials();
        // 返回登录信息
        return csmallAuthenticationInfo;
    }
    // 业务逻辑层大多数方法都是只需要用户的ID,所以专门编写一个方法返回id
    public Long getUserId(){
        return getUserInfo().getId();
    }




}

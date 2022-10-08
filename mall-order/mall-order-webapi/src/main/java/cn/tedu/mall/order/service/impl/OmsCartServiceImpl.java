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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

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

    // 根据登录用户id分页查询购物车中商品信息
    @Override
    public JsonPage<CartStandardVO> listCarts(Integer page, Integer pageSize) {
        // 从SpringSecurity中获得用户id
        Long userId=getUserId();
        // PageHelper设置分页查询条件
        PageHelper.startPage(page,pageSize);
        // 执行查询,这次查询会自动在sql语句末尾添加limit关键字完成分页
        List<CartStandardVO> list=omsCartMapper.selectCartsByUserId(userId);
        // list就是分页查询的数据,下面要将分页数据和分页信息转换为JsonPage返回
        return JsonPage.restPage(new PageInfo<>(list));
    }

    // (批量)删除购物车中sku商品信息的方法
    @Override
    public void removeCart(Long[] ids) {
        // 调用mapper中删除购物车sku信息的方法
        int rows=omsCartMapper.deleteCartsByIds(ids);
        if(rows==0){
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                    "您要删除的商品已经删除了!");
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
        // 直接调用OmsCart删除购物车的方法即可
        // 因为用户有可能是点击立即购买来生成订单,这样购物车中就不需要删除任何内容
        omsCartMapper.deleteCartByUserIdAndSkuId(omsCart);
    }

    @Override
    public void updateQuantity(CartUpdateDTO cartUpdateDTO) {
        // 因为执行修改的方法参数要求为OmsCart
        // 但是当前业务逻辑方法参数为CartUpdateDTO
        // 所以需要先实例化OmsCart对象
        OmsCart omsCart=new OmsCart();
        // 将CartUpdateDTO对象中的同名属性,赋值到omsCart中
        BeanUtils.copyProperties(cartUpdateDTO,omsCart);
        // 执行修改
        omsCartMapper.updateQuantityById(omsCart);
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

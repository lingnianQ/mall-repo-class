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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OmsCartServiceImpl implements IOmsCartService {

    @Autowired
    private OmsCartMapper omsCartMapper;

    @Override
    public void addCart(CartAddDTO cartDTO) {
        // 获得当前登录用户的id
        Long userId=getUserId();
        // 查询这个userId的用户是否已经将指定的sku添加到购物车
        OmsCart omsCart=omsCartMapper.selectExistsCart(userId,cartDTO.getSkuId());
        // 判断查询结果是否为null
        if(omsCart!=null){
            // 如果omsCart不是空,证明数据库表中已经包含这个sku商品
            // 那么我们做的就是修改购物车中sku的数量了
            // 将omsCart对象中的quantity和cartDTO中的quantity相加,然后赋值给omsCart的属性
            omsCart.setQuantity(omsCart.getQuantity()+cartDTO.getQuantity());
            // 调用修改购物车中sku数量的方法
            omsCartMapper.updateQuantityById(omsCart);
        }else{
            // 如果当前用户购物车中不存在指定的skuId,就进行新增操作
            // 执行新增购物车信息的参数是OmsCart对象
            // 现在的参数是cartDTO,使用BeanUtils赋值
            OmsCart newOmsCart=new OmsCart();
            BeanUtils.copyProperties(cartDTO,newOmsCart);
            // cartDTO中没有用户id信息的
            newOmsCart.setUserId(userId);
            // 执行新增
            omsCartMapper.saveCart(newOmsCart);
        }

    }

    // 根据用户id分页查询当前用户购物车sku商品列表
    @Override
    public JsonPage<CartStandardVO> listCarts(Integer page, Integer pageSize) {
        // 从SpringSecurity中获得用户id
        Long userId=getUserId();
        // PageHelper框架设置分页条件
        PageHelper.startPage(page,pageSize);
        // 执行查询,会自动在sql语句后生成limit关键字
        List<CartStandardVO> list=omsCartMapper.selectCartsByUserId(userId);
        // 将分页结果对象PageInfo转换为JsonPage返回
        return JsonPage.restPage(new PageInfo<>(list));
    }

    @Override
    public void removeCart(Long[] ids) {
        // 接收mapper执行删除时的返回值,也是删除成功的行数
        int rows=omsCartMapper.deleteCartsByIds(ids);
        if(rows==0){
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"您要删除的商品不存在");
        }
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

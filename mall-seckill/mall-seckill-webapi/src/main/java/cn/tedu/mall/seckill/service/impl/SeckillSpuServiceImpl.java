package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.product.vo.SpuStandardVO;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSpuService;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/13 11:05
 */
@Slf4j
@Service
public class SeckillSpuServiceImpl implements ISeckillSpuService {

    //  装配查询秒杀Spu列表的Mapper
    @Autowired
    private SeckillSpuMapper seckillSpuMapper;

    // 查询方法的返回值泛型为SeckillSpuVO,其中包含很多普通spu表中的信息
    // 所以我们还要在代码中Dubbo调用根据spuId查询普通spu信息的方法,还是product模块
    @DubboReference
    private IForSeckillSpuService dubboSeckillSpuService;

    @Override
    public JsonPage<SeckillSpuVO> listSeckillSpus(Integer page, Integer pageSize) {
        // 设置分页条件,准备分页查询
        PageHelper.startPage(page, pageSize);
        // 然后进行查询,获得分页数据
        List<SeckillSpu> seckillSpus = seckillSpuMapper.findSeckillSpus();
        // 我们要实例化一个SeckillSpuVO泛型的集合,以备赋值和最后的返回
        List<SeckillSpuVO> seckillSpuVOList = new ArrayList<>();
        // 遍历从数据库中查询出的所有秒杀商品列表
        for (SeckillSpu seckillSpu : seckillSpus) {
            // 获得秒杀商品的spuId
            Long spuId = seckillSpu.getSpuId();
            // 根据这个SpuId利用dubbo去查询这个spu的常规信息
            SpuStandardVO spuStandard = dubboSeckillSpuService.getSpuById(spuId);
            // 秒杀信息在seckillSpu中,常规信息在spuStandardVO中
            // 下面就是要将上面两个对象的属性都赋值到SeckillSpuVO对象中
            SeckillSpuVO seckillSpuVO = new SeckillSpuVO();
            // 常规信息都在spuStandardVO中,所以直接将同名属性赋值
            BeanUtils.copyProperties(spuStandard, seckillSpuVO);
            // 秒杀信息要单独赋值,因为不全是同名属性
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            // 到此为止seckillSpuVO包含了常规spu信息和秒杀spu信息
            // 将这个对象保存到集合
            seckillSpuVOList.add(seckillSpuVO);
        }
        // 最后别忘了返回
        return JsonPage.restPage(new PageInfo<>(seckillSpuVOList));
    }

    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {
        return null;
    }

    @Override
    public SeckillSpuDetailSimpleVO getSeckillSpuDetail(Long spuId) {
        return null;
    }
}

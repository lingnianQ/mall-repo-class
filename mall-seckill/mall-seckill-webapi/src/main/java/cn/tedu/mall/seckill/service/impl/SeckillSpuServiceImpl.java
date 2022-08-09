package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.product.vo.SpuDetailStandardVO;
import cn.tedu.mall.pojo.product.vo.SpuStandardVO;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSpuService;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillSpuServiceImpl implements ISeckillSpuService {

    // 查询秒杀表的spu数据
    @Autowired
    private SeckillSpuMapper seckillSpuMapper;
    // 秒杀spu表中,没有商品的详情介绍,需要根据spuid查询spu详情,所以需要Dubbo支持
    // 从mall_pms数据库查询spu详细信息
    @DubboReference
    private IForSeckillSpuService dubboSeckillSpuService;

    @Override
    public JsonPage<SeckillSpuVO> listSeckillSpus(Integer page, Integer pageSize) {
        // 分页查询秒杀表中spu信息
        // 设置分页信息
        PageHelper.startPage(page,pageSize);
        List<SeckillSpu> seckillSpus=seckillSpuMapper.findSeckillSpus();
        // 我们需要将seckillSpus集合中的所有对象,转换为SeckillSpuVO类型对象才能返回
        // 所以我们下面要准备遍历seckillSpus集合,并其中所有spu商品详情查询并赋值到SeckillSpuVO对象中
        List<SeckillSpuVO> seckillSpuVOs=new ArrayList<>();
        for(SeckillSpu seckillSpu : seckillSpus) {
            // 获得SpuId
            Long spuId = seckillSpu.getSpuId();
            // dubbo查询商品详情
            SpuStandardVO spuStandardVO = dubboSeckillSpuService.getSpuById(spuId);
            // 将dubbo查询出来的对象大部分的同名属性,赋值给SeckillSpuVO对象
            SeckillSpuVO seckillSpuVO = new SeckillSpuVO();
            BeanUtils.copyProperties(spuStandardVO, seckillSpuVO);
            // 下面将秒杀表的相关属性赋值给seckillSpuVO对象
            // 赋值秒杀价
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            // 赋值其实时间和结束时间
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            // 将既包含商品信息,又包含秒杀信息的seckillSpuVO保存到集合中
            seckillSpuVOs.add(seckillSpuVO);
        }
        // 返回分页结果
        return JsonPage.restPage(new PageInfo<>(seckillSpuVOs));
    }





    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {

        return null;
    }

    // 常量类中,没有定义Detail对应的Key值,所以我们自己定义一个
    public static final String SECKILL_SPU_DETAIL_VO_PREFIX="seckill:spu:detail:vo:";
    @Autowired
    private RedisTemplate redisTemplate;
    // 根据SpuId查询detali详情信息
    @Override
    public SeckillSpuDetailSimpleVO getSeckillSpuDetail(Long spuId) {
        // 常量后面添加spuId
        String seckillDetailKey=SECKILL_SPU_DETAIL_VO_PREFIX+spuId;
        // 声明当前方法规定的返回值对象
        SeckillSpuDetailSimpleVO seckillSpuDetailSimpleVO=null;
        // 判断Redis中是否已经包含这个对象
        if(redisTemplate.hasKey(seckillDetailKey)){
            // 如果Redis中已经有这个key了,直接复制给返回值对象
            seckillSpuDetailSimpleVO=(SeckillSpuDetailSimpleVO) redisTemplate
                                        .boundValueOps(seckillDetailKey).get();
        }else{
            // 如果Redis中没有这个key,就需要到数据库查询这个对象
            SpuDetailStandardVO spuDetailStandardVO=
                            dubboSeckillSpuService.getSpuDetailById(spuId);
            //将spuDetailStandardVO对象中的同名属性赋值给seckillSpuDetailSimpleVO
            seckillSpuDetailSimpleVO=new SeckillSpuDetailSimpleVO();
            BeanUtils.copyProperties(spuDetailStandardVO,seckillSpuDetailSimpleVO);
            //将当前赋值完成的对象保存到Redis中
            redisTemplate.boundValueOps(seckillDetailKey)
                    .set(seckillSpuDetailSimpleVO,125*60+ RandomUtils.nextInt(100),
                                                                TimeUnit.SECONDS);
        }
        // 最后的返回已经要写对
        return seckillSpuDetailSimpleVO;
    }
}

package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.product.vo.SpuDetailStandardVO;
import cn.tedu.mall.pojo.product.vo.SpuStandardVO;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSpuService;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import cn.tedu.mall.seckill.utils.RedisBloomUtils;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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


    @Autowired
    private RedisBloomUtils redisBloomUtils;

    // 根据SpuId查询Spu详情
    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {
        // 这里先判断当前spuId是否在布隆过滤器中
        // 如果不在直接抛出异常
        // 获得本批次的布隆过滤器的key
        // "spu:bloom:filter:2022-08-15"
        String bloomTodayKey=SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        log.info("当前批次的布隆过滤器key为:{}",bloomTodayKey);
        if(!redisBloomUtils.bfexists(bloomTodayKey,spuId+"")){
            // 进入这里表示布隆过滤器中没有这个spuId,防止缓存穿透,抛出异常
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                    "您访问的商品不存在(测试:布隆过滤器)");
        }
        // 声明返回值类型对象
        SeckillSpuVO seckillSpuVO=null;
        // 获得SpuVO对应的key常量
        // mall:seckill:spu:vo:1
        String seckillSpuKey=SeckillCacheUtils.getSeckillSpuVOKey(spuId);
        // 执行判断这个key是否在Redis中
        if(redisTemplate.hasKey(seckillSpuKey)){
            // 如果Redis中已经存在,直接从redis中获得
            seckillSpuVO=(SeckillSpuVO) redisTemplate.boundValueOps(seckillSpuKey).get();
        }else{
            // 如果Redis中不存在数据
            // 就要从两方面查询获得返回值需要的各种属性:1.pms_spu表  2.seckill_spu
            SeckillSpu seckillSpu=seckillSpuMapper.findSeckillSpuById(spuId);
            // 判断spuId查询不到信息的情况(布隆过滤器误判时会进这个if)
            if (seckillSpu==null){
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"您访问的商品不存在");
            }
            // 上面查询的是秒杀信息,下面查询pms_spu表中的信息
            // pms_spu表一定是product模块的dubbo调用
            SpuStandardVO spuStandardVO=dubboSeckillSpuService.getSpuById(spuId);
            // 将spuStandardVO对象的同名属性赋值给seckillSpuVO
            seckillSpuVO=new SeckillSpuVO();
            BeanUtils.copyProperties(spuStandardVO,seckillSpuVO);
            // 下面将秒杀信息赋值
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            // 将赋好值的spuVO对象保存在Redis中
            redisTemplate.boundValueOps(seckillSpuKey).set(seckillSpuVO,
                    125*60*1000+RandomUtils.nextInt(100*1000),TimeUnit.MILLISECONDS);
        }
        // 判断当前时间是否在秒杀时间段内
        // 为了提高效率不再连接数据库
        LocalDateTime nowTime=LocalDateTime.now();
        // 看当前时间是否大于开始时间 并且 小于结束时间
        // 时间对象计算时间差Duration
        // 当时间差为负值时会返回negative
        // 判断当前时间大于开始时间
        Duration afterTime=Duration.between(nowTime,seckillSpuVO.getStartTime());
        // 判断结束时间大于当前时间
        Duration beforeTime=Duration.between(seckillSpuVO.getEndTime(),nowTime);
        // 如果两个对象afterTime和beforeTime都是negative
        // 证明当前时间大于开始时间小于结束时间
        // 可以访问,需要将随机码返回给前端
        if(afterTime.isNegative()  && beforeTime.isNegative()){
            // 从Redis中获得随机码,并赋值到spuVO对象的url属性中
            //  mall:seckill:spu:url:rand:code:1
            String randomCodeKey=SeckillCacheUtils.getRandCodeKey(spuId);
            seckillSpuVO.setUrl("/seckill/"+
                            redisTemplate.boundValueOps(randomCodeKey).get());
        }
        // 别忘了返回
        // 返回的spuVO对象中包含了Url以及随机码,这个随机码会返回给前端保存
        // 前端只有利用这个Url发起的购买请求,才会给控制器接收处理
        return seckillSpuVO;
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

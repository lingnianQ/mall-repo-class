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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    // 装配操作Redis的对象
    @Autowired
    private RedisTemplate redisTemplate;

    // 当前项目中没有定义spuDetail对应Redis的key的常量
    // 要么自己去添加, 要么这里定义一个   PREFIX是"前缀"的意思
    public static final String
            SECKILL_SPU_DETAIL_VO_PREFIX = "seckill:spu:detail:vo:";

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

    /**
     * 根据spuId查询spu详情(返回值包含常规信息和秒杀信息已经随机码)
     *
     * @param spuId
     * @return
     */
    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {
        // 在最终完整版的代码中,要在这里添加布隆过滤器的判断
        // TODO 执行布隆过滤器判断spuId是否存在,如果不存在直接抛出异常

        // 检查spu信息是否已经在Redis中,还是先定Redis的key
        String seckillSpuKey = SeckillCacheUtils.getSeckillSpuVOKey(spuId);
        SeckillSpuVO seckillSpuVO = null;
        if (redisTemplate.hasKey(seckillSpuKey)) {
            seckillSpuVO = (SeckillSpuVO) redisTemplate
                    .boundValueOps(seckillSpuKey)
                    .get();
        } else {
            // redis中没有这个key,完成数据库查询
            // 先根据spuId查询秒杀信息
            SeckillSpu seckillSpu = seckillSpuMapper.findSeckillSpuById(spuId);
            // 判断seckillSpu是否为null(因为布隆过滤器可能产生误判)
            if (seckillSpu == null) {
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND, "您访问的商品不存在");
            }
            // 到此为止,我们已经查询出了spu的秒杀商品信息,下面还要获取常规商品信息
            // dubbo调用product模块的方法获取spu常规信息
            SpuStandardVO spuStandardVO = dubboSeckillSpuService
                    .getSpuById(spuId);
            seckillSpuVO = new SeckillSpuVO();
            // 将常规信息中的同名属性赋值到seckillSpuVO对象中
            BeanUtils.copyProperties(spuStandardVO, seckillSpuVO);
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            // seckillSpuVO对象保存到Redis中
            redisTemplate.boundValueOps(seckillSpuKey)
                    .set(seckillSpuVO, 10 * 60 * 1000 + RandomUtils.nextInt(10000),
                            TimeUnit.MILLISECONDS);

            // 我们观察seckillSpuVO对象,除了url属性外,所有属性都会被赋值了
            // 我们一旦给url赋值,就意味着允许用户指定秒杀购买操作了
            // 所以必须判断当前时间是否在秒杀时间范围内
            // 如果不在,就不需要给url赋值,用户就只能看不能买
            // 获取当前时间判断是否在秒杀时间范围内
            LocalDateTime nowTime = LocalDateTime.now();
            // 因为是秒杀高并发状态,所以尽量不连接数据库
            // 可以使用seckillSpuVO中秒杀开始和结束时间的属性来判断
            // 判断的基本原则是开始时间小于当前时间,当前时间要小于结束时间
            if (seckillSpuVO.getStartTime().isBefore(nowTime) &&
                    nowTime.isBefore(seckillSpuVO.getEndTime())) {
                // 进入if表示当前时间是允许执行秒杀的,可以为url属性赋值
                // 我们从redis中根据当前spuId获取已经预热好的随机码
                String randCodeKey = SeckillCacheUtils.getRandCodeKey(spuId);
                // redis获取随机码
                String randCode = redisTemplate.boundValueOps(randCodeKey).get() + "";
                // 将随机码赋值到url
                seckillSpuVO.setUrl("/seckill/" + randCode);
                log.info("url赋值随机码:{}", randCode);
            }
        }
        return seckillSpuVO;
    }

    /**
     * 查询秒杀spu信息的spuDetail
     *
     * @param spuId
     * @return
     */
    @Override
    public SeckillSpuDetailSimpleVO getSeckillSpuDetail(Long spuId) {
        // 使用常量拼接spuId获得Redis的key
        String seckillSpuDetailKey = SECKILL_SPU_DETAIL_VO_PREFIX + spuId;
        // 先声明一个返回值类型的对象,设值为null即可
        // 因为后面判断无论Redis中有没有这个key,都需要使用这个对象
        SeckillSpuDetailSimpleVO simpleVO = null;
        if (redisTemplate.hasKey(seckillSpuDetailKey)) {
            simpleVO = (SeckillSpuDetailSimpleVO) redisTemplate
                    .boundValueOps(seckillSpuDetailKey).get();
        } else {
            //dubbo调用product查找spuDetail
            SpuDetailStandardVO spuDetailStandardVO =
                    dubboSeckillSpuService.getSpuDetailById(spuId);
            //实例化对象并且赋值
            simpleVO = new SeckillSpuDetailSimpleVO();
            BeanUtils.copyProperties(spuDetailStandardVO, simpleVO);
            //存储到redis
            redisTemplate.boundValueOps(seckillSpuDetailKey)
                    .set(simpleVO, 10 * 60 * 1000 + RandomUtils.nextInt(10000),
                            TimeUnit.MILLISECONDS);
        }
        return simpleVO;
    }
}

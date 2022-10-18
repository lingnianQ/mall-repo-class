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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillSpuServiceImpl implements ISeckillSpuService {

    // 装配查询秒杀spu列表的mapper
    @Autowired
    private SeckillSpuMapper seckillSpuMapper;

    // 要查询pms_spu表中的常规spu信息,需要dubbo调用product模块根据id查询spu的方法
    @DubboReference
    private IForSeckillSpuService dubboSeckillSpuService;

    // 分页查询秒杀商品信息
    // 但是这个方法的返回值泛型SeckillSpuVO中包含了常规spu信息和秒杀spu信息
    // 所以我们的业务中要查询秒杀信息后,再将秒杀商品的常规信息查询出来赋值到返回值中
    @Override
    public JsonPage<SeckillSpuVO> listSeckillSpus(Integer page, Integer pageSize) {
        // 设置分页条件,执行查询秒杀商品列表
        PageHelper.startPage(page,pageSize);
        List<SeckillSpu> seckillSpus=seckillSpuMapper.findSeckillSpus();
        // 最终的返回值集合的泛型是SeckillSpuVO,所以我们要实例化它,后面再给他添加元素
        List<SeckillSpuVO> seckillSpuVOs=new ArrayList<>();
        // 遍历从数据库查询出的秒杀商品集合
        for(SeckillSpu seckillSpu : seckillSpus){
            // 获得秒杀商品的spuId
            Long spuId=seckillSpu.getSpuId();
            // 利用Dubbo查询当前SpuId对应的常规spu信息
            SpuStandardVO spuStandardVO=dubboSeckillSpuService.getSpuById(spuId);
            // 秒杀信息在seckillSpu中,常规信息在spuStandardVO中
            // 我们需要做的就是实例化SeckillSpuVO对象,将上面两个对象的属性都赋值到里面
            SeckillSpuVO seckillSpuVO=new SeckillSpuVO();
            BeanUtils.copyProperties(spuStandardVO,seckillSpuVO);
            // 上面已经将常规spu属性赋值到 seckillSpuVO中
            // 下面要将秒杀信息赋值到seckillSpuVO中
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            // 到此为止seckillSpuVO既包含了常规spu信息,又包含了秒杀spu信息
            // 添加到集合中
            seckillSpuVOs.add(seckillSpuVO);
        }
        // 最后别忘了返回!
        return JsonPage.restPage(new PageInfo<>(seckillSpuVOs));
    }

    @Autowired
    private RedisBloomUtils redisBloomUtils;
    // 根据SpuId查询spu详情(返回值包含秒杀信息和常规信息)
    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {
        // 最终完整版的代码,这里要先获取布隆过滤器
        // 判断参数spuId是否在布隆过滤器中,如果不在直接抛出异常
        // 获取本批次布隆过滤器的key
        String bloomTodayKey=SeckillCacheUtils
                                .getBloomFilterKey(LocalDate.now());
        log.info("当前批次的布隆过滤器key为:{}",bloomTodayKey);
        // 判断要访问的spuId是否在布隆过滤器中
        if(!redisBloomUtils.bfexists(bloomTodayKey,spuId+"")){
            // 进入这个if表示当前spuId不在布隆过滤器中
            // 防止缓存穿透,抛出异常
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                                    "您访问的商品不存在(布隆过滤器判断)");
        }
        // 获取要使用的spu对应的常量
        // mall:seckill:spu:vo:2
        String seckillSpuKey= SeckillCacheUtils.getSeckillSpuVOKey(spuId);
        // 声明要返回的对象类型
        SeckillSpuVO seckillSpuVO=null;
        // 判断Redis中是否包含这个Key
        if(redisTemplate.hasKey(seckillSpuKey)){
            // 直接从Redis中获取即可
            seckillSpuVO=(SeckillSpuVO)redisTemplate
                    .boundValueOps(seckillSpuKey).get();
        }else{
            // 如果Redis中没有这个Key
            // 要从数据库中查询秒杀spu信息和常规spu信息赋值到seckillSpuVO对象中
            // 先查询秒杀信息:
            SeckillSpu seckillSpu= seckillSpuMapper.findSeckillSpuById(spuId);
            // 判断seckillSpu是否为空(因为布隆过滤器可能产生误判)
            if(seckillSpu==null){
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                        "您访问的商品不存在");
            }
            // 到此为止,我们已经查询出了spu商品的秒杀信息,下面要查询常规信息
            // dubbo调用product模块的方法获得spu常规信息
            SpuStandardVO spuStandardVO= dubboSeckillSpuService
                                                    .getSpuById(spuId);
            seckillSpuVO=new SeckillSpuVO();
            // 将常规spu信息对象的同名属性赋值给seckillSpuVO
            BeanUtils.copyProperties(spuStandardVO,seckillSpuVO);
            // 最后将秒杀信息赋值到seckillSpuVO
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            // seckillSpuVO 就完成了常规spu信息和秒杀spu信息的赋值过程
            // 将这个对象保存到Redis中
            redisTemplate.boundValueOps(seckillSpuKey).set(
                    seckillSpuVO,10*60*1000+RandomUtils.nextInt(10000),
                    TimeUnit.MILLISECONDS);
        }
        // 返回前最后的步骤是给seckillSpuVO的url属性赋值
        // 一旦给url属性赋值,就意味着当前用户可以提交购买订单了
        // 必须判断当前时间是否在秒杀时间段内,才能决定是否给url赋值
        LocalDateTime nowTime=LocalDateTime.now();
        // 当前是高并发状态,不要连接数据库去判断时间
        // 我们使用seckillSpuVO中的开始时间和结束时间属性去判断
        // 判断的基本原则是开始时间小于当前时间小于结束时间
        // 本次我们使用"时间差"对象Duration来判断时间关系
        // 我们会利用Duration提供的between方法来获得两个时间的时间差
        // 这个方法有个特征,如果时间差是负数,会返回一个negative的状态
        // 判断当前时间大于开始时间
        //                             2022-10-12 16:45   , 2022-10-12 16:40
        Duration afterTime=Duration.between(nowTime,seckillSpuVO.getStartTime());
        // 判断结束时间大于当前时间
        Duration beforeTime=Duration.between(seckillSpuVO.getEndTime(),nowTime);
        // 简单来说, between方法中两个时间参数,前面的大后面的小就会返回negative
        // 上面两个变量如果返回值都是negative,
        // 就证明当前时间大于开始时间,结束时间大于当前时间
        if(afterTime.isNegative()  &&  beforeTime.isNegative()){
            // 进入if表示时间是正确的,要授权url,允许秒杀购买
            // 根据spuId获得redis事先预热好的随机码
            String randCodeKey=SeckillCacheUtils.getRandCodeKey(spuId);
            // 从redis中获取随机码
            String randCode=redisTemplate.boundValueOps(randCodeKey).get()+"";
            // 将随机码赋值到url
            seckillSpuVO.setUrl("/seckill/"+randCode);
            log.info("url赋值随机码为:{}",randCode);
        }
        // 最后别忘了返回
        // 最后返回的seckillSpuVO实际上是 秒杀spu信息+常规spu信息+url
        return seckillSpuVO;
    }


    // 秒杀所有信息都有保存到Redis中
    @Autowired
    private RedisTemplate redisTemplate;
    // 没有定义SpuDetail对应的常量,所以这个要自己定义
    // PREFIX是"前缀"的意思
    public static final String
            SECKILL_SPU_DETAIL_VO_PREFIX="seckill:spu:detail:vo:";

    @Override
    public SeckillSpuDetailSimpleVO getSeckillSpuDetail(Long spuId) {
        // 获得常量Key
        String seckillSpuDetailKey=SECKILL_SPU_DETAIL_VO_PREFIX+spuId;
        // 先声明一个当前方法的返回值类型的对象,设值为null,以备后续使用
        SeckillSpuDetailSimpleVO simpleVO=null;
        // 判断redis中是否已经包含这个key
        if(redisTemplate.hasKey(seckillSpuDetailKey)){
            // 如果已经保存在Redis中,直接赋值
            simpleVO=(SeckillSpuDetailSimpleVO)redisTemplate
                    .boundValueOps(seckillSpuDetailKey).get();
        }else{
            // 如果Redis中没有这个对象,就需要按spuId到数据库中查询
            // 使用Dubbo调用product模块的方法查询
            SpuDetailStandardVO spuDetailStandardVO=
                dubboSeckillSpuService.getSpuDetailById(spuId);
            // 实例化SeckillSpuDetailSimpleVO对象
            simpleVO=new SeckillSpuDetailSimpleVO();
            BeanUtils.copyProperties(spuDetailStandardVO,simpleVO);
            // simpleVO赋值完成后,将它保存到Redis中,以便后续获取
            redisTemplate.boundValueOps(seckillSpuDetailKey)
                    .set(simpleVO,10*60*1000+ RandomUtils.nextInt(10000),
                            TimeUnit.MILLISECONDS);
        }
        // 最后别忘了写返回值!!!!
        return simpleVO;
    }
}

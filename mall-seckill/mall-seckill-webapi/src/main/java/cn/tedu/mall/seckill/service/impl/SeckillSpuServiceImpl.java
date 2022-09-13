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

    // 查询秒杀商品spu列表
    @Autowired
    private SeckillSpuMapper seckillSpuMapper;

    // 要想查询pms_spu,必须查询mall_pms数据库,查询这个数据库都是product模块的功能
    @DubboReference
    private IForSeckillSpuService dubboSeckillSpuService;

    // 分页查询秒杀商品信息的方法
    // 这个方法的返回值是包含pms_spu表中spu商品的一般信息的
    // 所以业务中一定会查询pms_spu表中的信息,最终才能返回满足要求的商品信息
    @Override
    public JsonPage<SeckillSpuVO> listSeckillSpus(Integer page, Integer pageSize) {
        // 分页查询秒杀商品列表
        PageHelper.startPage(page,pageSize);
        // 执行查询
        List<SeckillSpu> seckillSpus=seckillSpuMapper.findSeckillSpus();
        // 返回值SeckillSpuVO是既包含秒杀spu信息又包含普通spu信息
        // 最终返回它必须在当前方法中去查询spu普通信息,所以先实例化集合以保存它
        List<SeckillSpuVO> seckillSpuVOs=new ArrayList<>();
        // 遍历从数据库中查询出的秒杀商品列表的集合
        for(SeckillSpu seckillSpu : seckillSpus){
            // 获得秒杀商品的spuId
            Long spuId=seckillSpu.getSpuId();
            // 利用dubbo查询当前spuId对应的普通商品信息
            SpuStandardVO spuStandardVO=
                dubboSeckillSpuService.getSpuById(spuId);
            // seckillSpu是秒杀信息,spuStandardVO是普通信息
            // 后面要将上面两组信息都赋值到SeckillSpuVO类型对象中
            SeckillSpuVO seckillSpuVO=new SeckillSpuVO();
            BeanUtils.copyProperties(spuStandardVO,seckillSpuVO);
            // 下面将秒杀信息中特有的信息赋值到seckillSpuVO中
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            //seckillSpuVO对象既包含了spu秒杀信息,又包含了spu普通信息
            seckillSpuVOs.add(seckillSpuVO);
        }
        // 最后别忘了返回
        return JsonPage.restPage(new PageInfo<>(seckillSpuVOs));
    }


    @Autowired
    private RedisBloomUtils redisBloomUtils;
    // 根据SpuId查询Spu详情(包含秒杀信息和spu普通信息)
    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {
        // 先使用布隆过滤器对参数有spuId进行判断
        // 如果判断结果是spuId不存在于数据库中,直接抛出异常
        // 获得本次布隆过滤器的Key
        String bloomTodayKey=SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        log.info("当前批次布隆过滤器的key为:{}",bloomTodayKey);
        if(!redisBloomUtils.bfexists(bloomTodayKey,spuId+"")){
            //  进入这个if表示当前spuId不在布隆过滤器保存的数据中
            //  为了防止缓存穿透,抛出异常,终止程序
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                                        "您访问的商品不存在(布隆过滤器判断)");
        }
        // 声明返回值类型的对象
        SeckillSpuVO seckillSpuVO=null;
        // 获得spuVo对应的常量key
        // mall:seckill:spu:vo:2
        String seckillSpuKey= SeckillCacheUtils.getSeckillSpuVOKey(spuId);
        // 判断Redis中是否包含这个key
        if(redisTemplate.hasKey(seckillSpuKey)){
            // 如果Redis已经存在,直接从Redis中获取
            seckillSpuVO=(SeckillSpuVO)redisTemplate
                        .boundValueOps(seckillSpuKey).get();
        }else{
            // 如果Redis中不存在这个key
            // 返回值seckillSpuVO包含spu秒杀信息和spu普通信息
            // 先查询秒杀信息:
            SeckillSpu seckillSpu= seckillSpuMapper.findSeckillSpuById(spuId);
            // 判断seckillSpu是否为空(原因是布隆过滤器可能误判)
            if(seckillSpu==null){
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                        "您访问的商品不存在");
            }
            // 到此为止,秒杀信息已经成功获取,下面获取spu普通商品信息
            // 所以需要Dubbo调用product模块的提供的查询mall_pms数据库的信息
            SpuStandardVO spuStandardVO=dubboSeckillSpuService.getSpuById(spuId);
            // 先实例化SeckillSpuVo对象,再进行赋值
            seckillSpuVO=new SeckillSpuVO();
            // spuStandardVO对象的同名属性赋值到seckillSpuVO对象中,赋值普通spu信息
            BeanUtils.copyProperties(spuStandardVO,seckillSpuVO);
            // 下面将秒杀spu信息赋值
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            // 将seckillSpuVO对象保存到Redis中
            redisTemplate.boundValueOps(seckillSpuKey).set(seckillSpuVO,
                120*60*1000+RandomUtils.nextInt(10000),TimeUnit.MILLISECONDS);
        }
        // 返回前最后的步骤是给seckillSpuVO对象的url属性赋值
        // 一旦赋值url属性,就意味着当前用户具备了提高订单的路径信息
        // 所以必须经过秒杀时间判断,才能给url赋值
        // 判断当前时间是否在秒杀时间段内
        LocalDateTime nowTime=LocalDateTime.now();
        // 当前是高并发状态,不能再轻易连接数据库,所以使用不连库的方式判断
        // 判断的基本原则是当前时间大于开始时间并且小于结束时间
        // 我们可以利用"时间差"对象Duration来判断时间
        // Duration对象有一个计算时间差的方法between
        // 这个方法中传入两个参数,来计算时间差
        // between方法是第二个参数减第一个参数计算时间差
        // 特征是如果时间差为负值返回negative
        // 判断当前时间大于开始时间
        Duration afterTime=Duration.between(nowTime,seckillSpuVO.getStartTime());
        // 判断结束时间大于当前时间
        Duration beforeTime=Duration.between(seckillSpuVO.getEndTime(),nowTime);
        // 如果上面afterTime和beforeTime同时是negative
        // 表示nowTime在这两个时间之间
        // 在满足条件的前提下,我们向seckillSpuVO的url属性赋值
        if(afterTime.isNegative() && beforeTime.isNegative()) {
            // 所有随机码都会在预热时保存redis中,我们先只需要确定key就可以获取
            // mall:seckill:spu:url:rand:code:2
            String randCodeKey=SeckillCacheUtils.getRandCodeKey(spuId);
            String randCode=redisTemplate.boundValueOps(randCodeKey).get()+"";
            // 向url属性赋值
            seckillSpuVO.setUrl("/seckill/"+randCode);
            System.out.println("--------url赋值随机码为:"+randCode+"---------");
        }
        // 最后别忘了返回
        // 正常情况下返回的seckillSpuVO 是包含url的,这个url会响应给前端来保存
        // 在前端进行提交订单时,需要利用这个url才能正常发起购买
        return seckillSpuVO;
    }


    // 秒杀所有信息都要保存到Redis中
    @Autowired
    private RedisTemplate redisTemplate;
    // 没有定义SpuDetail使用的常量,我们自己定义一下
    // PREFIX是"前缀"的意思
    public static final String
            SECKILL_SPU_DETAIL_VO_PREFIX="seckill:spu:detail:vo:";

    @Override
    public SeckillSpuDetailSimpleVO getSeckillSpuDetail(Long spuId) {
        // 获得常量的字符串
        String seckillDetailKey=SECKILL_SPU_DETAIL_VO_PREFIX+spuId;
        // 先声明一个当前方法返回值类型的对象
        SeckillSpuDetailSimpleVO simpleVO=null;
        // 判断Redis中是否已经包含这个key
        if(redisTemplate.hasKey(seckillDetailKey)){
            // 如果已经包含这个key直接取出
            simpleVO=(SeckillSpuDetailSimpleVO)redisTemplate
                    .boundValueOps(seckillDetailKey).get();
        }else {
            // 如果Redis中没有这个key,就需要查询这个spuId的detail数据
            // 借助product模块提供的服务,利用dubbo实现查询
            SpuDetailStandardVO spuDetailStandardVO=
                dubboSeckillSpuService.getSpuDetailById(spuId);
            // 将SpuDetailStandardVO转换为SeckillSpuDetailSimpleVO
            simpleVO=new SeckillSpuDetailSimpleVO();
            BeanUtils.copyProperties(spuDetailStandardVO,simpleVO);
            // 将转换后,赋好值的simpleVO保存到Redis中
            redisTemplate.boundValueOps(seckillDetailKey).
                    set(simpleVO,120*60*1000+ RandomUtils.nextInt(10000),
                            TimeUnit.MILLISECONDS);
        }
        // 最后的返回值一定要写!!!!
        return simpleVO;
    }
}

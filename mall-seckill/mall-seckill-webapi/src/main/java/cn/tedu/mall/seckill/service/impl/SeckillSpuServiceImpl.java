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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {
        return null;
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

        }

        return null;
    }
}

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {
        return null;
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

        }

        return simpleVO;
    }
}

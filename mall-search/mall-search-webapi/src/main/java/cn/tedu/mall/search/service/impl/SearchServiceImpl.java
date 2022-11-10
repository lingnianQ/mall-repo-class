package cn.tedu.mall.search.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.product.model.Spu;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import cn.tedu.mall.search.repository.SpuForElasticRepository;
import cn.tedu.mall.search.service.ISearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/11 3:42
 */
@Service
@Slf4j
public class SearchServiceImpl implements ISearchService {

    @DubboReference
    private IForFrontSpuService dubboSpuService;
    @Autowired
    private SpuForElasticRepository spuRepository;

    /**
     * 向ES中加载数据
     */
    @Override
    public void loadSpuByPage() {
        int i = 1;
        //总页数
        int pages;
        do {
            JsonPage<Spu> spus = dubboSpuService.getSpuByPage(i, 2);
            List<SpuForElastic> esSpus = new ArrayList<>();
            for (Spu spu : spus.getList()) {
                SpuForElastic esSpu = new SpuForElastic();
                BeanUtils.copyProperties(spu, esSpu);
                esSpus.add(esSpu);
            }
            spuRepository.saveAll(esSpus);
            log.info("成功加载了第{}页数据", i);
            i++;
            pages = spus.getTotalPage();
        } while (i <= pages);
    }

    /**
     * ES,分页
     *
     * @param keyword
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public JsonPage<SpuForElastic> search(String keyword, Integer page, Integer pageSize) {
        return null;
    }
}

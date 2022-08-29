package cn.tedu.mall.search.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.search.entity.SpuEntity;
import cn.tedu.mall.search.repository.SpuEntityRepository;
import cn.tedu.mall.search.service.ISearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

// 查询远程服务器(虚拟机)ELK系统的业务逻辑层
@Service
@Slf4j
public class SearchRemoteServiceImpl implements ISearchService {
    // 注入查询ES的Repository
    @Autowired
    private SpuEntityRepository spuEntityRepository;

    @Override
    public JsonPage<SpuEntity> search(String keyword, Integer page, Integer pageSize) {
        // 执行按关键字进行分页查询
        Page<SpuEntity> spuEntities=spuEntityRepository.querySearchByText(
                keyword, PageRequest.of(page-1,pageSize));
        JsonPage<SpuEntity> jsonPage=new JsonPage<>();
        //把spuEntities中的必要分页信息和数据赋值给jsonPage用于返回
        jsonPage.setPage(page);
        jsonPage.setPageSize(pageSize);
        jsonPage.setTotalPage(spuEntities.getTotalPages());
        jsonPage.setTotal(spuEntities.getTotalElements());
        jsonPage.setList(spuEntities.getContent());
        // 别忘了返回
        return jsonPage;

    }

    @Override
    public void loadSpuByPage() {

    }
}

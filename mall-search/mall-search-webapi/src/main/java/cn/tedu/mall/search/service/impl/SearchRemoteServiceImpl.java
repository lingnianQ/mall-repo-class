package cn.tedu.mall.search.service.impl;


import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.search.entity.SpuEntity;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.search.repository.SpuEntityRepository;
import cn.tedu.mall.search.service.ISearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


// 实现查询远程服务器(虚拟机Linux系统)ELK系统的业务逻辑层
@Service
@Slf4j
public class SearchRemoteServiceImpl implements ISearchService {

    @Autowired
    private SpuEntityRepository spuEntityRepository;

    @Override
    public JsonPage<SpuEntity> search(String keyword, Integer page, Integer pageSize) {
        // 执行调用按关键字进行查询的方法
        Page<SpuEntity> spuEntities=spuEntityRepository
                .querySearchByText(keyword, PageRequest.of(page-1,pageSize));
        // 将Page<SpuEntity>转换为JsonPage<SpuEntity>
        JsonPage<SpuEntity> jsonPage=new JsonPage<>();
        // 赋值相关分页信息
        jsonPage.setPage(page);
        jsonPage.setPageSize(pageSize);
        //总条数和总页数
        jsonPage.setTotal(spuEntities.getTotalElements());
        jsonPage.setTotalPage(spuEntities.getTotalPages());
        // 将查询到的数据赋值到jsonPage
        jsonPage.setList(spuEntities.getContent());
        // 最后别忘了返回
        return jsonPage;

    }

    // 加载和同步数据完全由logstash完成,无需编写下面加载数据的方法
    @Override
    public void loadSpuByPage() {

    }
}

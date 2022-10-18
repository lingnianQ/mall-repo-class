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

// 实现查询远程服务器(虚拟机Linux)ELK系统的信息的业务逻辑层
@Service
@Slf4j
public class SearchRemoteServiceImpl implements ISearchService {

    @Autowired
    private SpuEntityRepository spuEntityRepository;

    @Override
    public JsonPage<SpuEntity> search(String keyword, Integer page, Integer pageSize) {

        Page<SpuEntity> spus=spuEntityRepository.querySearchByText(keyword,
                PageRequest.of(page-1,pageSize));
        // 当前业务逻辑层方法要求返回JsonPage类型,但是我们查询结果是Page类型,需要转换
        // 转换的实现可以通过在JsonPage类中编写方法实现,也可以在当前位置手动转换实现
        JsonPage<SpuEntity> jsonPage=new JsonPage<>();
        // 赋值分页信息
        jsonPage.setPage(page);
        jsonPage.setPageSize(pageSize);
        // 赋值总页数和总条数
        jsonPage.setTotal(spus.getTotalElements());
        jsonPage.setTotalPage(spus.getTotalPages());
        // 赋值分页数据
        jsonPage.setList(spus.getContent());
        // 最后别忘了返回!!!!
        return jsonPage;
    }

    @Override
    public void loadSpuByPage() {

    }
}

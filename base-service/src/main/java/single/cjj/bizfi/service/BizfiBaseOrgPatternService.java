package single.cjj.bizfi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.bizfi.entity.BizfiBaseOrgPattern;

import java.util.Map;

/**
 * 组织形态服务接口
 */
public interface BizfiBaseOrgPatternService extends IService<BizfiBaseOrgPattern> {
    BizfiBaseOrgPattern add(BizfiBaseOrgPattern pattern);

    BizfiBaseOrgPattern update(BizfiBaseOrgPattern pattern);

    boolean delete(Long fid);

    BizfiBaseOrgPattern get(Long fid);

    IPage<BizfiBaseOrgPattern> list(int page, int size, Map<String, Object> query);
}

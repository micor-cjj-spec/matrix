package single.cjj.bizfi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.bizfi.entity.BizfiBaseOrgFunctionType;

import java.util.Map;

/**
 * 组织职能类型服务接口
 */
public interface BizfiBaseOrgFunctionTypeService extends IService<BizfiBaseOrgFunctionType> {
    BizfiBaseOrgFunctionType add(BizfiBaseOrgFunctionType type);

    BizfiBaseOrgFunctionType update(BizfiBaseOrgFunctionType type);

    boolean delete(Long fid);

    BizfiBaseOrgFunctionType get(Long fid);

    IPage<BizfiBaseOrgFunctionType> list(int page, int size, Map<String, Object> query);
}

package single.cjj.bizfi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.bizfi.entity.BizfiDeptDim;

import java.util.Map;

/**
 * 部门维度服务接口
 */
public interface BizfiDeptDimService extends IService<BizfiDeptDim> {
    BizfiDeptDim add(BizfiDeptDim dept);

    BizfiDeptDim update(BizfiDeptDim dept);

    boolean delete(Long fid);

    BizfiDeptDim get(Long fid);

    IPage<BizfiDeptDim> list(int page, int size, Map<String, Object> query);
}

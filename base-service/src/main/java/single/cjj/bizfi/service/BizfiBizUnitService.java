package single.cjj.bizfi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.bizfi.entity.BizfiBizUnit;

import java.util.Map;

/**
 * 业务单元服务接口
 */
public interface BizfiBizUnitService extends IService<BizfiBizUnit> {
    BizfiBizUnit add(BizfiBizUnit unit);

    BizfiBizUnit update(BizfiBizUnit unit);

    boolean delete(Long fid);

    BizfiBizUnit get(Long fid);

    IPage<BizfiBizUnit> list(int page, int size, Map<String, Object> query);
}

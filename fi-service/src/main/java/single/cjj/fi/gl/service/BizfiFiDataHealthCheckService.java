package single.cjj.fi.gl.service;

import single.cjj.fi.gl.vo.BizfiFiHealthCheckResultVO;

public interface BizfiFiDataHealthCheckService {
    BizfiFiHealthCheckResultVO check(Long forg, Long templateId, Integer sampleSize);
}

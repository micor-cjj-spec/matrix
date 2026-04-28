package single.cjj.fi.gl.service;

import single.cjj.fi.gl.vo.MonthEndArchivePackageVO;

public interface BizfiFiMonthEndArchiveService {
    MonthEndArchivePackageVO getPackage(Long forg, String period);
}

package single.cjj.fi.gl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.service.BizfiFiMonthEndArchiveService;
import single.cjj.fi.gl.vo.MonthEndArchivePackageVO;

@RestController
@RequestMapping("/month-end-archive")
public class BizfiFiMonthEndArchiveController {

    @Autowired
    private BizfiFiMonthEndArchiveService service;

    @GetMapping("/package")
    public ApiResponse<MonthEndArchivePackageVO> getPackage(
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "period", required = false) String period
    ) {
        return ApiResponse.success(service.getPackage(forg, period));
    }
}

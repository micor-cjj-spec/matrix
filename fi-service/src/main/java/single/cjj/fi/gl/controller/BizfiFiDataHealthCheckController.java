package single.cjj.fi.gl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.service.BizfiFiDataHealthCheckService;
import single.cjj.fi.gl.vo.BizfiFiHealthCheckResultVO;

/**
 * 财务数据健康检查接口
 */
@RestController
@RequestMapping("/data-health-check")
public class BizfiFiDataHealthCheckController {

    @Autowired
    private BizfiFiDataHealthCheckService service;

    @GetMapping("/finance-foundation")
    public ApiResponse<BizfiFiHealthCheckResultVO> financeFoundation(
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "sampleSize", required = false) Integer sampleSize
    ) {
        return ApiResponse.success(service.check(forg, templateId, sampleSize));
    }
}

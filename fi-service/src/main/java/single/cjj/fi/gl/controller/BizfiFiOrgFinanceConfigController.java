package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.entity.BizfiFiOrgFinanceConfig;
import single.cjj.fi.gl.service.BizfiFiOrgFinanceConfigService;

import java.util.HashMap;
import java.util.Map;

/**
 * 组织财务配置接口
 */
@RestController
@RequestMapping("/org-finance-config")
public class BizfiFiOrgFinanceConfigController {

    @Autowired
    private BizfiFiOrgFinanceConfigService service;

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiOrgFinanceConfig> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    @GetMapping("/org/{forg}")
    public ApiResponse<BizfiFiOrgFinanceConfig> getByOrg(@PathVariable("forg") Long forg) {
        return ApiResponse.success(service.getByOrg(forg));
    }

    @PostMapping
    public ApiResponse<BizfiFiOrgFinanceConfig> add(@RequestBody BizfiFiOrgFinanceConfig config) {
        return ApiResponse.success(service.add(config));
    }

    @PutMapping
    public ApiResponse<BizfiFiOrgFinanceConfig> update(@RequestBody BizfiFiOrgFinanceConfig config) {
        return ApiResponse.success(service.update(config));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiOrgFinanceConfig>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "fstatus", required = false) String fstatus,
            @RequestParam(value = "fbaseCurrency", required = false) String fbaseCurrency
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("forg", forg);
        query.put("fstatus", fstatus);
        query.put("fbaseCurrency", fbaseCurrency);
        return ApiResponse.success(service.list(page, size, query));
    }
}

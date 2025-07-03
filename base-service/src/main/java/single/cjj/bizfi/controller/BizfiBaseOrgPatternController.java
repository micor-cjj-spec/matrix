package single.cjj.bizfi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.entity.BizfiBaseOrgPattern;
import single.cjj.bizfi.service.BizfiBaseOrgPatternService;

import java.util.HashMap;
import java.util.Map;

/**
 * 组织形态接口
 */
@RestController
@RequestMapping("/api/org-pattern")
public class BizfiBaseOrgPatternController {
    @Autowired
    private BizfiBaseOrgPatternService service;

    /** 根据ID获取详情 */
    @GetMapping("/{fid}")
    public ApiResponse<BizfiBaseOrgPattern> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    /** 新增 */
    @PostMapping
    public ApiResponse<BizfiBaseOrgPattern> add(@RequestBody BizfiBaseOrgPattern pattern) {
        return ApiResponse.success(service.add(pattern));
    }

    /** 修改 */
    @PutMapping
    public ApiResponse<BizfiBaseOrgPattern> update(@RequestBody BizfiBaseOrgPattern pattern) {
        return ApiResponse.success(service.update(pattern));
    }

    /** 删除 */
    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    /** 分页/条件查询列表 */
    @GetMapping("/list")
    public ApiResponse<IPage<BizfiBaseOrgPattern>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "fname", required = false) String fname,
            @RequestParam(value = "fbillno", required = false) String fbillno,
            @RequestParam(value = "fpattern", required = false) String fpattern,
            @RequestParam(value = "fenable", required = false) Integer fenable
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("fname", fname);
        query.put("fbillno", fbillno);
        query.put("fpattern", fpattern);
        query.put("fenable", fenable);
        return ApiResponse.success(service.list(page, size, query));
    }
}

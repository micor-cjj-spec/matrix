package single.cjj.bizfi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.entity.BizfiBizUnit;
import single.cjj.bizfi.service.BizfiBizUnitService;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务单元接口
 */
@RestController
@RequestMapping("/biz-unit")
public class BizfiBizUnitController {
    @Autowired
    private BizfiBizUnitService service;

    /** 根据ID获取详情 */
    @GetMapping("/{fid}")
    public ApiResponse<BizfiBizUnit> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    /** 新增业务单元 */
    @PostMapping
    public ApiResponse<BizfiBizUnit> add(@RequestBody BizfiBizUnit unit) {
        return ApiResponse.success(service.add(unit));
    }

    /** 修改业务单元 */
    @PutMapping
    public ApiResponse<BizfiBizUnit> update(@RequestBody BizfiBizUnit unit) {
        return ApiResponse.success(service.update(unit));
    }

    /** 删除业务单元 */
    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    /** 分页/条件查询列表 */
    @GetMapping("/list")
    public ApiResponse<IPage<BizfiBizUnit>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "fname", required = false) String fname,
            @RequestParam(value = "fcode", required = false) String fcode,
            @RequestParam(value = "fstatus", required = false) String fstatus
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("fname", fname);
        query.put("fcode", fcode);
        query.put("fstatus", fstatus);
        return ApiResponse.success(service.list(page, size, query));
    }
}

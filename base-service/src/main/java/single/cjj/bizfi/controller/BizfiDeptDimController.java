package single.cjj.bizfi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.entity.BizfiDeptDim;
import single.cjj.bizfi.service.BizfiDeptDimService;

import java.util.HashMap;
import java.util.Map;

/**
 * 部门维度接口
 */
@RestController
@RequestMapping("/dept-dim")
public class BizfiDeptDimController {
    @Autowired
    private BizfiDeptDimService service;

    /** 根据ID获取详情 */
    @GetMapping("/{fid}")
    public ApiResponse<BizfiDeptDim> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    /** 新增部门 */
    @PostMapping
    public ApiResponse<BizfiDeptDim> add(@RequestBody BizfiDeptDim dept) {
        return ApiResponse.success(service.add(dept));
    }

    /** 修改部门 */
    @PutMapping
    public ApiResponse<BizfiDeptDim> update(@RequestBody BizfiDeptDim dept) {
        return ApiResponse.success(service.update(dept));
    }

    /** 删除部门 */
    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    /** 分页/条件查询列表 */
    @GetMapping("/list")
    public ApiResponse<IPage<BizfiDeptDim>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "fname", required = false) String fname,
            @RequestParam(value = "fcode", required = false) String fcode,
            @RequestParam(value = "fbizunitid", required = false) Long fbizunitid,
            @RequestParam(value = "fstatus", required = false) String fstatus
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("fname", fname);
        query.put("fcode", fcode);
        query.put("fbizunitid", fbizunitid);
        query.put("fstatus", fstatus);
        return ApiResponse.success(service.list(page, size, query));
    }
}

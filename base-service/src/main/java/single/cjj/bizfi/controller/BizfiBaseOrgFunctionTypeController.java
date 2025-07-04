package single.cjj.bizfi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.entity.BizfiBaseOrgFunctionType;
import single.cjj.bizfi.service.BizfiBaseOrgFunctionTypeService;

import java.util.HashMap;
import java.util.Map;

/**
 * 组织职能类型接口
 */
@RestController
@RequestMapping("/org-function-type")
public class BizfiBaseOrgFunctionTypeController {
    @Autowired
    private BizfiBaseOrgFunctionTypeService service;

    /** 根据ID获取详情 */
    @GetMapping("/{fid}")
    public ApiResponse<BizfiBaseOrgFunctionType> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    /** 新增类型 */
    @PostMapping
    public ApiResponse<BizfiBaseOrgFunctionType> add(@RequestBody BizfiBaseOrgFunctionType type) {
        return ApiResponse.success(service.add(type));
    }

    /** 修改类型 */
    @PutMapping
    public ApiResponse<BizfiBaseOrgFunctionType> update(@RequestBody BizfiBaseOrgFunctionType type) {
        return ApiResponse.success(service.update(type));
    }

    /** 删除类型 */
    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    /** 分页/条件查询列表 */
    @GetMapping("/list")
    public ApiResponse<IPage<BizfiBaseOrgFunctionType>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "fname", required = false) String fname,
            @RequestParam(value = "fnumber", required = false) String fnumber,
            @RequestParam(value = "ftype", required = false) String ftype,
            @RequestParam(value = "fbasefunction", required = false) Integer fbasefunction,
            @RequestParam(value = "fcustom", required = false) Integer fcustom
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("fname", fname);
        query.put("fnumber", fnumber);
        query.put("ftype", ftype);
        query.put("fbasefunction", fbasefunction);
        query.put("fcustom", fcustom);
        return ApiResponse.success(service.list(page, size, query));
    }
}

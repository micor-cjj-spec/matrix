package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.entity.BizfiFiAccount;
import single.cjj.fi.gl.service.BizfiFiAccountService;

import java.util.HashMap;
import java.util.Map;

/**
 * 会计科目接口
 */
@RestController
@RequestMapping("/account-subject")
public class BizfiFiAccountController {

    @Autowired
    private BizfiFiAccountService service;

    /** 获取详情 */
    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiAccount> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    /** 新增科目 */
    @PostMapping
    public ApiResponse<BizfiFiAccount> add(@RequestBody BizfiFiAccount account) {
        return ApiResponse.success(service.add(account));
    }

    /** 修改科目 */
    @PutMapping
    public ApiResponse<BizfiFiAccount> update(@RequestBody BizfiFiAccount account) {
        return ApiResponse.success(service.update(account));
    }

    /** 删除科目 */
    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    /** 分页/条件查询列表 */
    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiAccount>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "fcode", required = false) String fcode,
            @RequestParam(value = "fname", required = false) String fname
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("fcode", fcode);
        query.put("fname", fname);
        return ApiResponse.success(service.list(page, size, query));
    }
}

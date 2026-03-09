package single.cjj.bizfi.controller;

import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class BaseDataWorkflowController {

    private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
            "material", "customer", "supplier", "currency", "exchange-rate", "bank-info", "country", "region", "unit"
    ));

    private static final Map<String, Map<Long, BaseDataItem>> STORE = new ConcurrentHashMap<>();
    private static final AtomicLong ID_GEN = new AtomicLong(1000);

    @GetMapping({"/material/list", "/customer/list", "/supplier/list", "/currency/list", "/exchange-rate/list", "/bank-info/list", "/country/list", "/region/list", "/unit/list"})
    public ApiResponse<Map<String, Object>> list(HttpServletRequest request) {
        String type = resolveType(request.getRequestURI());
        List<BaseDataItem> records = new ArrayList<>(store(type).values());
        records.sort(Comparator.comparing(BaseDataItem::getFid));
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", records.size());
        return ApiResponse.success(data);
    }

    @PostMapping({"/material", "/customer", "/supplier", "/currency", "/exchange-rate", "/bank-info", "/country", "/region", "/unit"})
    public ApiResponse<BaseDataItem> create(HttpServletRequest request, @RequestBody BaseDataItem item) {
        String type = resolveType(request.getRequestURI());
        item.setFid(ID_GEN.incrementAndGet());
        item.setFstatus("DRAFT");
        if (item.getFname() == null) item.setFname("");
        if (item.getFcode() == null) item.setFcode("");
        store(type).put(item.getFid(), item);
        return ApiResponse.success("创建成功", item);
    }

    @PutMapping({"/material", "/customer", "/supplier", "/currency", "/exchange-rate", "/bank-info", "/country", "/region", "/unit"})
    public ApiResponse<BaseDataItem> update(HttpServletRequest request, @RequestBody BaseDataItem item) {
        String type = resolveType(request.getRequestURI());
        BaseDataItem db = store(type).get(item.getFid());
        if (db == null) return ApiResponse.error("数据不存在");
        if ("AUDITED".equals(db.getFstatus())) return ApiResponse.error("已审核数据不允许修改");
        db.setFname(item.getFname());
        db.setFcode(item.getFcode());
        return ApiResponse.success("更新成功", db);
    }

    @DeleteMapping({"/material/{fid}", "/customer/{fid}", "/supplier/{fid}", "/currency/{fid}", "/exchange-rate/{fid}", "/bank-info/{fid}", "/country/{fid}", "/region/{fid}", "/unit/{fid}"})
    public ApiResponse<Boolean> delete(HttpServletRequest request, @PathVariable("fid") Long fid) {
        String type = resolveType(request.getRequestURI());
        BaseDataItem db = store(type).get(fid);
        if (db == null) return ApiResponse.error("数据不存在");
        if ("AUDITED".equals(db.getFstatus())) return ApiResponse.error("已审核数据不允许删除");
        store(type).remove(fid);
        return ApiResponse.success(true);
    }

    @PostMapping({"/material/{fid}/submit", "/customer/{fid}/submit", "/supplier/{fid}/submit", "/currency/{fid}/submit", "/exchange-rate/{fid}/submit", "/bank-info/{fid}/submit", "/country/{fid}/submit", "/region/{fid}/submit", "/unit/{fid}/submit"})
    public ApiResponse<BaseDataItem> submit(HttpServletRequest request, @PathVariable("fid") Long fid) {
        String type = resolveType(request.getRequestURI());
        BaseDataItem db = store(type).get(fid);
        if (db == null) return ApiResponse.error("数据不存在");
        if (!"DRAFT".equals(db.getFstatus()) && !"REJECTED".equals(db.getFstatus())) {
            return ApiResponse.error("仅草稿或已驳回状态可提交");
        }
        db.setFstatus("SUBMITTED");
        db.setFsubmitBy("system");
        db.setFsubmitTime(LocalDateTime.now().toString());
        return ApiResponse.success("提交审核成功", db);
    }

    @PostMapping({"/material/{fid}/audit", "/customer/{fid}/audit", "/supplier/{fid}/audit", "/currency/{fid}/audit", "/exchange-rate/{fid}/audit", "/bank-info/{fid}/audit", "/country/{fid}/audit", "/region/{fid}/audit", "/unit/{fid}/audit"})
    public ApiResponse<BaseDataItem> audit(HttpServletRequest request, @PathVariable("fid") Long fid) {
        String type = resolveType(request.getRequestURI());
        BaseDataItem db = store(type).get(fid);
        if (db == null) return ApiResponse.error("数据不存在");
        if (!"SUBMITTED".equals(db.getFstatus())) return ApiResponse.error("仅已提交状态可审核");
        db.setFstatus("AUDITED");
        db.setFauditBy("system");
        db.setFauditTime(LocalDateTime.now().toString());
        db.setFauditRemark("审核通过");
        return ApiResponse.success("审核通过", db);
    }

    @PostMapping({"/material/{fid}/reject", "/customer/{fid}/reject", "/supplier/{fid}/reject", "/currency/{fid}/reject", "/exchange-rate/{fid}/reject", "/bank-info/{fid}/reject", "/country/{fid}/reject", "/region/{fid}/reject", "/unit/{fid}/reject"})
    public ApiResponse<BaseDataItem> reject(HttpServletRequest request, @PathVariable("fid") Long fid) {
        String type = resolveType(request.getRequestURI());
        BaseDataItem db = store(type).get(fid);
        if (db == null) return ApiResponse.error("数据不存在");
        if (!"SUBMITTED".equals(db.getFstatus())) return ApiResponse.error("仅已提交状态可驳回");
        db.setFstatus("REJECTED");
        db.setFauditBy("system");
        db.setFauditTime(LocalDateTime.now().toString());
        db.setFauditRemark("审核驳回");
        return ApiResponse.success("已驳回", db);
    }

    private Map<Long, BaseDataItem> store(String type) {
        return STORE.computeIfAbsent(type, k -> new ConcurrentHashMap<>());
    }

    private String resolveType(String uri) {
        String u = uri;
        if (u.contains("?")) u = u.substring(0, u.indexOf("?"));
        for (String t : TYPES) {
            if (u.contains("/" + t + "/") || u.endsWith("/" + t)) return t;
        }
        throw new IllegalArgumentException("unsupported type: " + uri);
    }

    public static class BaseDataItem {
        private Long fid;
        private String fname;
        private String fcode;
        private String fstatus;
        private String fsubmitBy;
        private String fsubmitTime;
        private String fauditBy;
        private String fauditTime;
        private String fauditRemark;

        public Long getFid() { return fid; }
        public void setFid(Long fid) { this.fid = fid; }
        public String getFname() { return fname; }
        public void setFname(String fname) { this.fname = fname; }
        public String getFcode() { return fcode; }
        public void setFcode(String fcode) { this.fcode = fcode; }
        public String getFstatus() { return fstatus; }
        public void setFstatus(String fstatus) { this.fstatus = fstatus; }
        public String getFsubmitBy() { return fsubmitBy; }
        public void setFsubmitBy(String fsubmitBy) { this.fsubmitBy = fsubmitBy; }
        public String getFsubmitTime() { return fsubmitTime; }
        public void setFsubmitTime(String fsubmitTime) { this.fsubmitTime = fsubmitTime; }
        public String getFauditBy() { return fauditBy; }
        public void setFauditBy(String fauditBy) { this.fauditBy = fauditBy; }
        public String getFauditTime() { return fauditTime; }
        public void setFauditTime(String fauditTime) { this.fauditTime = fauditTime; }
        public String getFauditRemark() { return fauditRemark; }
        public void setFauditRemark(String fauditRemark) { this.fauditRemark = fauditRemark; }
    }
}

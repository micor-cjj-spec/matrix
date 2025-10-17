package single.cjj.share.model;

import java.util.List;
import java.util.Map;

public class ReportExportRequest {
    private Map<String, List<String>> filters;
    private String format;

    public Map<String, List<String>> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, List<String>> filters) {
        this.filters = filters;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}

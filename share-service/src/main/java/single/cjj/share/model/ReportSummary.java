package single.cjj.share.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportSummary {
    private int totalCreated;
    private int completed;
    private int overdue;
    private double avgAgeDays;
    private Map<String, PriorityReport> byPriority = new HashMap<>();
    private List<ModuleReport> byModule;

    public int getTotalCreated() {
        return totalCreated;
    }

    public void setTotalCreated(int totalCreated) {
        this.totalCreated = totalCreated;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getOverdue() {
        return overdue;
    }

    public void setOverdue(int overdue) {
        this.overdue = overdue;
    }

    public double getAvgAgeDays() {
        return avgAgeDays;
    }

    public void setAvgAgeDays(double avgAgeDays) {
        this.avgAgeDays = avgAgeDays;
    }

    public Map<String, PriorityReport> getByPriority() {
        return byPriority;
    }

    public void setByPriority(Map<String, PriorityReport> byPriority) {
        this.byPriority = byPriority;
    }

    public List<ModuleReport> getByModule() {
        return byModule;
    }

    public void setByModule(List<ModuleReport> byModule) {
        this.byModule = byModule;
    }

    public static class PriorityReport {
        private int count;
        private double avgResponseHours;

        public PriorityReport() {
        }

        public PriorityReport(int count, double avgResponseHours) {
            this.count = count;
            this.avgResponseHours = avgResponseHours;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public double getAvgResponseHours() {
            return avgResponseHours;
        }

        public void setAvgResponseHours(double avgResponseHours) {
            this.avgResponseHours = avgResponseHours;
        }
    }

    public static class ModuleReport {
        private String module;
        private int count;

        public ModuleReport() {
        }

        public ModuleReport(String module, int count) {
            this.module = module;
            this.count = count;
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}

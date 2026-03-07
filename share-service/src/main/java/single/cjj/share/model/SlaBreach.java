package single.cjj.share.model;

import java.time.Instant;

public class SlaBreach {
    private String status;
    private Instant breachedAt;
    private Double durationHours;

    public SlaBreach() {
    }

    public SlaBreach(String status, Instant breachedAt, Double durationHours) {
        this.status = status;
        this.breachedAt = breachedAt;
        this.durationHours = durationHours;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getBreachedAt() {
        return breachedAt;
    }

    public void setBreachedAt(Instant breachedAt) {
        this.breachedAt = breachedAt;
    }

    public Double getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Double durationHours) {
        this.durationHours = durationHours;
    }
}

package single.cjj.share.model;

import java.time.Instant;

public class ExportJob {
    private String jobId;
    private String downloadUrl;
    private Instant expiresAt;
    private Instant createdAt;

    public ExportJob() {
    }

    public ExportJob(String jobId, String downloadUrl, Instant expiresAt, Instant createdAt) {
        this.jobId = jobId;
        this.downloadUrl = downloadUrl;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

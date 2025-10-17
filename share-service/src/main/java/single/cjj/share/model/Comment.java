package single.cjj.share.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Comment {
    private String id;
    private String body;
    private UserSummary author;
    private List<UserSummary> mentions = new ArrayList<>();
    private Instant createdAt;
    private List<Attachment> attachments = new ArrayList<>();

    public Comment() {
    }

    public Comment(String id, String body, UserSummary author, List<UserSummary> mentions, Instant createdAt) {
        this.id = id;
        this.body = body;
        this.author = author;
        if (mentions != null) {
            this.mentions = mentions;
        }
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public UserSummary getAuthor() {
        return author;
    }

    public void setAuthor(UserSummary author) {
        this.author = author;
    }

    public List<UserSummary> getMentions() {
        return mentions;
    }

    public void setMentions(List<UserSummary> mentions) {
        this.mentions = mentions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }
}

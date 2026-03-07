package single.cjj.share.model;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class CreateCommentRequest {
    @NotBlank(message = "评论内容不能为空")
    private String body;
    private List<String> mentions;
    private List<Attachment> attachments;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getMentions() {
        return mentions;
    }

    public void setMentions(List<String> mentions) {
        this.mentions = mentions;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }
}

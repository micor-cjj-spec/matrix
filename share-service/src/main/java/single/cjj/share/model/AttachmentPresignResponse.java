package single.cjj.share.model;

public class AttachmentPresignResponse {
    private String uploadUrl;
    private Attachment attachment;

    public AttachmentPresignResponse() {
    }

    public AttachmentPresignResponse(String uploadUrl, Attachment attachment) {
        this.uploadUrl = uploadUrl;
        this.attachment = attachment;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }
}

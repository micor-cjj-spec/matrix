package single.cjj.share.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AttachmentPresignRequest {
    @NotBlank(message = "文件名必填")
    private String fileName;

    @NotBlank(message = "contentType必填")
    private String contentType;

    @NotNull(message = "文件大小必填")
    private Long size;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}

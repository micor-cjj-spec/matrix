package single.cjj.share.model;

public class Attachment {
    private String id;
    private String name;
    private AttachmentType type;
    private String url;
    private Long size;
    private String contentType;

    public Attachment() {
    }

    public Attachment(String id, String name, AttachmentType type, String url, Long size, String contentType) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.url = url;
        this.size = size;
        this.contentType = contentType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AttachmentType getType() {
        return type;
    }

    public void setType(AttachmentType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}

package single.cjj.share.model;

public class TicketSource {
    private TicketSourceType type;
    private String detail;

    public TicketSource() {
    }

    public TicketSource(TicketSourceType type, String detail) {
        this.type = type;
        this.detail = detail;
    }

    public TicketSourceType getType() {
        return type;
    }

    public void setType(TicketSourceType type) {
        this.type = type;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}

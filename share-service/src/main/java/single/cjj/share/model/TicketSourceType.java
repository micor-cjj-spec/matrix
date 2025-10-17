package single.cjj.share.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TicketSourceType {
    CUSTOMER("客户"),
    OPERATIONS("运营"),
    MONITORING("监控"),
    INTERNAL("内部"),
    CHANNEL("渠道名");

    private final String value;

    TicketSourceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TicketSourceType fromValue(String value) {
        for (TicketSourceType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知工单来源类型: " + value);
    }
}

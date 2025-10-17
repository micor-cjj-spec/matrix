package single.cjj.share.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EventType {
    STATUS_CHANGED("STATUS_CHANGED"),
    ASSIGNEE_CHANGED("ASSIGNEE_CHANGED"),
    LABELS_UPDATED("LABELS_UPDATED"),
    SLA_BREACH("SLA_BREACH"),
    COMMENT_ADDED("COMMENT_ADDED"),
    FILE_UPLOADED("FILE_UPLOADED"),
    ROLLBACK_PLAN_UPDATED("ROLLBACK_PLAN_UPDATED"),
    DUPLICATE_MARKED("DUPLICATE_MARKED");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static EventType fromValue(String value) {
        for (EventType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知事件类型: " + value);
    }
}

package single.cjj.share.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {
    TRIAGE("待分诊"),
    BACKLOG("待办"),
    IN_PROGRESS("进行中"),
    READY_FOR_ACCEPTANCE("待验收"),
    COMPLETED("已完成"),
    ON_HOLD("搁置"),
    BLOCKED("阻塞");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TaskStatus fromValue(String value) {
        for (TaskStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知状态: " + value);
    }
}

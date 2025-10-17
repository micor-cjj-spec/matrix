package single.cjj.share.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CiStatus {
    PASSED("通过"),
    FAILED("失败"),
    RUNNING("进行中"),
    UNKNOWN("未知");

    private final String value;

    CiStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CiStatus fromValue(String value) {
        for (CiStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知CI状态: " + value);
    }
}

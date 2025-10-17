package single.cjj.share.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SecurityLevel {
    PUBLIC("公开"),
    INTERNAL("内部"),
    CONFIDENTIAL("涉密");

    private final String value;

    SecurityLevel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SecurityLevel fromValue(String value) {
        for (SecurityLevel level : values()) {
            if (level.value.equals(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知安全级别: " + value);
    }
}

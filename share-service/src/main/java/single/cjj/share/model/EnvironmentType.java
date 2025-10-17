package single.cjj.share.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnvironmentType {
    PRODUCTION("生产"),
    PRE_RELEASE("预发"),
    TEST("测试");

    private final String value;

    EnvironmentType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static EnvironmentType fromValue(String value) {
        for (EnvironmentType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知环境类型: " + value);
    }
}

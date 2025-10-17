package single.cjj.share.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskType {
    DEFECT("缺陷"),
    REQUIREMENT("需求"),
    OPERATIONS("运营"),
    SUPPORT("支持"),
    DATA("数据"),
    AUTOMATION("自动化");

    private final String value;

    TaskType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TaskType fromValue(String value) {
        for (TaskType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知任务类型: " + value);
    }
}

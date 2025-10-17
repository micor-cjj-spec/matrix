package single.cjj.share.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Priority {
    P0("P0", "#F44336", "SLA-P0"),
    P1("P1", "#FB8C00", "SLA-P1"),
    P2("P2", "#1E88E5", "SLA-P2"),
    P3("P3", "#90A4AE", "SLA-P2");

    private final String value;
    private final String color;
    private final String defaultSla;

    Priority(String value, String color, String defaultSla) {
        this.value = value;
        this.color = color;
        this.defaultSla = defaultSla;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getColor() {
        return color;
    }

    public String getDefaultSla() {
        return defaultSla;
    }

    @JsonCreator
    public static Priority fromValue(String value) {
        for (Priority priority : values()) {
            if (priority.value.equalsIgnoreCase(value)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("未知优先级: " + value);
    }
}

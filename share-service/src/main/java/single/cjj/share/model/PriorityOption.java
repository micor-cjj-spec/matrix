package single.cjj.share.model;

public class PriorityOption {
    private String value;
    private String label;
    private String color;
    private String defaultSla;

    public PriorityOption() {
    }

    public PriorityOption(String value, String label, String color, String defaultSla) {
        this.value = value;
        this.label = label;
        this.color = color;
        this.defaultSla = defaultSla;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDefaultSla() {
        return defaultSla;
    }

    public void setDefaultSla(String defaultSla) {
        this.defaultSla = defaultSla;
    }
}

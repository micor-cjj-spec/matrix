package single.cjj.share.model;

public class Estimate {
    private Double value;
    private EstimateUnit unit;

    public Estimate() {
    }

    public Estimate(Double value, EstimateUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public EstimateUnit getUnit() {
        return unit;
    }

    public void setUnit(EstimateUnit unit) {
        this.unit = unit;
    }
}

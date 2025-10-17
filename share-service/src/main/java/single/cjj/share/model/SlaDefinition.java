package single.cjj.share.model;

import java.util.ArrayList;
import java.util.List;

public class SlaDefinition {
    private Integer targetHours;
    private List<String> breachEvents = new ArrayList<>();

    public SlaDefinition() {
    }

    public SlaDefinition(Integer targetHours, List<String> breachEvents) {
        this.targetHours = targetHours;
        if (breachEvents != null) {
            this.breachEvents = breachEvents;
        }
    }

    public Integer getTargetHours() {
        return targetHours;
    }

    public void setTargetHours(Integer targetHours) {
        this.targetHours = targetHours;
    }

    public List<String> getBreachEvents() {
        return breachEvents;
    }

    public void setBreachEvents(List<String> breachEvents) {
        this.breachEvents = breachEvents;
    }
}

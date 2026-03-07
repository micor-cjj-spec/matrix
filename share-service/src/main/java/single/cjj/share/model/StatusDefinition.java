package single.cjj.share.model;

import java.util.ArrayList;
import java.util.List;

public class StatusDefinition {
    private String status;
    private List<String> transitions = new ArrayList<>();
    private SlaDefinition sla;

    public StatusDefinition() {
    }

    public StatusDefinition(String status, List<String> transitions, SlaDefinition sla) {
        this.status = status;
        if (transitions != null) {
            this.transitions = transitions;
        }
        this.sla = sla;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<String> transitions) {
        this.transitions = transitions;
    }

    public SlaDefinition getSla() {
        return sla;
    }

    public void setSla(SlaDefinition sla) {
        this.sla = sla;
    }
}

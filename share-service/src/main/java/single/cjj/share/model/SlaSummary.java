package single.cjj.share.model;

import java.util.ArrayList;
import java.util.List;

public class SlaSummary {
    private String currentPhase;
    private Double targetHours;
    private Double elapsedHours;
    private List<SlaBreach> breaches = new ArrayList<>();

    public SlaSummary() {
    }

    public SlaSummary(String currentPhase, Double targetHours, Double elapsedHours, List<SlaBreach> breaches) {
        this.currentPhase = currentPhase;
        this.targetHours = targetHours;
        this.elapsedHours = elapsedHours;
        if (breaches != null) {
            this.breaches = breaches;
        }
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public Double getTargetHours() {
        return targetHours;
    }

    public void setTargetHours(Double targetHours) {
        this.targetHours = targetHours;
    }

    public Double getElapsedHours() {
        return elapsedHours;
    }

    public void setElapsedHours(Double elapsedHours) {
        this.elapsedHours = elapsedHours;
    }

    public List<SlaBreach> getBreaches() {
        return breaches;
    }

    public void setBreaches(List<SlaBreach> breaches) {
        this.breaches = breaches;
    }
}

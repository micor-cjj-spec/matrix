package single.cjj.share.model;

import java.time.Instant;

public class Event {
    private String id;
    private EventType type;
    private Object from;
    private Object to;
    private Instant timestamp;
    private UserSummary actor;

    public Event() {
    }

    public Event(String id, EventType type, Object from, Object to, Instant timestamp, UserSummary actor) {
        this.id = id;
        this.type = type;
        this.from = from;
        this.to = to;
        this.timestamp = timestamp;
        this.actor = actor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Object getFrom() {
        return from;
    }

    public void setFrom(Object from) {
        this.from = from;
    }

    public Object getTo() {
        return to;
    }

    public void setTo(Object to) {
        this.to = to;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public UserSummary getActor() {
        return actor;
    }

    public void setActor(UserSummary actor) {
        this.actor = actor;
    }
}

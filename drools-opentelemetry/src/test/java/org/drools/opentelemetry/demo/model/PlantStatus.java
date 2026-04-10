package org.drools.opentelemetry.demo.model;

import java.time.Instant;

public class PlantStatus {

    public enum State { NORMAL, DEGRADED, CRITICAL, RECOVERING }

    private State state;
    private Instant lastStateChange;

    public PlantStatus(State state) {
        this.state = state;
        this.lastStateChange = Instant.now();
    }

    public State getState() { return state; }

    public void setState(State state) {
        this.state = state;
        this.lastStateChange = Instant.now();
    }

    public Instant getLastStateChange() { return lastStateChange; }

    public long getSecondsSinceLastChange() {
        return java.time.Duration.between(lastStateChange, Instant.now()).getSeconds();
    }

    @Override
    public String toString() {
        return "PlantStatus=" + state;
    }
}

package org.drools.opentelemetry.demo.model;

import java.time.Instant;

public class Alert {

    public enum Severity { INFO, WARNING, CRITICAL }

    private Severity severity;
    private String source;
    private String message;
    private Instant timestamp;
    private boolean acknowledged;

    public Alert(Severity severity, String source, String message) {
        this.severity = severity;
        this.source = source;
        this.message = message;
        this.timestamp = Instant.now();
        this.acknowledged = false;
    }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getSource() { return source; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    @Override
    public String toString() {
        return String.format("%s [%s] %s", severity, source, message);
    }
}

package org.drools.opentelemetry.demo.model;

import java.time.Instant;

public abstract class SensorReading {

    private final String sensorId;
    private final Instant timestamp;
    private final long sequenceNumber;

    protected SensorReading(String sensorId, long sequenceNumber) {
        this.sensorId = sensorId;
        this.timestamp = Instant.now();
        this.sequenceNumber = sequenceNumber;
    }

    public String getSensorId() { return sensorId; }
    public Instant getTimestamp() { return timestamp; }
    public long getSequenceNumber() { return sequenceNumber; }

    public abstract double getPrimaryValue();
    public abstract String getUnit();
}

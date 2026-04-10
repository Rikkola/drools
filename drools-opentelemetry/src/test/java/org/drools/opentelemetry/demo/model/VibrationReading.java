package org.drools.opentelemetry.demo.model;

public class VibrationReading extends SensorReading {

    private final double rmsVelocity;

    public VibrationReading(String sensorId, long sequenceNumber, double rmsVelocity) {
        super(sensorId, sequenceNumber);
        this.rmsVelocity = rmsVelocity;
    }

    public double getRmsVelocity() { return rmsVelocity; }

    @Override
    public double getPrimaryValue() { return rmsVelocity; }

    @Override
    public String getUnit() { return "mm/s"; }

    @Override
    public String toString() {
        return String.format("Vibration[%s]=%.2fmm/s", getSensorId(), rmsVelocity);
    }
}

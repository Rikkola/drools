package org.drools.opentelemetry.demo.model;

public class PressureReading extends SensorReading {

    private final double value;

    public PressureReading(String sensorId, long sequenceNumber, double value) {
        super(sensorId, sequenceNumber);
        this.value = value;
    }

    public double getValue() { return value; }

    @Override
    public double getPrimaryValue() { return value; }

    @Override
    public String getUnit() { return "bar"; }

    @Override
    public String toString() {
        return String.format("Pressure[%s]=%.2fbar", getSensorId(), value);
    }
}

package org.drools.opentelemetry.demo.model;

public class TemperatureReading extends SensorReading {

    private final double value;

    public TemperatureReading(String sensorId, long sequenceNumber, double value) {
        super(sensorId, sequenceNumber);
        this.value = value;
    }

    public double getValue() { return value; }

    @Override
    public double getPrimaryValue() { return value; }

    @Override
    public String getUnit() { return "°C"; }

    @Override
    public String toString() {
        return String.format("Temperature[%s]=%.1f°C", getSensorId(), value);
    }
}

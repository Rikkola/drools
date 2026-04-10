package org.drools.opentelemetry.demo.model;

public class HumidityReading extends SensorReading {

    private final double percentage;

    public HumidityReading(String sensorId, long sequenceNumber, double percentage) {
        super(sensorId, sequenceNumber);
        this.percentage = percentage;
    }

    public double getPercentage() { return percentage; }

    @Override
    public double getPrimaryValue() { return percentage; }

    @Override
    public String getUnit() { return "%"; }

    @Override
    public String toString() {
        return String.format("Humidity[%s]=%.1f%%", getSensorId(), percentage);
    }
}

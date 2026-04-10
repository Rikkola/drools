package org.drools.opentelemetry.demo.model;

public class GasConcentrationReading extends SensorReading {

    private final double ppm;

    public GasConcentrationReading(String sensorId, long sequenceNumber, double ppm) {
        super(sensorId, sequenceNumber);
        this.ppm = ppm;
    }

    public double getPpm() { return ppm; }

    @Override
    public double getPrimaryValue() { return ppm; }

    @Override
    public String getUnit() { return "ppm"; }

    @Override
    public String toString() {
        return String.format("Gas[%s]=%.1fppm", getSensorId(), ppm);
    }
}

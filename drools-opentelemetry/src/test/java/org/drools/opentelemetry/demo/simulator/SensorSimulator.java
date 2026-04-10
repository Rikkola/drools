package org.drools.opentelemetry.demo.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.drools.opentelemetry.demo.model.GasConcentrationReading;
import org.drools.opentelemetry.demo.model.HumidityReading;
import org.drools.opentelemetry.demo.model.PressureReading;
import org.drools.opentelemetry.demo.model.SensorReading;
import org.drools.opentelemetry.demo.model.TemperatureReading;
import org.drools.opentelemetry.demo.model.VibrationReading;

public class SensorSimulator {

    private final Random random = new Random(42);
    private final AtomicLong sequence = new AtomicLong(0);

    public enum Phase {
        NORMAL_OPS,
        TEMPERATURE_DRIFT,
        ANOMALY_SPIKE,
        CASCADING_ALERTS,
        OPERATOR_RESPONSE,
        RETURN_TO_NORMAL
    }

    public Phase getPhase(long elapsedSeconds) {
        if (elapsedSeconds < 180) return Phase.NORMAL_OPS;          // 0:00 - 3:00
        if (elapsedSeconds < 330) return Phase.TEMPERATURE_DRIFT;   // 3:00 - 5:30
        if (elapsedSeconds < 420) return Phase.ANOMALY_SPIKE;       // 5:30 - 7:00
        if (elapsedSeconds < 540) return Phase.CASCADING_ALERTS;    // 7:00 - 9:00
        if (elapsedSeconds < 720) return Phase.OPERATOR_RESPONSE;   // 9:00 - 12:00
        return Phase.RETURN_TO_NORMAL;                               // 12:00 - 15:00
    }

    public List<SensorReading> tick(long elapsedSeconds) {
        Phase phase = getPhase(elapsedSeconds);
        List<SensorReading> readings = new ArrayList<>();

        readings.add(generateTemperature(phase, elapsedSeconds));
        readings.add(generatePressure(phase));
        readings.add(generateVibration(phase));
        readings.add(generateGas(phase, elapsedSeconds));
        readings.add(generateHumidity(phase));

        return readings;
    }

    private TemperatureReading generateTemperature(Phase phase, long elapsed) {
        double value;
        switch (phase) {
            case NORMAL_OPS:
                value = 70.0 + noise(2.5);
                break;
            case TEMPERATURE_DRIFT:
                double progress = (elapsed - 180.0) / 150.0;
                value = 70.0 + (22.0 * progress) + noise(1.5);
                break;
            case ANOMALY_SPIKE:
                value = 95.0 + noise(3.0);
                break;
            case CASCADING_ALERTS:
                value = 93.0 + noise(2.0);
                break;
            case OPERATOR_RESPONSE:
                double coolProgress = (elapsed - 540.0) / 180.0;
                value = 93.0 - (21.0 * coolProgress) + noise(1.5);
                break;
            default: // RETURN_TO_NORMAL
                value = 70.0 + noise(2.5);
                break;
        }
        return new TemperatureReading("TEMP-001", sequence.incrementAndGet(), value);
    }

    private PressureReading generatePressure(Phase phase) {
        double value;
        switch (phase) {
            case ANOMALY_SPIKE:
            case CASCADING_ALERTS:
                value = 1.85 + noise(0.15);
                break;
            default:
                value = 1.25 + noise(0.15);
                break;
        }
        return new PressureReading("PRES-001", sequence.incrementAndGet(), value);
    }

    private VibrationReading generateVibration(Phase phase) {
        double value;
        switch (phase) {
            case ANOMALY_SPIKE:
                value = 5.5 + noise(1.0);
                break;
            case CASCADING_ALERTS:
                value = 4.0 + noise(0.8);
                break;
            case TEMPERATURE_DRIFT:
                value = 2.5 + noise(0.5);
                break;
            default:
                value = 1.2 + noise(0.4);
                break;
        }
        return new VibrationReading("VIB-001", sequence.incrementAndGet(), value);
    }

    private GasConcentrationReading generateGas(Phase phase, long elapsed) {
        double value;
        switch (phase) {
            case CASCADING_ALERTS:
                double gasProgress = (elapsed - 420.0) / 120.0;
                value = 50.0 + (170.0 * gasProgress) + noise(10.0);
                break;
            case OPERATOR_RESPONSE:
                double gasDropProgress = (elapsed - 540.0) / 180.0;
                value = 220.0 - (190.0 * gasDropProgress) + noise(8.0);
                break;
            default:
                value = 25.0 + noise(12.0);
                break;
        }
        return new GasConcentrationReading("GAS-001", sequence.incrementAndGet(), Math.max(0, value));
    }

    private HumidityReading generateHumidity(Phase phase) {
        double value;
        switch (phase) {
            case ANOMALY_SPIKE:
            case CASCADING_ALERTS:
                value = 72.0 + noise(5.0);
                break;
            default:
                value = 50.0 + noise(5.0);
                break;
        }
        return new HumidityReading("HUM-001", sequence.incrementAndGet(), value);
    }

    private double noise(double amplitude) {
        return (random.nextGaussian() * amplitude * 0.5);
    }
}

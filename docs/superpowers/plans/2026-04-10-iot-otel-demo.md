# IoT OpenTelemetry Demo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a 15-minute IoT sensor monitoring demo that showcases drools-opentelemetry with Grafana visualization.

**Architecture:** A standalone `main()` class inserts scenario-driven sensor readings into a Drools session every 2 seconds. The session is instrumented with `DroolsOpenTelemetry.instrument()`. Traces export via OTLP gRPC to Tempo; metrics are scraped by Prometheus from an embedded HTTP endpoint. Grafana displays a pre-provisioned dashboard with 4 panels.

**Tech Stack:** Drools (KieSession, DRL), OpenTelemetry SDK 1.40.0 (traces + metrics), Docker Compose (Grafana + Tempo + Prometheus)

---

## File Map

| Action | Path | Responsibility |
|---|---|---|
| Create | `src/test/java/org/drools/opentelemetry/demo/model/SensorReading.java` | Base class for all sensor readings |
| Create | `src/test/java/org/drools/opentelemetry/demo/model/TemperatureReading.java` | Temperature sensor reading |
| Create | `src/test/java/org/drools/opentelemetry/demo/model/PressureReading.java` | Pressure sensor reading |
| Create | `src/test/java/org/drools/opentelemetry/demo/model/VibrationReading.java` | Vibration sensor reading |
| Create | `src/test/java/org/drools/opentelemetry/demo/model/GasConcentrationReading.java` | Gas concentration reading |
| Create | `src/test/java/org/drools/opentelemetry/demo/model/HumidityReading.java` | Humidity sensor reading |
| Create | `src/test/java/org/drools/opentelemetry/demo/model/Alert.java` | Alert fact produced by rules |
| Create | `src/test/java/org/drools/opentelemetry/demo/model/PlantStatus.java` | Singleton plant state fact |
| Create | `src/test/java/org/drools/opentelemetry/demo/simulator/SensorSimulator.java` | Scenario-driven data generator |
| Create | `src/test/resources/org/drools/opentelemetry/demo/IotRules.drl` | 10 DRL rules |
| Create | `src/test/java/org/drools/opentelemetry/demo/IotMonitoringDemo.java` | Main class orchestrator |
| Create | `src/test/resources/demo/docker-compose.yml` | Grafana + Tempo + Prometheus |
| Create | `src/test/resources/demo/tempo/tempo.yml` | Tempo config |
| Create | `src/test/resources/demo/prometheus/prometheus.yml` | Prometheus config |
| Create | `src/test/resources/demo/grafana/provisioning/datasources/datasources.yml` | Grafana data sources |
| Create | `src/test/resources/demo/grafana/provisioning/dashboards/dashboards.yml` | Grafana dashboard provisioning |
| Create | `src/test/resources/demo/grafana/dashboards/drools-iot-monitoring.json` | Dashboard definition |
| Modify | `pom.xml` | Add test-scope dependencies |

All paths below are relative to `drools-opentelemetry/`.

---

### Task 1: Add Maven Dependencies

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add OpenTelemetry SDK, exporters, and drools-engine to pom.xml**

Add these dependencies inside the `<dependencies>` section, after the existing test dependencies:

```xml
        <!-- Demo dependencies (test scope) -->
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-sdk</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-exporter-otlp</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-exporter-prometheus</artifactId>
            <version>${opentelemetry.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.drools</groupId>
            <artifactId>drools-engine</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.drools</groupId>
            <artifactId>drools-mvel</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
```

Note: `opentelemetry-exporter-prometheus` may not be in the `opentelemetry-bom` at 1.40.0 — it's in `opentelemetry-java-instrumentation`. If it fails to resolve, use `io.opentelemetry.instrumentation:opentelemetry-prometheus-exporter:2.6.0` instead. Alternatively, use `opentelemetry-exporter-otlp` for metrics too and add an OpenTelemetry Collector container to the docker-compose that receives OTLP metrics and exposes a Prometheus scrape endpoint.

- [ ] **Step 2: Verify dependencies resolve**

Run: `mvn -pl drools-opentelemetry dependency:resolve -q`
Expected: BUILD SUCCESS, no resolution errors

- [ ] **Step 3: Commit**

```bash
git add drools-opentelemetry/pom.xml
git commit -m "chore: add OTel SDK and exporter deps for IoT demo"
```

---

### Task 2: Domain Model Classes

**Files:**
- Create: `src/test/java/org/drools/opentelemetry/demo/model/SensorReading.java`
- Create: `src/test/java/org/drools/opentelemetry/demo/model/TemperatureReading.java`
- Create: `src/test/java/org/drools/opentelemetry/demo/model/PressureReading.java`
- Create: `src/test/java/org/drools/opentelemetry/demo/model/VibrationReading.java`
- Create: `src/test/java/org/drools/opentelemetry/demo/model/GasConcentrationReading.java`
- Create: `src/test/java/org/drools/opentelemetry/demo/model/HumidityReading.java`
- Create: `src/test/java/org/drools/opentelemetry/demo/model/Alert.java`
- Create: `src/test/java/org/drools/opentelemetry/demo/model/PlantStatus.java`

- [ ] **Step 1: Create SensorReading base class**

```java
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
```

- [ ] **Step 2: Create TemperatureReading**

```java
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
```

- [ ] **Step 3: Create PressureReading**

```java
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
```

- [ ] **Step 4: Create VibrationReading**

```java
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
```

- [ ] **Step 5: Create GasConcentrationReading**

```java
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
```

- [ ] **Step 6: Create HumidityReading**

```java
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
```

- [ ] **Step 7: Create Alert**

```java
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
```

- [ ] **Step 8: Create PlantStatus**

```java
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
```

- [ ] **Step 9: Verify model compiles**

Run: `mvn -pl drools-opentelemetry test-compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 10: Commit**

```bash
git add drools-opentelemetry/src/test/java/org/drools/opentelemetry/demo/model/
git commit -m "feat: add IoT demo domain model classes"
```

---

### Task 3: Sensor Simulator

**Files:**
- Create: `src/test/java/org/drools/opentelemetry/demo/simulator/SensorSimulator.java`

- [ ] **Step 1: Create SensorSimulator**

The simulator holds a list of phase definitions. On each `tick(elapsedSeconds)`, it determines the current phase and generates 5 sensor readings using the phase's parameters.

```java
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
                // Linear climb from 70 to 92 over 150 seconds (3:00 - 5:30)
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
                // Cool down from 93 to 72 over 180 seconds (9:00 - 12:00)
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
                // Gas rises from 50 to 220 over 120 seconds (7:00 - 9:00)
                double gasProgress = (elapsed - 420.0) / 120.0;
                value = 50.0 + (170.0 * gasProgress) + noise(10.0);
                break;
            case OPERATOR_RESPONSE:
                // Gas drops from 220 to 30 over 180 seconds (9:00 - 12:00)
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
```

- [ ] **Step 2: Verify compiles**

Run: `mvn -pl drools-opentelemetry test-compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add drools-opentelemetry/src/test/java/org/drools/opentelemetry/demo/simulator/
git commit -m "feat: add scenario-driven sensor simulator for IoT demo"
```

---

### Task 4: DRL Rules

**Files:**
- Create: `src/test/resources/org/drools/opentelemetry/demo/IotRules.drl`

- [ ] **Step 1: Create IotRules.drl with all 10 rules**

```drl
package org.drools.opentelemetry.demo;

import org.drools.opentelemetry.demo.model.TemperatureReading;
import org.drools.opentelemetry.demo.model.PressureReading;
import org.drools.opentelemetry.demo.model.VibrationReading;
import org.drools.opentelemetry.demo.model.GasConcentrationReading;
import org.drools.opentelemetry.demo.model.HumidityReading;
import org.drools.opentelemetry.demo.model.Alert;
import org.drools.opentelemetry.demo.model.Alert.Severity;
import org.drools.opentelemetry.demo.model.PlantStatus;
import org.drools.opentelemetry.demo.model.PlantStatus.State;

// Rule 1: Threshold Warning
rule "Threshold Warning - Temperature"
    salience 90
    when
        $t : TemperatureReading(value > 80.0, value <= 90.0)
        $ps : PlantStatus()
        not Alert(source == "temperature", severity == Severity.WARNING, acknowledged == false)
    then
        System.out.println("  [WARN] Temperature warning: " + $t.getValue() + "°C");
        insert(new Alert(Severity.WARNING, "temperature", "Temperature above warning threshold: " + String.format("%.1f", $t.getValue()) + "°C"));
end

rule "Threshold Warning - Vibration"
    salience 90
    when
        $v : VibrationReading(rmsVelocity > 3.5, rmsVelocity <= 5.0)
        not Alert(source == "vibration", severity == Severity.WARNING, acknowledged == false)
    then
        System.out.println("  [WARN] Vibration warning: " + $v.getRmsVelocity() + "mm/s");
        insert(new Alert(Severity.WARNING, "vibration", "Vibration above warning threshold: " + String.format("%.2f", $v.getRmsVelocity()) + "mm/s"));
end

rule "Threshold Warning - Humidity"
    salience 90
    when
        $h : HumidityReading(percentage > 80.0 || percentage < 20.0)
        not Alert(source == "humidity", severity == Severity.WARNING, acknowledged == false)
    then
        System.out.println("  [WARN] Humidity warning: " + $h.getPercentage() + "%");
        insert(new Alert(Severity.WARNING, "humidity", "Humidity out of range: " + String.format("%.1f", $h.getPercentage()) + "%"));
end

// Rule 2: Threshold Critical
rule "Threshold Critical - Temperature"
    salience 100
    when
        $t : TemperatureReading(value > 90.0)
        $ps : PlantStatus(state != State.CRITICAL)
        not Alert(source == "temperature", severity == Severity.CRITICAL, acknowledged == false)
    then
        System.out.println("  [CRIT] Temperature critical: " + $t.getValue() + "°C");
        insert(new Alert(Severity.CRITICAL, "temperature", "Temperature CRITICAL: " + String.format("%.1f", $t.getValue()) + "°C"));
        modify($ps) { setState(State.CRITICAL) }
end

rule "Threshold Critical - Vibration"
    salience 100
    when
        $v : VibrationReading(rmsVelocity > 5.0)
        $ps : PlantStatus(state != State.CRITICAL)
        not Alert(source == "vibration", severity == Severity.CRITICAL, acknowledged == false)
    then
        System.out.println("  [CRIT] Vibration critical: " + $v.getRmsVelocity() + "mm/s");
        insert(new Alert(Severity.CRITICAL, "vibration", "Vibration CRITICAL: " + String.format("%.2f", $v.getRmsVelocity()) + "mm/s"));
        modify($ps) { setState(State.CRITICAL) }
end

// Rule 3: Temperature Drift Detection
rule "Temperature Drift Detection"
    salience 80
    when
        $t1 : TemperatureReading($s1 : sequenceNumber)
        $t2 : TemperatureReading(sequenceNumber > $s1, value > $t1.value)
        $t3 : TemperatureReading(sequenceNumber > $t2.sequenceNumber, value > $t2.value,
                                  (value - $t1.value) > 2.0)
        not Alert(source == "temperature-drift", acknowledged == false)
    then
        double delta = $t3.getValue() - $t1.getValue();
        System.out.println("  [WARN] Temperature drift detected: delta=" + String.format("%.1f", delta) + "°C");
        insert(new Alert(Severity.WARNING, "temperature-drift",
            "Temperature drift detected: " + String.format("%.1f", delta) + "°C rise over 3 readings"));
end

// Rule 4: Multi-Sensor Correlation
rule "Multi-Sensor Correlation"
    salience 95
    when
        $t : TemperatureReading(value > 85.0)
        $v : VibrationReading(rmsVelocity > 3.5)
        not Alert(source == "multi-sensor-correlation", acknowledged == false)
    then
        System.out.println("  [CRIT] Multi-sensor correlation: temp=" + $t.getValue() + "°C, vibration=" + $v.getRmsVelocity() + "mm/s");
        insert(new Alert(Severity.CRITICAL, "multi-sensor-correlation",
            "Mechanical failure risk: temp=" + String.format("%.1f", $t.getValue()) + "°C + vibration=" + String.format("%.2f", $v.getRmsVelocity()) + "mm/s"));
end

// Rule 5: Gas Leak Detection
rule "Gas Leak Detection"
    salience 95
    when
        $g : GasConcentrationReading(ppm > 100.0)
        $t : TemperatureReading(value > 85.0)
        not Alert(source == "gas-leak", acknowledged == false)
    then
        System.out.println("  [CRIT] Gas leak detected: ppm=" + $g.getPpm() + ", temp=" + $t.getValue() + "°C");
        insert(new Alert(Severity.CRITICAL, "gas-leak",
            "Possible thermal gas leak: " + String.format("%.0f", $g.getPpm()) + "ppm at " + String.format("%.1f", $t.getValue()) + "°C"));
end

// Rule 6: Alert Escalation
rule "Alert Escalation"
    salience 70
    when
        $ps : PlantStatus(state != State.CRITICAL)
        accumulate(
            Alert(severity == Severity.CRITICAL, acknowledged == false);
            $count : count();
            $count >= 3
        )
    then
        System.out.println("  [ESCALATION] " + $count + " critical alerts — escalating plant status");
        modify($ps) { setState(State.CRITICAL) }
end

// Rule 7: Cooldown Suppression
rule "Cooldown Suppression"
    salience 110
    when
        $existing : Alert(acknowledged == false, $source : source, $severity : severity)
        $duplicate : Alert(this != $existing, source == $source, severity == $severity,
                           acknowledged == false,
                           timestamp.isAfter($existing.timestamp))
    then
        System.out.println("  [COOLDOWN] Suppressing duplicate alert: " + $duplicate.getSource());
        retract($duplicate);
end

// Rule 8: Recovery Detection
rule "Recovery Detection"
    salience 50
    when
        $ps : PlantStatus(state == State.CRITICAL, secondsSinceLastChange > 60)
        $t : TemperatureReading(value <= 80.0)
        $v : VibrationReading(rmsVelocity <= 3.5)
        $g : GasConcentrationReading(ppm <= 100.0)
    then
        System.out.println("  [RECOVERY] All sensors returning to normal — starting recovery");
        modify($ps) { setState(State.RECOVERING) }
        insert(new Alert(Severity.INFO, "recovery", "System recovery detected — sensors returning to normal"));
end

// Rule 9: System Normalized
rule "System Normalized"
    salience 40
    when
        $ps : PlantStatus(state == State.RECOVERING, secondsSinceLastChange > 120)
        $t : TemperatureReading(value >= 60.0, value <= 80.0)
        $v : VibrationReading(rmsVelocity <= 2.0)
        $g : GasConcentrationReading(ppm <= 50.0)
    then
        System.out.println("  [NORMALIZED] System fully recovered");
        modify($ps) { setState(State.NORMAL) }
        // Acknowledge all existing alerts
        for (Object obj : drools.getKieRuntime().getObjects(o -> o instanceof Alert && !((Alert) o).isAcknowledged())) {
            Alert a = (Alert) obj;
            a.setAcknowledged(true);
        }
end

// Rule 10: Health Check
rule "Health Check"
    salience 10
    when
        $ps : PlantStatus(state == State.NORMAL)
        $t : TemperatureReading(value >= 60.0, value <= 80.0)
    then
        System.out.println("  [HEALTH] System OK — temp=" + String.format("%.1f", $t.getValue()) + "°C, status=NORMAL");
end
```

- [ ] **Step 2: Commit**

```bash
git add drools-opentelemetry/src/test/resources/org/drools/opentelemetry/demo/IotRules.drl
git commit -m "feat: add 10 IoT monitoring DRL rules"
```

---

### Task 5: Main Orchestrator Class

**Files:**
- Create: `src/test/java/org/drools/opentelemetry/demo/IotMonitoringDemo.java`

- [ ] **Step 1: Create IotMonitoringDemo.java**

```java
package org.drools.opentelemetry.demo;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.drools.opentelemetry.DroolsOpenTelemetry;
import org.drools.opentelemetry.demo.model.Alert;
import org.drools.opentelemetry.demo.model.PlantStatus;
import org.drools.opentelemetry.demo.model.SensorReading;
import org.drools.opentelemetry.demo.simulator.SensorSimulator;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

public class IotMonitoringDemo {

    private static final long DURATION_SECONDS = 15 * 60; // 15 minutes
    private static final long TICK_INTERVAL_MS = 2000;     // 2 seconds

    public static void main(String[] args) throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  DROOLS IoT MONITORING DEMO — OpenTelemetry Showcase");
        System.out.println("  Duration: 15 minutes | Grafana: http://localhost:3000");
        System.out.println("=".repeat(70) + "\n");

        // 1. Configure OpenTelemetry SDK
        Resource resource = Resource.getDefault().toBuilder()
                .put(AttributeKey.stringKey("service.name"), "drools-iot-demo")
                .put(AttributeKey.stringKey("service.version"), "1.0.0")
                .build();

        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:4317")
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();

        PrometheusHttpServer prometheusServer = PrometheusHttpServer.builder()
                .setPort(9464)
                .build();

        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(prometheusServer)
                .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .build();

        System.out.println("[Setup] OpenTelemetry configured — traces → localhost:4317, metrics → :9464");

        // 2. Build KieSession
        KieSession session = createKieSession();
        System.out.println("[Setup] KieSession created with IoT rules");

        // 3. Instrument session
        DroolsOpenTelemetry.instrument(session, openTelemetry);
        System.out.println("[Setup] Session instrumented with OpenTelemetry\n");

        // 4. Insert initial PlantStatus
        PlantStatus plantStatus = new PlantStatus(PlantStatus.State.NORMAL);
        session.insert(plantStatus);

        // 5. Create simulator
        SensorSimulator simulator = new SensorSimulator();

        // Track fact handles for retraction
        Map<String, FactHandle> currentReadings = new HashMap<>();

        Tracer tracer = openTelemetry.getTracer("drools-iot-demo");
        Instant startTime = Instant.now();
        long totalRulesFired = 0;
        long tickCount = 0;

        System.out.println("[Running] Starting 15-minute IoT monitoring simulation...\n");

        // 6. Main loop
        try {
            while (true) {
                long elapsedSeconds = Duration.between(startTime, Instant.now()).getSeconds();
                if (elapsedSeconds >= DURATION_SECONDS) break;

                SensorSimulator.Phase phase = simulator.getPhase(elapsedSeconds);

                // Create a parent span for this tick
                Span tickSpan = tracer.spanBuilder("iot-monitoring-tick")
                        .setAttribute("demo.phase", phase.name())
                        .setAttribute("demo.elapsed_seconds", elapsedSeconds)
                        .startSpan();

                int rulesFired;
                try (Scope scope = tickSpan.makeCurrent()) {
                    // Generate new readings
                    List<SensorReading> readings = simulator.tick(elapsedSeconds);

                    // Retract previous readings, insert new ones
                    for (SensorReading reading : readings) {
                        FactHandle old = currentReadings.get(reading.getSensorId());
                        if (old != null) {
                            session.delete(old);
                        }
                        FactHandle handle = session.insert(reading);
                        currentReadings.put(reading.getSensorId(), handle);
                    }

                    // Fire rules
                    rulesFired = session.fireAllRules();
                    totalRulesFired += rulesFired;
                    tickCount++;

                } finally {
                    tickSpan.end();
                }

                // Console output
                long minutes = elapsedSeconds / 60;
                long secs = elapsedSeconds % 60;
                System.out.printf("[%02d:%02d] Phase: %-20s | Rules fired: %d | Status: %s%n",
                        minutes, secs, phase, rulesFired, plantStatus.getState());

                // Print active alerts
                session.getObjects(o -> o instanceof Alert && !((Alert) o).isAcknowledged())
                        .forEach(o -> {
                            Alert a = (Alert) o;
                            String icon = a.getSeverity() == Alert.Severity.CRITICAL ? "!!!" :
                                          a.getSeverity() == Alert.Severity.WARNING ? " ! " : " i ";
                            System.out.printf("       %s %s%n", icon, a);
                        });

                Thread.sleep(TICK_INTERVAL_MS);
            }
        } finally {
            // 7. Shutdown
            System.out.println("\n" + "=".repeat(70));
            System.out.printf("  SIMULATION COMPLETE — %d ticks, %d total rules fired%n", tickCount, totalRulesFired);
            System.out.println("=".repeat(70));

            session.dispose();
            tracerProvider.close();
            meterProvider.close();

            System.out.println("\n[Shutdown] Session disposed, telemetry flushed");
            System.out.println("[Shutdown] View results at http://localhost:3000\n");
        }
    }

    private static KieSession createKieSession() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        // Load the DRL from classpath
        String drl = new String(
                IotMonitoringDemo.class.getResourceAsStream(
                        "/org/drools/opentelemetry/demo/IotRules.drl"
                ).readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8
        );

        kfs.write("src/main/resources/org/drools/opentelemetry/demo/IotRules.drl", drl);

        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("DRL build errors:\n" + kieBuilder.getResults().getMessages());
        }

        KieContainer kContainer = ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
        return kContainer.newKieSession();
    }
}
```

Note: If `PrometheusHttpServer` fails to resolve (it may be in a separate artifact), the fallback is to send metrics via OTLP too:
- Replace `PrometheusHttpServer` with `OtlpGrpcMetricExporter`
- Add an OpenTelemetry Collector container to docker-compose that receives OTLP metrics and exposes Prometheus format
- The plan addresses this in Task 6 with an alternative docker-compose

- [ ] **Step 2: Verify compiles**

Run: `mvn -pl drools-opentelemetry test-compile -q`
Expected: BUILD SUCCESS. If `PrometheusHttpServer` fails to resolve, switch to the OTLP metric exporter fallback described above.

- [ ] **Step 3: Commit**

```bash
git add drools-opentelemetry/src/test/java/org/drools/opentelemetry/demo/IotMonitoringDemo.java
git commit -m "feat: add IoT monitoring demo main class"
```

---

### Task 6: Docker Compose and Infrastructure Configs

**Files:**
- Create: `src/test/resources/demo/docker-compose.yml`
- Create: `src/test/resources/demo/tempo/tempo.yml`
- Create: `src/test/resources/demo/prometheus/prometheus.yml`

- [ ] **Step 1: Create Tempo config**

```yaml
# tempo.yml
server:
  http_listen_port: 3200

distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: "0.0.0.0:4317"

storage:
  trace:
    backend: local
    local:
      path: /var/tempo/traces

metrics_generator:
  storage:
    path: /var/tempo/wal
```

- [ ] **Step 2: Create Prometheus config**

```yaml
# prometheus.yml
global:
  scrape_interval: 5s
  evaluation_interval: 5s

scrape_configs:
  - job_name: "drools-iot-demo"
    static_configs:
      - targets: ["host.docker.internal:9464"]
        labels:
          application: "drools-iot-demo"
```

- [ ] **Step 3: Create docker-compose.yml**

```yaml
version: "3.8"

services:
  tempo:
    image: grafana/tempo:2.3.1
    command: ["-config.file=/etc/tempo/tempo.yml"]
    volumes:
      - ./tempo/tempo.yml:/etc/tempo/tempo.yml:ro
    ports:
      - "4317:4317"   # OTLP gRPC
      - "3200:3200"   # Tempo HTTP API

  prometheus:
    image: prom/prometheus:v2.48.1
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    ports:
      - "9090:9090"
    extra_hosts:
      - "host.docker.internal:host-gateway"

  grafana:
    image: grafana/grafana:10.2.3
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
      - ./grafana/dashboards:/var/lib/grafana/dashboards:ro
    ports:
      - "3000:3000"
    depends_on:
      - tempo
      - prometheus
```

- [ ] **Step 4: Commit**

```bash
git add drools-opentelemetry/src/test/resources/demo/docker-compose.yml \
       drools-opentelemetry/src/test/resources/demo/tempo/ \
       drools-opentelemetry/src/test/resources/demo/prometheus/
git commit -m "feat: add docker-compose with Tempo and Prometheus for IoT demo"
```

---

### Task 7: Grafana Provisioning and Dashboard

**Files:**
- Create: `src/test/resources/demo/grafana/provisioning/datasources/datasources.yml`
- Create: `src/test/resources/demo/grafana/provisioning/dashboards/dashboards.yml`
- Create: `src/test/resources/demo/grafana/dashboards/drools-iot-monitoring.json`

- [ ] **Step 1: Create datasources.yml**

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false

  - name: Tempo
    type: tempo
    access: proxy
    url: http://tempo:3200
    editable: false
```

- [ ] **Step 2: Create dashboards.yml**

```yaml
apiVersion: 1

providers:
  - name: "default"
    orgId: 1
    folder: ""
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    options:
      path: /var/lib/grafana/dashboards
      foldersFromFilesStructure: false
```

- [ ] **Step 3: Create drools-iot-monitoring.json**

This is the Grafana dashboard JSON. It defines 4 panels:

```json
{
  "annotations": { "list": [] },
  "editable": true,
  "fiscalYearStartMonth": 0,
  "graphTooltip": 1,
  "links": [],
  "panels": [
    {
      "title": "Rules Fired / sec",
      "type": "timeseries",
      "gridPos": { "h": 8, "w": 12, "x": 0, "y": 0 },
      "datasource": { "type": "prometheus", "uid": "PBFA97CFB590B2093" },
      "targets": [
        {
          "expr": "rate(drools_rules_fired_total[30s])",
          "legendFormat": "{{drools_rule_name}}",
          "refId": "A"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": { "mode": "palette-classic" },
          "custom": {
            "axisBorderShow": false,
            "drawStyle": "line",
            "fillOpacity": 10,
            "lineWidth": 2,
            "pointSize": 5,
            "showPoints": "auto",
            "stacking": { "mode": "none" }
          },
          "unit": "ops"
        },
        "overrides": []
      },
      "options": { "legend": { "displayMode": "table", "placement": "right" } }
    },
    {
      "title": "Rule Firing Duration (ms)",
      "type": "heatmap",
      "gridPos": { "h": 8, "w": 12, "x": 12, "y": 0 },
      "datasource": { "type": "prometheus", "uid": "PBFA97CFB590B2093" },
      "targets": [
        {
          "expr": "rate(drools_rules_firing_duration_milliseconds_bucket[30s])",
          "legendFormat": "{{le}}",
          "format": "heatmap",
          "refId": "A"
        }
      ],
      "options": {
        "color": { "mode": "scheme", "scheme": "Oranges" },
        "yAxis": { "unit": "ms" }
      }
    },
    {
      "title": "Matches Created vs Cancelled / sec",
      "type": "timeseries",
      "gridPos": { "h": 8, "w": 12, "x": 0, "y": 8 },
      "datasource": { "type": "prometheus", "uid": "PBFA97CFB590B2093" },
      "targets": [
        {
          "expr": "rate(drools_matches_created_total[30s])",
          "legendFormat": "created",
          "refId": "A"
        },
        {
          "expr": "rate(drools_matches_cancelled_total[30s])",
          "legendFormat": "cancelled",
          "refId": "B"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": { "mode": "palette-classic" },
          "custom": {
            "drawStyle": "line",
            "fillOpacity": 20,
            "lineWidth": 2
          },
          "unit": "ops"
        },
        "overrides": [
          {
            "matcher": { "id": "byName", "options": "cancelled" },
            "properties": [
              { "id": "color", "value": { "fixedColor": "red", "mode": "fixed" } }
            ]
          }
        ]
      },
      "options": { "legend": { "displayMode": "list", "placement": "bottom" } }
    },
    {
      "title": "Recent Traces",
      "type": "traces",
      "gridPos": { "h": 8, "w": 12, "x": 12, "y": 8 },
      "datasource": { "type": "tempo", "uid": "P214B5B846CF3925F" },
      "targets": [
        {
          "queryType": "nativeSearch",
          "serviceName": "drools-iot-demo",
          "limit": 20,
          "refId": "A"
        }
      ]
    }
  ],
  "refresh": "5s",
  "schemaVersion": 38,
  "tags": ["drools", "iot", "opentelemetry"],
  "templating": { "list": [] },
  "time": { "from": "now-15m", "to": "now" },
  "timepicker": {},
  "timezone": "",
  "title": "Drools IoT Monitoring",
  "uid": "drools-iot-demo"
}
```

Note: The datasource UIDs (`PBFA97CFB590B2093` for Prometheus, `P214B5B846CF3925F` for Tempo) are Grafana-generated defaults for provisioned datasources. If they don't match at runtime, Grafana will still resolve by name. If panels show "No data source", edit the dashboard JSON to replace the UIDs with the values shown in Grafana's datasource settings page, or change the targets to use `"datasource": "Prometheus"` / `"datasource": "Tempo"` by name.

- [ ] **Step 4: Commit**

```bash
git add drools-opentelemetry/src/test/resources/demo/grafana/
git commit -m "feat: add Grafana provisioning and dashboard for IoT demo"
```

---

### Task 8: End-to-End Verification

- [ ] **Step 1: Compile the full module**

Run: `mvn -pl drools-opentelemetry test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: Start the infrastructure**

Run from the repo root:
```bash
cd drools-opentelemetry/src/test/resources/demo && docker-compose up -d
```
Expected: All 3 containers start. Verify with `docker-compose ps` — all should show "Up".

- [ ] **Step 3: Run the demo**

Run:
```bash
cd /path/to/drools
mvn -pl drools-opentelemetry exec:java \
  -Dexec.mainClass="org.drools.opentelemetry.demo.IotMonitoringDemo" \
  -Dexec.classpathScope="test"
```

Alternatively, run `IotMonitoringDemo.main()` directly from your IDE (it's in test sources, so ensure the IDE includes test classpath).

Expected: Console output showing phase transitions, rule firings, and alerts over 15 minutes.

- [ ] **Step 4: Verify Grafana dashboard**

Open `http://localhost:3000` in a browser. Navigate to Dashboards → "Drools IoT Monitoring".

Expected:
- "Rules Fired / sec" panel shows periodic spikes during anomaly phases
- "Rule Firing Duration" heatmap shows activity
- "Matches Created vs Cancelled" shows cooldown suppression
- "Recent Traces" panel shows clickable trace spans

If datasource UIDs don't match, go to Grafana → Configuration → Data Sources, note the UIDs, and update the dashboard JSON accordingly.

- [ ] **Step 5: Stop infrastructure after demo**

Run: `cd drools-opentelemetry/src/test/resources/demo && docker-compose down`

- [ ] **Step 6: Final commit**

If any fixes were needed during verification, commit them:
```bash
git add -A drools-opentelemetry/
git commit -m "fix: adjustments from end-to-end IoT demo verification"
```

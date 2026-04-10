# IoT OpenTelemetry Demo — Design Spec

**Date:** 2026-04-10
**Module:** drools-opentelemetry (existing)
**Purpose:** A 15-minute standalone demo showcasing drools-opentelemetry with a scripted IoT anomaly scenario, visualized in Grafana.

---

## Scenario Story

A simulated industrial plant monitoring system. Five sensors feed readings into a Drools session over 15 minutes. A scripted story unfolds in six phases:

| Phase | Minutes | What happens | What the rules detect |
|---|---|---|---|
| Normal ops | 0:00–3:00 | All sensors within range, steady readings | Nothing fires except periodic health checks |
| Temperature drift | 3:00–5:30 | Temperature climbs gradually 70°C → 92°C | Rate-of-change rule detects drift, issues WARNING |
| Anomaly spike | 5:30–7:00 | Temperature hits 95°C+, vibration spikes | Threshold rules fire, multi-sensor correlation triggers CRITICAL |
| Cascading alerts | 7:00–9:00 | Gas concentration rises (simulating leak) | Alert escalation chain fires, cooldown suppresses duplicates |
| Operator response | 9:00–12:00 | Temperature drops, gas normalizes | Recovery detection rules fire, alerts downgraded |
| Return to normal | 12:00–15:00 | All sensors stabilize | Health check rules confirm system OK |

---

## Domain Model

### Sensor Readings

| Sensor | Field | Normal range | Warning | Critical | Unit |
|---|---|---|---|---|---|
| Temperature | value | 65–75°C | >80°C | >90°C | °C |
| Pressure | value | 1.0–1.5 bar | >1.8 | >2.0 | bar |
| Vibration | rmsVelocity | 0.5–2.0 mm/s | >3.5 | >5.0 | mm/s |
| Gas concentration | ppm | 0–50 ppm | >100 | >200 | ppm |
| Humidity | percentage | 40–60% | >80% or <20% | N/A | % |

Each reading carries `sensorId`, `timestamp`, and `sequenceNumber`. New readings inserted every 2 seconds per sensor (~2.5 inserts/sec total). Old readings retracted after processing.

### Alert

```
Alert {
    severity: INFO | WARNING | CRITICAL
    source: String
    message: String
    timestamp: Instant
    acknowledged: boolean
}
```

### PlantStatus

```
PlantStatus {
    state: NORMAL | DEGRADED | CRITICAL | RECOVERING
    lastStateChange: Instant
}
```

---

## Rules (10 DRL rules)

| # | Rule name | Fires when | Produces |
|---|---|---|---|
| 1 | Threshold Warning | Any sensor above warning threshold | Alert(WARNING) |
| 2 | Threshold Critical | Any sensor above critical threshold | Alert(CRITICAL), PlantStatus → CRITICAL |
| 3 | Temperature Drift | 3+ consecutive rising temp readings, delta > 2°C | Alert(WARNING, "temperature drift detected") |
| 4 | Multi-Sensor Correlation | Temp > 85°C AND vibration > 3.5 mm/s within 30s | Alert(CRITICAL, "mechanical failure risk") |
| 5 | Gas Leak Detection | Gas ppm > 100 AND temp > 85°C | Alert(CRITICAL, "possible thermal gas leak") |
| 6 | Alert Escalation | 3+ CRITICAL alerts within 60s | PlantStatus → CRITICAL if not already |
| 7 | Cooldown Suppression | Duplicate alert same source within 30s | Retracts duplicate |
| 8 | Recovery Detection | All readings normal for 60s after CRITICAL | PlantStatus → RECOVERING, Alert(INFO) |
| 9 | System Normalized | RECOVERING for 120s, all normal | PlantStatus → NORMAL |
| 10 | Health Check | Every 30s when NORMAL | Logs heartbeat (visible as periodic span) |

---

## Infrastructure — Docker Compose

Three containers:

- **Tempo** — receives traces via OTLP gRPC on port 4317
- **Prometheus** — scrapes metrics from the Java demo on port 9464
- **Grafana** — pre-provisioned dashboard on port 3000 with Tempo and Prometheus as data sources

The Java demo:
- Pushes spans to Tempo via `opentelemetry-exporter-otlp`
- Exposes metrics via `opentelemetry-exporter-prometheus` on an embedded HTTP server (port 9464)

---

## Grafana Dashboard — 4 Panels

| Panel | Type | Query | Purpose |
|---|---|---|---|
| Rules Fired / sec | Time series | `rate(drools_rules_fired_total[30s])` by rule name | Shows burst during anomaly vs. quiet during normal ops |
| Rule Firing Duration | Heatmap | `drools_rules_firing_duration_milliseconds` histogram | Shows cost of complex rules (correlation > threshold) |
| Matches Created vs Cancelled | Time series (dual axis) | `rate(drools_matches_created_total[30s])` and `rate(drools_matches_cancelled_total[30s])` | Shows cooldown suppression in action |
| Recent Traces | Tempo trace list | `service.name=drools-iot-demo` | Click to explore rule firing spans and cascading chains |

Dashboard provisioned via JSON file mounted into Grafana — no manual setup.

---

## Project Structure

All files in the existing `drools-opentelemetry` module under `src/test/`:

```
drools-opentelemetry/
├── pom.xml                          (add OTel SDK + exporter deps, test scope)
└── src/test/
    ├── java/org/drools/opentelemetry/demo/
    │   ├── IotMonitoringDemo.java          (main class)
    │   ├── model/
    │   │   ├── SensorReading.java          (base class)
    │   │   ├── TemperatureReading.java
    │   │   ├── PressureReading.java
    │   │   ├── VibrationReading.java
    │   │   ├── GasConcentrationReading.java
    │   │   ├── HumidityReading.java
    │   │   ├── Alert.java
    │   │   └── PlantStatus.java
    │   └── simulator/
    │       └── SensorSimulator.java        (scenario-driven generator)
    └── resources/
        ├── org/drools/opentelemetry/demo/
        │   └── IotRules.drl
        └── demo/
            ├── docker-compose.yml
            ├── grafana/
            │   ├── provisioning/
            │   │   ├── datasources/datasources.yml
            │   │   └── dashboards/dashboards.yml
            │   └── dashboards/
            │       └── drools-iot-monitoring.json
            ├── prometheus/
            │   └── prometheus.yml
            └── tempo/
                └── tempo.yml
```

---

## Main Class Lifecycle

```
main()
  1. Configure OTel SDK (TracerProvider + MeterProvider + exporters)
  2. Build KieSession from IotRules.drl
  3. DroolsOpenTelemetry.instrument(session, otel)
  4. Insert PlantStatus(NORMAL)
  5. Create SensorSimulator with scenario timeline
  6. Loop 15 minutes:
     - simulator.tick(elapsed) → new readings
     - Retract previous readings for same sensor
     - Insert new readings
     - session.fireAllRules()
     - Print console summary
     - Sleep 2 seconds
  7. Shutdown: dispose session, flush exporters, print stats
```

---

## New Maven Dependencies (test scope)

- `opentelemetry-sdk`
- `opentelemetry-exporter-otlp`
- `opentelemetry-exporter-prometheus`
- `drools-mvel`

---

## How to Run

1. `cd drools-opentelemetry/src/test/resources/demo && docker-compose up -d`
2. Run `IotMonitoringDemo.main()` from IDE or `mvn exec:java`
3. Open `http://localhost:3000` (Grafana, no login required)
4. Watch the dashboard populate over 15 minutes

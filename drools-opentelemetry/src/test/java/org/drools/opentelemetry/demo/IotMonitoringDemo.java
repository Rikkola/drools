/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.opentelemetry.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
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

    private static final long DURATION_SECONDS = 15 * 60;
    private static final long TICK_INTERVAL_MS = 2000;

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

        OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
                .setEndpoint("http://localhost:4317")
                .build();

        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(
                        PeriodicMetricReader.builder(metricExporter)
                                .setInterval(Duration.ofSeconds(5))
                                .build())
                .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .build();

        System.out.println("[Setup] OpenTelemetry configured — OTLP → localhost:4317 (traces + metrics)");

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

        try {
            String drl = new String(
                    IotMonitoringDemo.class.getResourceAsStream(
                            "/org/drools/opentelemetry/demo/IotRules.drl"
                    ).readAllBytes(),
                    StandardCharsets.UTF_8
            );
            kfs.write("src/main/resources/org/drools/opentelemetry/demo/IotRules.drl", drl);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load IotRules.drl from classpath", e);
        }

        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("DRL build errors:\n" + kieBuilder.getResults().getMessages());
        }

        KieContainer kContainer = ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
        return kContainer.newKieSession();
    }
}

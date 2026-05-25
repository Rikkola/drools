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
package org.drools.modelcompiler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.drools.model.Rule;
import org.drools.model.TimedKind;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.model.view.TimedExprViewItem;
import org.drools.modelcompiler.domain.SensorEvents.HeartbeatOk;
import org.drools.modelcompiler.domain.SensorEvents.MonitoringStation;
import org.drools.modelcompiler.domain.SensorEvents.OperatorAcknowledged;
import org.drools.modelcompiler.domain.SensorEvents.SensorActivated;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.and;
import static org.drools.model.PatternDSL.never;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;
import static org.drools.model.PatternDSL.within;

public class PatternDSLSequenceWithinTest {

    private final Variable<HeartbeatOk>          heartbeat       = declarationOf(HeartbeatOk.class);
    private final Variable<OperatorAcknowledged> ack             = declarationOf(OperatorAcknowledged.class);
    private final Variable<MonitoringStation>    station         = declarationOf(MonitoringStation.class);
    private final Variable<SensorActivated>      sensorActivated = declarationOf(SensorActivated.class);
    private KieSession ksession;

    @AfterEach
    public void tearDown() {
        if (ksession != null) {
            ksession.dispose();
        }
    }

    private void insertAndFire(Object... facts) {
        for (Object fact : facts) {
            ksession.insert(fact);
        }
        ksession.fireAllRules();
    }

    @Test
    public void withinRejectsNullTimeout() {
        assertThatThrownBy(() -> within(null, pattern(ack)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void withinRejectsZeroTimeout() {
        assertThatThrownBy(() -> within(Duration.ZERO, pattern(ack)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void withinRejectsNegativeTimeout() {
        assertThatThrownBy(() -> within(Duration.ofSeconds(-1), pattern(ack)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void withinWrapsStepWithTimeout() {
        TimedExprViewItem<?> timed = within(Duration.ofSeconds(5), pattern(ack));
        assertThat(timed.getDurationMillis()).isEqualTo(5000L);
        assertThat(timed.getKind()).isEqualTo(TimedKind.TIMEOUT);
    }

    @Test
    public void sequenceAdvancesWhenStepMatchesBeforeDeadline() {
        final List<String> results = new ArrayList<>();

        Rule r = rule("within-met").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        within(Duration.ofSeconds(30),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice")))
                ),
                execute(() -> results.add("acknowledged"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));     // step 0 matches → within-step activates, timer scheduled
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(10)); // still inside the 30s window
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceAbortsWhenDeadlineExpires() {
        final List<String> results = new ArrayList<>();

        Rule r = rule("within-expired").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        within(Duration.ofSeconds(30),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice")))
                ),
                execute(() -> results.add("acknowledged"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // within-step activates, timer scheduled
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(31)); // blow past the 30s deadline, process timeout job → sequencer stops
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // too late — sequencer aborted

        assertThat(results).isEmpty();
    }

    @Test
    public void sequenceAdvancesWhenCompositeStepCompletesBeforeDeadline() {
        // within(d, and(...)) — the timer attaches to whatever buildStepGate returns,
        // so a composite gate should obey the deadline just like a single pattern.
        final List<String> results = new ArrayList<>();

        Rule r = rule("within-composite-met").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        within(Duration.ofSeconds(30),
                                and(
                                        pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                ))
                ),
                execute(() -> results.add("acknowledged"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));     // within-step activates, timer scheduled
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(10)); // still inside the 30s window
        insertAndFire(new HeartbeatOk("sensor-1"));         // AND child #1 — composite still waiting
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // AND child #2 — composite completes

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceAbortsWhenCompositeStepHalfCompletesAtDeadline() {
        // Only one AND child arrives before the deadline. The timer governs the whole
        // composite gate, so the partially-satisfied step must still abort the sequencer.
        final List<String> results = new ArrayList<>();

        Rule r = rule("within-composite-expired").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        within(Duration.ofSeconds(30),
                                and(
                                        pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                ))
                ),
                execute(() -> results.add("acknowledged"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // within-step activates, timer scheduled
        insertAndFire(new HeartbeatOk("sensor-1"));          // AND child #1 only — composite half-satisfied
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(31)); // blow past the 30s deadline, process timeout job → sequencer stops
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // too late — sequencer aborted

        assertThat(results).isEmpty();
    }

    @Test
    public void withinRejectsSelfRevertingInnerStep() {
        Rule r = rule("within-never-rejected").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        within(Duration.ofSeconds(30),
                                never(pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")))),
                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                ),
                execute(() -> { })
        );

        assertThatThrownBy(() -> KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support self-reverting steps at the top level")
                .hasMessageContaining("ADR 0001");
    }

    @Test
    public void withinRejectsNestedWithin() {
        Rule r = rule("within-nested-rejected").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        within(Duration.ofSeconds(30),
                                within(Duration.ofSeconds(10),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))))
                ),
                execute(() -> { })
        );

        assertThatThrownBy(() -> KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("one temporal decorator per step");
    }
}

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
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
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
import static org.drools.model.PatternDSL.armAfter;
import static org.drools.model.PatternDSL.never;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;

public class PatternDSLSequenceArmAfterTest {

    private final Variable<MonitoringStation>    station         = declarationOf(MonitoringStation.class);
    private final Variable<SensorActivated>      sensorActivated = declarationOf(SensorActivated.class);
    private final Variable<OperatorAcknowledged> ack             = declarationOf(OperatorAcknowledged.class);
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
    public void armAfter_matchAfterWindow_propagates() {
        // sequence: anchor → armAfter(30s, ack) → final propagation.
        // After 30s quiet window elapses post-activation, the first matching ack should propagate.
        final List<String> results = new ArrayList<>();

        Rule r = rule("armAfter-happy").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        armAfter(Duration.ofSeconds(30),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice")))
                ),
                execute(() -> results.add("armed-and-fired"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));                       // armAfter activates, window opens at t=0
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(31)); // past 30s → gate armed (in spec)
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));         // first post-window match should propagate

        assertThat(results).containsExactly("armed-and-fired");
    }

    @Test
    public void armAfter_matchDuringWindow_isDropped_thenPostWindowMatchPropagates() {
        // anchor → armAfter(30s, ack) → first ack arrives mid-window (dropped), second ack arrives past-window (propagates).
        final List<String> results = new ArrayList<>();

        Rule r = rule("armAfter-drop").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        armAfter(Duration.ofSeconds(30),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice")))
                ),
                execute(() -> results.add("armed-and-fired"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));                        // window opens at t=0
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(10)); // t=10 — still inside the window
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));          // mid-window match — must be dropped (gate not armed)
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(25)); // t=35 — past 30s window, gate now armed
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));          // second match — should propagate

        assertThat(results).containsExactly("armed-and-fired");
    }

    @Test
    public void armAfter_indefiniteWaitAfterArming_doesNotFail() {
        // After the window expires the gate stays armed indefinitely.
        // No deadline; if no match arrives the sequence simply doesn't fire — it does NOT fail.
        final List<String> results = new ArrayList<>();

        Rule r = rule("armAfter-indefinite").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        armAfter(Duration.ofSeconds(30),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice")))
                ),
                execute(() -> results.add("armed-and-fired"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));                         // window opens
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(30));  // window expires, gate arms
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(300)); // 10× longer with no match — must not fail

        assertThat(results).isEmpty();
    }

    @Test
    public void armAfterRejectsNullDuration() {
        assertThatThrownBy(() -> armAfter(null, pattern(ack)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void armAfterRejectsZeroDuration() {
        assertThatThrownBy(() -> armAfter(Duration.ZERO, pattern(ack)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void armAfterRejectsNegativeDuration() {
        assertThatThrownBy(() -> armAfter(Duration.ofSeconds(-1), pattern(ack)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void armAfterRejectsNullStep() {
        assertThatThrownBy(() -> armAfter(Duration.ofSeconds(1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-null step");
    }

    @Test
    public void armAfterRejectsSelfRevertingInnerStep() {
        Rule r = rule("armAfter-never-rejected").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        armAfter(Duration.ofSeconds(1),
                                never(pattern(ack).expr("ack", a -> a.getOperator().equals("alice")))),
                        pattern(ack).expr("ack-final", a -> a.getOperator().equals("alice"))
                ),
                execute(() -> { })
        );

        assertThatThrownBy(() -> KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support self-reverting steps at the top level")
                .hasMessageContaining("ADR 0001");
    }
}

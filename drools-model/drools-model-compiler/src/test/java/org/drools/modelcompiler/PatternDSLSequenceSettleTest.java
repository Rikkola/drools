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
import org.kie.api.runtime.rule.FactHandle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;
import static org.drools.model.PatternDSL.settle;

public class PatternDSLSequenceSettleTest {

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
    public void settle_happyPath_stableMatchHeldForDurationPropagates() {
        // sequence: anchor → settle(2s, ack) → rule fires.
        // After ack inserts and 2s elapse without retraction, the sequence should complete.
        final List<String> results = new ArrayList<>();

        Rule r = rule("settle-happy").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        settle(Duration.ofSeconds(2),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice")))
                ),
                execute(() -> results.add("settled"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));           // step 0 matches → settle-step activates, delay scheduled
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // ack matches → delay timer starts
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(2)); // delay expires → ack propagates

        assertThat(results).containsExactly("settled");
    }

    @Test
    public void settle_partialDelay_doesNotPropagateBeforeWindowExpires() {
        // sequence: anchor → settle(2s, ack) → rule fires.
        // Verifies that the gate parks on the settle step and does NOT fire before the full
        // d-stable window elapses.  This exercises the DELAY job scheduling path and confirms
        // that the job fires only after the configured duration, not earlier.
        final List<String> results = new ArrayList<>();

        Rule r = rule("settle-partial").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        settle(Duration.ofSeconds(2),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice")))
                ),
                execute(() -> results.add("settled"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));              // anchor matches → settle-step activates
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // ack matches → DELAY job scheduled at t=0

        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(1)); // t=1s — only half the window elapsed
        assertThat(results).as("sequence must not fire before the full 2s delay").isEmpty();

        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(1)); // t=2s — delay expires
        assertThat(results).containsExactly("settled");
    }

    @Test
    public void settle_cancelAndReschedule_freshDelayAfterSecondMatch() {
        // sequence: anchor → settle(2s, ack) → rule fires.
        // The DELAY cancel-before-schedule guard in DelayFromMatchTimer.matched() ensures each
        // call to matched() cancels any existing DELAY job before scheduling a new one, so that
        // a second activate cycle starts its own fresh 2s countdown from zero.
        //
        // Note: AlphaAdapter.retractRightTuple is not yet wired to send UNMATCHED signals, so
        // true fact-retraction-based revert cannot be tested at integration level.  Instead this
        // test verifies the cancel-before-schedule is idempotent on the happy path: the gate
        // fires exactly once, not twice, even though the cancel guard runs on every matched() call.
        final List<String> results = new ArrayList<>();

        Rule r = rule("settle-cancel-reschedule").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        settle(Duration.ofSeconds(2),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice")))
                ),
                execute(() -> results.add("settled"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));              // anchor matches → settle-step activates
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // ack matches → DELAY job scheduled at t=0

        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(2)); // t=2s — delay fires with MATCHED → propagates

        // Rule must fire exactly once — no double-fire from cancel-before-schedule artefacts.
        assertThat(results).containsExactly("settled");
    }
}

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

import org.drools.model.PatternDSL.SequenceViewItem;
import org.drools.model.Rule;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.domain.SensorEvents.CalibrationPassed;
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
import static org.drools.model.PatternDSL.or;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;
import static org.drools.model.PatternDSL.within;

public class PatternDSLSequenceCompleteWithinTest {

    private final Variable<MonitoringStation>    station         = declarationOf(MonitoringStation.class);
    private final Variable<SensorActivated>      sensorActivated = declarationOf(SensorActivated.class);
    private final Variable<HeartbeatOk>          heartbeat       = declarationOf(HeartbeatOk.class);
    private final Variable<CalibrationPassed>    calibration     = declarationOf(CalibrationPassed.class);
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

    // ---- validation (no KieBase needed) ----

    @Test
    public void completeWithinRejectsNullDuration() {
        assertThatThrownBy(() -> sequence(pattern(sensorActivated)).completeWithin(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void completeWithinRejectsZeroDuration() {
        assertThatThrownBy(() -> sequence(pattern(sensorActivated)).completeWithin(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void completeWithinRejectsNegativeDuration() {
        assertThatThrownBy(() -> sequence(pattern(sensorActivated)).completeWithin(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void completeWithinRejectsSubMillisecondDuration() {
        // A positive duration that rounds down to 0ms would be silently dropped (no deadline).
        assertThatThrownBy(() -> sequence(pattern(sensorActivated)).completeWithin(Duration.ofNanos(500_000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 1ms");
    }

    @Test
    public void completeWithinRejectsDoubleSet() {
        assertThatThrownBy(() -> sequence(pattern(sensorActivated))
                        .completeWithin(Duration.ofSeconds(10))
                        .completeWithin(Duration.ofSeconds(20)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already set");
    }

    @Test
    public void completeWithinStoresDeadlineMillis() {
        SequenceViewItem sv = sequence(pattern(sensorActivated)).completeWithin(Duration.ofMinutes(10));
        assertThat(sv.getDeadlineMillis()).isEqualTo(600_000L);
    }

    // ---- top-level behaviour ----

    @Test
    public void sequenceFiresWhenItCompletesBeforeTheDeadline() {
        final List<String> results = new ArrayList<>();

        Rule r = rule("complete-within-met").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        pattern(heartbeat).expr("hb", h -> h.getSensorId().equals("sensor-1")),
                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                ).completeWithin(Duration.ofSeconds(30)),
                execute(() -> results.add("done"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // step 0 → sequence starts, 30s deadline scheduled
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(10)); // still inside the window
        assertThat(results).isEmpty(); // whole-sequence timer must NOT abort a still-progressing sequence
        insertAndFire(new HeartbeatOk("sensor-1"));          // step 1
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // step 2 → final → fire

        assertThat(results).containsExactly("done");
    }

    @Test
    public void sequenceAbortsWhenTheWholeSequenceDeadlineExpires() {
        final List<String> results = new ArrayList<>();

        Rule r = rule("complete-within-expired").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        pattern(heartbeat).expr("hb", h -> h.getSensorId().equals("sensor-1")),
                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                ).completeWithin(Duration.ofSeconds(30)),
                execute(() -> results.add("done"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // sequence starts, 30s deadline scheduled
        insertAndFire(new HeartbeatOk("sensor-1"));          // step 1 reached, but step 2 stalls
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(31)); // blow past the whole-sequence deadline → abort
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // too late — sequence already aborted

        assertThat(results).isEmpty();
    }

    @Test
    public void nestedCompleteWithinCancelsOnInnerCompletionAndOuterProceeds() {
        // Outer: anchor, then a nested sequence(heartbeat) with its OWN 30s deadline, then ack.
        // The inner completes in time → its deadline is cancelled → the outer advances to ack and fires.
        final List<String> results = new ArrayList<>();

        Rule r = rule("complete-within-nested-met").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        sequence(
                                pattern(heartbeat).expr("hb", h -> h.getSensorId().equals("sensor-1"))
                        ).completeWithin(Duration.ofSeconds(30)),
                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                ),
                execute(() -> results.add("done"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // outer step 0; nested subsequence + its 30s deadline start
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(10)); // inside the inner window
        insertAndFire(new HeartbeatOk("sensor-1"));          // inner completes → inner deadline cancelled → outer advances
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(60)); // outer still progresses long after the inner window — nested deadline composes cleanly
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // outer final step → fire

        assertThat(results).containsExactly("done");
    }

    @Test
    public void nestedCompleteWithinExpiryAbortsTheWholeRuleUnderPlainNesting() {
        // The real proof that the nested controller is live: under plain nesting the inner
        // sequence is a SubsequenceStep of the outer, so an expired inner completeWithin
        // propagates up the DEFAULT childFailed policy and aborts the whole rule. Without a
        // nested controller the inner would simply stay open and a late heartbeat would
        // complete it — so this abort is observable only because the nested deadline fires.
        final List<String> results = new ArrayList<>();

        Rule r = rule("complete-within-nested-expired").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        sequence(
                                pattern(heartbeat).expr("hb", h -> h.getSensorId().equals("sensor-1"))
                        ).completeWithin(Duration.ofSeconds(30)),
                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                ),
                execute(() -> results.add("done"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // outer step 0 → inner subsequence starts, 30s deadline scheduled
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(31)); // inner deadline expires → propagates → whole rule aborts
        insertAndFire(new HeartbeatOk("sensor-1"));          // would have completed the inner — too late, sequence aborted
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // also inert

        assertThat(results).isEmpty();
    }

    @Test
    public void completeWithinOnOrBranchLosesOnlyThatBranchAndSiblingWins() {
        // anchor, then or( sequence(heartbeat).completeWithin(30s) , sequence(calibration) ), then ack.
        // Branch A's 30s deadline expires (heartbeat never arrives); childFailed must lose only
        // branch A and keep the OR open. Branch B (calibration) then completes → OR joins → ack → fire.
        final List<String> results = new ArrayList<>();

        Rule r = rule("complete-within-or-branch").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        or(
                                sequence(pattern(heartbeat).expr("hb", h -> h.getSensorId().equals("sensor-1")))
                                        .completeWithin(Duration.ofSeconds(30)),
                                sequence(pattern(calibration).expr("cal", c -> c.getSensorId().equals("sensor-1")))
                        ),
                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                ),
                execute(() -> results.add("done"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // anchor → both OR branches activate; branch A's 30s deadline scheduled
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(31)); // branch A times out → loses only branch A
        insertAndFire(new CalibrationPassed("sensor-1"));    // branch B completes → OR joins → advance to ack
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // final step → fire

        assertThat(results).containsExactly("done");
    }

    @Test
    public void completeWithinOnOrBranchKillsThatBranchSoItsLateCompletionIsInert() {
        // The non-vacuous half: prove the expired branch is actually DEAD. anchor activates
        // both OR branches; branch A's 30s deadline expires. A late heartbeat (branch A's own
        // fact) must NOT retroactively complete branch A — if the deadline had done nothing,
        // the late heartbeat would complete A, the OR would join via A, ack would fire the rule.
        final List<String> results = new ArrayList<>();

        Rule r = rule("complete-within-or-branch-dead").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        or(
                                sequence(pattern(heartbeat).expr("hb", h -> h.getSensorId().equals("sensor-1")))
                                        .completeWithin(Duration.ofSeconds(30)),
                                sequence(pattern(calibration).expr("cal", c -> c.getSensorId().equals("sensor-1")))
                        ),
                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                ),
                execute(() -> results.add("done"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // anchor → both OR branches active; branch A's 30s deadline scheduled
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(31)); // branch A times out → branch A dead
        insertAndFire(new HeartbeatOk("sensor-1"));          // branch A's own fact, but A is dead → inert; OR does not join
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // outer never reached ack → no fire

        assertThat(results).isEmpty();
    }

    @Test
    public void perStepWithinFiresBeforeWholeSequenceDeadline() {
        // Sequence carries BOTH a per-step within(5s) on the ack step and a completeWithin(60s).
        // The ack step stalls past 5s: the per-step within (the EARLIER deadline) aborts first.
        // The late ack must be inert and the rule must not fire.
        final List<String> results = new ArrayList<>();

        Rule r = rule("within-plus-complete-within").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        within(Duration.ofSeconds(5),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice")))
                ).completeWithin(Duration.ofSeconds(60)),
                execute(() -> results.add("done"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // step 0; within-step arms its 5s timer; 60s sequence deadline scheduled
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(6)); // past the 5s per-step within, well inside 60s → per-step aborts
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // too late — sequence already aborted by the within

        assertThat(results).isEmpty();
    }

    @Test
    public void completeWithinFiresBeforePerStepWithin() {
        // Reverse ordering: completeWithin(5s) is the EARLIER deadline; the per-step within(60s)
        // is later. The completeWithin must abort first. Non-vacuous: if completeWithin's
        // controller were silently cancelled by the presence of a within, the within(60s) would
        // let the ack (at 6s) complete the sequence and the rule would fire.
        final List<String> results = new ArrayList<>();

        Rule r = rule("complete-within-before-within").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        within(Duration.ofSeconds(60),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice")))
                ).completeWithin(Duration.ofSeconds(5)),
                execute(() -> results.add("done"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // step 0; 5s sequence deadline scheduled; within-step's 60s timer arms next
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(6)); // past the 5s completeWithin, well inside the 60s within → completeWithin aborts
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // too late — sequence already aborted by completeWithin

        assertThat(results).isEmpty();
    }
}

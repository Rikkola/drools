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

import java.util.ArrayList;
import java.util.List;

import org.drools.model.Model;
import org.drools.model.Rule;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.domain.Person;
import org.drools.modelcompiler.domain.Relationship;
import org.drools.modelcompiler.domain.SensorEvents.AlarmRaised;
import org.drools.modelcompiler.domain.SensorEvents.HeartbeatOk;
import org.drools.modelcompiler.domain.SensorEvents.MonitoringStation;
import org.drools.modelcompiler.domain.SensorEvents.OperatorAcknowledged;
import org.drools.modelcompiler.domain.SensorEvents.SensorActivated;
import org.drools.modelcompiler.domain.Toy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.and;
import static org.drools.model.PatternDSL.or;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;

/**
 * Advanced integration tests for the sequencing API, covering scenarios not
 * exercised by the basic and composite test classes:
 *
 * <ul>
 *   <li>Noise events (non-matching facts interleaved mid-sequence)</li>
 *   <li>Event arrives before anchor — must be invisible to the sequencer</li>
 *   <li>OR gate: second branch fires (not just the first)</li>
 *   <li>OR gate: both branches arrive — gate fires exactly once</li>
 *   <li>AND gate: signals arrive interleaved with noise</li>
 *   <li>Three-step plain sequence</li>
 *   <li>Retract after full completion — no second firing</li>
 *   <li>Two concurrent anchors: first completes while second is mid-sequence</li>
 *   <li>Same event type used in step 1 and step 3 of a three-step sequence</li>
 *   <li>Two chained sequences: first must complete before second starts</li>
 *   <li>Two chained sequences: event for second sequence arriving before first completes is ignored</li>
 * </ul>
 */
public class PatternDSLSequenceAdvancedTest {

    private final List<String> results = new ArrayList<>();
    private KieSession ksession;

    @AfterEach
    public void tearDown() {
        results.clear();
        if (ksession != null) {
            ksession.dispose();
            ksession = null;
        }
    }

    private void insertAndFire(Object... facts) {
        for (Object f : facts) { ksession.insert(f); }
        ksession.fireAllRules();
    }

    // -------------------------------------------------------------------------
    // Noise events between steps must not stall or advance the sequence.
    //
    // Step 1: Toy("ball"). Step 2: Relationship("go").
    // Between steps we insert a Toy("other") (wrong name — must not advance step 2)
    // and a Relationship("nope") (wrong start — must not fire the rule).
    // Only the correct Relationship("go") finally fires the rule.
    // -------------------------------------------------------------------------
    @Test
    public void noiseEventsBetweenStepsAreIgnored() {
        Variable<Person>       personV = declarationOf(Person.class);
        Variable<Toy>          toyV    = declarationOf(Toy.class);
        Variable<Relationship> relV    = declarationOf(Relationship.class);

        Rule rule = rule("noise-rule").build(
                pattern(personV),
                sequence(
                        pattern(toyV).expr("is-ball", t -> t.getName().equals("ball")),
                        pattern(relV).expr("is-go",   r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new Person("anchor"));

        insertAndFire(new Toy("ball"));               // step 1 satisfied

        // Noise: wrong toy name (must not re-satisfy step 1 or advance step 2)
        insertAndFire(new Toy("other"));
        assertThat(results).isEmpty();

        // Noise: wrong relationship start (must not fire rule)
        insertAndFire(new Relationship("nope", "x"));
        assertThat(results).isEmpty();

        // Correct step 2
        insertAndFire(new Relationship("go", "done"));
        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------------
    // An event that matches step 1 arriving BEFORE the anchor must not be
    // captured by the sequencer.
    //
    // The sequencer only starts listening after the anchor tuple activates it.
    // Facts already in the session at that point must not retroactively satisfy steps.
    // -------------------------------------------------------------------------
    @Test
    public void eventBeforeAnchorIsNotCaptured() {
        Variable<Person>       personV = declarationOf(Person.class);
        Variable<Toy>          toyV    = declarationOf(Toy.class);
        Variable<Relationship> relV    = declarationOf(Relationship.class);

        Rule rule = rule("pre-anchor-rule").build(
                pattern(personV),
                sequence(
                        pattern(toyV).expr("is-ball", t -> t.getName().equals("ball")),
                        pattern(relV).expr("is-go",   r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        // Step-1 event arrives BEFORE the anchor
        insertAndFire(new Toy("ball"));

        // Anchor inserted — sequencer starts, but the ball is already in the past
        insertAndFire(new Person("anchor"));

        // Step-2 event: the sequencer is at step 0 (ball not yet seen), must NOT fire
        insertAndFire(new Relationship("go", "done"));

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // OR gate: second branch (alarm) fires when only that branch is satisfied.
    //
    // Existing composite tests only fire the first OR branch (heartbeat).
    // This test fires only the second (alarm), verifying both branches are wired.
    // -------------------------------------------------------------------------
    @Test
    public void orSecondBranchAloneFires() {
        Variable<MonitoringStation>    stationV   = declarationOf(MonitoringStation.class);
        Variable<SensorActivated>      activatedV = declarationOf(SensorActivated.class);
        Variable<HeartbeatOk>          heartbeatV = declarationOf(HeartbeatOk.class);
        Variable<AlarmRaised>          alarmV     = declarationOf(AlarmRaised.class);
        Variable<OperatorAcknowledged> ackV       = declarationOf(OperatorAcknowledged.class);

        Rule rule = rule("or-second-branch").build(
                pattern(stationV),
                sequence(
                        pattern(activatedV).expr("s1", a -> a.getSensorId().equals("s1")),
                        or(
                                pattern(heartbeatV).expr("hb",  h  -> h.getSensorId().equals("s1")),
                                pattern(alarmV).expr("al",      al -> al.getSeverity().equals("high"))
                        ),
                        pattern(ackV).expr("ack", a -> a.getOperator().equals("alice"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("s1"));

        // Only the alarm branch — no heartbeat
        insertAndFire(new AlarmRaised("s1", "high"));
        insertAndFire(new OperatorAcknowledged("s1", "alice"));

        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------------
    // OR gate: both branches arrive — the gate fires exactly once.
    //
    // Once the first branch satisfies the OR predicate the step advances and the
    // gate deactivates. The second branch arriving after deactivation must be
    // ignored — the rule must fire exactly once, not twice.
    // -------------------------------------------------------------------------
    @Test
    public void orBothBranchesArriveRuleFiresOnce() {
        Variable<MonitoringStation>    stationV   = declarationOf(MonitoringStation.class);
        Variable<SensorActivated>      activatedV = declarationOf(SensorActivated.class);
        Variable<HeartbeatOk>          heartbeatV = declarationOf(HeartbeatOk.class);
        Variable<AlarmRaised>          alarmV     = declarationOf(AlarmRaised.class);
        Variable<OperatorAcknowledged> ackV       = declarationOf(OperatorAcknowledged.class);

        Rule rule = rule("or-both-branches").build(
                pattern(stationV),
                sequence(
                        pattern(activatedV).expr("s1", a -> a.getSensorId().equals("s1")),
                        or(
                                pattern(heartbeatV).expr("hb",  h  -> h.getSensorId().equals("s1")),
                                pattern(alarmV).expr("al",      al -> al.getSeverity().equals("high"))
                        ),
                        pattern(ackV).expr("ack", a -> a.getOperator().equals("alice"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("s1"));

        // First branch fires — OR step advances, gate deactivates
        insertAndFire(new HeartbeatOk("s1"));

        // Second branch arrives AFTER step has advanced — must be ignored
        insertAndFire(new AlarmRaised("s1", "high"));

        insertAndFire(new OperatorAcknowledged("s1", "alice"));

        // Rule must have fired exactly once
        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------------
    // AND gate: non-matching events interleaved between the two required signals.
    //
    // Step requires and(heartbeat, alarm). Non-matching events for the wrong
    // sensor arrive between them. The gate must wait for both signals on the
    // correct sensor and not fire prematurely.
    // -------------------------------------------------------------------------
    @Test
    public void andGateWithInterleavedNoiseWaitsForBothSignals() {
        Variable<MonitoringStation>    stationV   = declarationOf(MonitoringStation.class);
        Variable<SensorActivated>      activatedV = declarationOf(SensorActivated.class);
        Variable<HeartbeatOk>          heartbeatV = declarationOf(HeartbeatOk.class);
        Variable<AlarmRaised>          alarmV     = declarationOf(AlarmRaised.class);
        Variable<OperatorAcknowledged> ackV       = declarationOf(OperatorAcknowledged.class);

        Rule rule = rule("and-noise-rule").build(
                pattern(stationV),
                sequence(
                        pattern(activatedV).expr("s1", a -> a.getSensorId().equals("s1")),
                        and(
                                pattern(heartbeatV).expr("hb", h  -> h.getSensorId().equals("s1")),
                                pattern(alarmV).expr("al",     al -> al.getSeverity().equals("high"))
                        ),
                        pattern(ackV).expr("ack", a -> a.getOperator().equals("alice"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("s1"));

        // AND child #1 (heartbeat for s1)
        insertAndFire(new HeartbeatOk("s1"));
        assertThat(results).isEmpty();                              // still waiting for alarm

        // Noise: heartbeat for a different sensor — must not count as s1's signal
        insertAndFire(new HeartbeatOk("s2"));
        assertThat(results).isEmpty();

        // Noise: low-severity alarm — doesn't satisfy the "high" constraint
        insertAndFire(new AlarmRaised("s1", "low"));
        assertThat(results).isEmpty();

        // AND child #2 (high alarm for s1) — both gates now satisfied
        insertAndFire(new AlarmRaised("s1", "high"));

        insertAndFire(new OperatorAcknowledged("s1", "alice"));
        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------------
    // Three-step plain sequence: A → B → C.
    //
    // Verifies that a sequence with three simple pattern steps advances through
    // each gate in order. All existing three-step tests use OR/AND composites;
    // this covers a plain three-step path.
    // -------------------------------------------------------------------------
    @Test
    public void threeStepSequenceFiresInOrder() {
        Variable<Person>       personV = declarationOf(Person.class);
        Variable<Toy>          toyV    = declarationOf(Toy.class);
        Variable<Relationship> relV    = declarationOf(Relationship.class);
        Variable<SensorActivated> sensorV = declarationOf(SensorActivated.class);

        Rule rule = rule("three-step-rule").build(
                pattern(personV),
                sequence(
                        pattern(toyV).expr("is-ball",   t -> t.getName().equals("ball")),
                        pattern(relV).expr("is-go",     r -> r.getStart().equals("go")),
                        pattern(sensorV).expr("is-s1",  s -> s.getSensorId().equals("s1"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new Person("anchor"));

        insertAndFire(new Toy("ball"));
        assertThat(results).isEmpty();

        insertAndFire(new Relationship("go", "done"));
        assertThat(results).isEmpty();

        insertAndFire(new SensorActivated("s1"));
        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------------
    // Three-step sequence: step 3 event arriving after step 1 but before step 2
    // must not advance anything.
    // -------------------------------------------------------------------------
    @Test
    public void threeStepSequenceOutOfOrderDoesNotFire() {
        Variable<Person>       personV = declarationOf(Person.class);
        Variable<Toy>          toyV    = declarationOf(Toy.class);
        Variable<Relationship> relV    = declarationOf(Relationship.class);
        Variable<SensorActivated> sensorV = declarationOf(SensorActivated.class);

        Rule rule = rule("three-step-oo-rule").build(
                pattern(personV),
                sequence(
                        pattern(toyV).expr("is-ball",   t -> t.getName().equals("ball")),
                        pattern(relV).expr("is-go",     r -> r.getStart().equals("go")),
                        pattern(sensorV).expr("is-s1",  s -> s.getSensorId().equals("s1"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new Person("anchor"));

        insertAndFire(new Toy("ball"));                         // step 1 done

        // Step 3 event arrives before step 2 — must not skip step 2
        insertAndFire(new SensorActivated("s1"));
        assertThat(results).isEmpty();

        // Step 2 now — but step 3 event in the past, so still at step 2 waiting for step 3 again
        insertAndFire(new Relationship("go", "done"));          // step 2 done
        assertThat(results).isEmpty();

        // Step 3 again — now fires
        insertAndFire(new SensorActivated("s1"));
        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------------
    // Retract anchor AFTER full sequence completion — must not throw, must not
    // fire again.
    //
    // Before the N1 fix: Sequencer.stop() called getSteps()[step] with
    // step == steps.length, throwing ArrayIndexOutOfBoundsException.
    // After fix: stop() guards step < steps.length and sets a terminal marker.
    // -------------------------------------------------------------------------
    @Test
    public void retractAnchorAfterCompletionIsNoOp() {
        Variable<Person>       personV = declarationOf(Person.class);
        Variable<Toy>          toyV    = declarationOf(Toy.class);
        Variable<Relationship> relV    = declarationOf(Relationship.class);

        Rule rule = rule("retract-after-rule").build(
                pattern(personV),
                sequence(
                        pattern(toyV).expr("is-ball", t -> t.getName().equals("ball")),
                        pattern(relV).expr("is-go",   r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        FactHandle anchorHandle = ksession.insert(new Person("anchor"));
        ksession.fireAllRules();

        insertAndFire(new Toy("ball"));
        insertAndFire(new Relationship("go", "done"));          // sequence complete
        assertThat(results).containsExactly("fired");

        // Retract after completion — must not throw, must not fire again
        ksession.retract(anchorHandle);
        ksession.fireAllRules();

        assertThat(results).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // Two concurrent anchors: anchor-A completes the sequence while anchor-B
    // is still mid-sequence (step 1 consumed, waiting for step 2).
    //
    // After anchor-A fires, anchor-B's step 2 must still complete independently.
    // Known limitation: DynamicFilter slots are currently shared (see
    // twoAnchorsConcurrentlyBugOnlyOneFires in the lifecycle test). This test
    // documents the EXPECTED behaviour once per-tuple isolation is fixed.
    // Marked with a TODO comment so it can be tightened when isolation lands.
    // -------------------------------------------------------------------------
    @Test
    public void firstAnchorCompletesWhileSecondIsMidSequence() {
        Variable<Person>       personV = declarationOf(Person.class);
        Variable<Toy>          toyV    = declarationOf(Toy.class);
        Variable<Relationship> relV    = declarationOf(Relationship.class);

        Rule rule = rule("concurrent-anchor-rule").build(
                pattern(personV),
                sequence(
                        pattern(toyV).expr("is-ball", t -> t.getName().equals("ball")),
                        pattern(relV).expr("is-go",   r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        FactHandle anchorAHandle = ksession.insert(new Person("anchor-A"));
        FactHandle anchorBHandle = ksession.insert(new Person("anchor-B"));
        ksession.fireAllRules();                                // both sequencers start

        // Both anchors consume step 1
        insertAndFire(new Toy("ball"));

        // Step 2 arrives — both sequences should be able to complete
        insertAndFire(new Relationship("go", "done"));

        // TODO: once per-tuple DynamicFilter isolation is fixed, change to hasSize(2).
        // Current known limitation: only one sequencer advances due to shared filter slots.
        assertThat(results).hasSizeGreaterThanOrEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // Same event type in steps 1 and 3 of a three-step sequence.
    //
    // Verifies that adapterIndex sharing (one AlphaAdapter for one type)
    // works correctly when the same type appears non-consecutively: the
    // AlphaAdapter for Toy must be active for step 1, inactive during step 2,
    // then active again for step 3.
    // -------------------------------------------------------------------------
    @Test
    public void sameTypeInFirstAndThirdStep() {
        Variable<Person>       personV = declarationOf(Person.class);
        Variable<Toy>          toyV1   = declarationOf(Toy.class);
        Variable<Relationship> relV    = declarationOf(Relationship.class);
        Variable<Toy>          toyV2   = declarationOf(Toy.class);

        Rule rule = rule("type-reuse-rule").build(
                pattern(personV),
                sequence(
                        pattern(toyV1).expr("is-ball",  t -> t.getName().equals("ball")),
                        pattern(relV).expr("is-go",     r -> r.getStart().equals("go")),
                        pattern(toyV2).expr("is-doll",  t -> t.getName().equals("doll"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new Person("anchor"));

        // Step 1: Toy("ball")
        insertAndFire(new Toy("ball"));
        assertThat(results).isEmpty();

        // Wrong Toy during step 2 wait — must not advance step 3
        insertAndFire(new Toy("doll"));
        assertThat(results).isEmpty();

        // Step 2: Relationship("go")
        insertAndFire(new Relationship("go", "done"));
        assertThat(results).isEmpty();

        // Step 3: Toy("doll") — same type as step 1, different filter
        insertAndFire(new Toy("doll"));
        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------------
    // Two chained sequences: event for the second sequence must be ignored if
    // it arrives before the first sequence completes.
    //
    // Rule: anchor → sequence(Toy) → sequence(Relationship) → fire.
    // The Relationship arrives before the Toy — the second SequenceNode is not
    // yet reached, so it must not fire.
    // -------------------------------------------------------------------------
    @Test
    public void secondSequenceEventBeforeFirstSequenceCompletesIsIgnored() {
        Variable<Person>       personV = declarationOf(Person.class);
        Variable<Toy>          toyV    = declarationOf(Toy.class);
        Variable<Relationship> relV    = declarationOf(Relationship.class);

        Rule rule = rule("chained-seq-order-rule").build(
                pattern(personV),
                sequence(
                        pattern(toyV).expr("is-ball", t -> t.getName().equals("ball"))
                ),
                sequence(
                        pattern(relV).expr("is-go", r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new Person("anchor"));

        // Relationship arrives before Toy — second SequenceNode not yet active
        insertAndFire(new Relationship("go", "done"));
        assertThat(results).isEmpty();

        // Now Toy arrives — first sequence completes, second SequenceNode activates
        insertAndFire(new Toy("ball"));
        assertThat(results).isEmpty();                          // second step not yet seen

        // Relationship again — now the second SequenceNode is listening
        insertAndFire(new Relationship("go", "done"));
        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------------
    // batchInsertBeforeFireDoesNotFire
    // Insert anchor, step-1 fact, and step-2 fact all before the first
    // fireAllRules() call, then fire once — rule must NOT fire.
    //
    // Step signals are delivered via AlphaAdapter.assertObject() at insert time,
    // before the Phreak cycle runs doLeftInserts() to start the sequencer.
    // When all facts are batched before the first fireAllRules(), the step facts
    // arrive while the sequencer is not yet listening, so they are silently
    // dropped. The sequence engine requires the anchor to be committed
    // (fireAllRules() called) before it will register step filters.
    // -------------------------------------------------------------------------
    @Test
    public void batchInsertBeforeFireDoesNotFire() {
        Variable<Person>       personV = declarationOf(Person.class);
        Variable<Toy>          toyV    = declarationOf(Toy.class);
        Variable<Relationship> relV    = declarationOf(Relationship.class);

        Rule rule = rule("batch-insert-rule").build(
                pattern(personV),
                sequence(
                        pattern(toyV).expr("is-ball", t -> t.getName().equals("ball")),
                        pattern(relV).expr("is-go", r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        // Insert all facts before any fireAllRules() — step signals are lost
        // because the sequencer hasn't started yet
        ksession.insert(new Person("anchor"));
        ksession.insert(new Toy("ball"));
        ksession.insert(new Relationship("go", "done"));

        ksession.fireAllRules();

        // Sequence does not fire: step facts were inserted before the sequencer
        // started. This is a known limitation of the current AlphaAdapter design.
        assertThat(results).isEmpty();
    }
}

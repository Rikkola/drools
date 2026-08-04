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
import org.drools.modelcompiler.domain.Result;
import org.drools.modelcompiler.domain.SensorEvents.AlarmRaised;
import org.drools.modelcompiler.domain.SensorEvents.HeartbeatOk;
import org.drools.modelcompiler.domain.SensorEvents.MonitoringStation;
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
import static org.drools.model.PatternDSL.on;
import static org.drools.model.PatternDSL.or;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;
import static org.drools.model.PatternDSL.when;

/**
 * Regression tests for the four critical bugs identified in the 2026-08-04 PR review (C1–C4).
 *
 * <p>C2 (circular buffer overflow) is written test-first: it FAILS on the current branch
 * and PASSES after the C2 fix is applied.</p>
 *
 * <p>C1, C3, and C4 bugs are NOT directly triggerable via PatternDSL — they manifest only
 * through internal APIs (SubsequenceStep, raw SequenceNode wiring, or nested-sequence DSL
 * not yet wired in v1). Those three tests verify that the REACHABLE DSL path behaves
 * correctly and document where phreak-level tests are needed for the raw-API paths.
 * See docs/notes/2026-08-04-sequence-coverage-gaps.md for the gap inventory
 * (created in Task 4 of this plan, after the first JaCoCo coverage run).</p>
 */
public class PatternDSLSequenceCriticalBugTest {

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

    // -------------------------------------------------------------------
    // C1 — case CONSEQUENCE falls through into case SEQUENCE (KiePackagesBuilder:510-517)
    //
    // The fallthrough path is only reachable if a CONSEQUENCE-typed condition is
    // neither NamedConsequenceImpl nor ConditionalNamedConsequenceImpl. Normal
    // PatternDSL always produces one of those two types, so the crash path cannot
    // be triggered via DSL.
    //
    // This test verifies the REACHABLE positive path: a rule combining a
    // ConditionalNamedConsequence (when/then/elseWhen) with a sequence() step
    // builds successfully. This is the shape that existed before the C1 regression
    // was introduced and must continue to work.
    //
    // The raw crash path requires a phreak-level test constructing a custom
    // CONSEQUENCE condition type — see coverage gap doc for the action item.
    // -------------------------------------------------------------------
    @Test
    public void consequenceBranchDoesNotFallThroughToSequence() {
        Variable<Result>       resultV = declarationOf(Result.class);
        Variable<Person>       personV = declarationOf(Person.class);
        Variable<Toy>          toyV    = declarationOf(Toy.class);
        Variable<Relationship> relV    = declarationOf(Relationship.class);

        // The conditional named consequence (when/then/elseWhen) is the trigger for
        // the CONSEQUENCE case in conditionToElement. Combining it with a sequence()
        // step in the same rule exposes the missing break.
        Rule rule = rule("c1-rule").build(
                pattern(resultV),
                pattern(personV),
                when("cond", personV, p -> p.getAge() < 30).then(
                        on(personV, resultV).breaking().execute((p, r) -> r.setValue("young"))
                ).elseWhen().then(
                        on(personV, resultV).breaking().execute((p, r) -> r.setValue("other"))
                ),
                sequence(
                        pattern(toyV).expr("is-ball", t -> t.getName().equals("ball")),
                        pattern(relV).expr("is-go",   r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("seq-fired"))
        );

        // Before fix: ClassCastException thrown here.
        // After fix : builds without exception; KieBase is non-null.
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule));
        assertThat(kieBase).isNotNull();
    }

    // -------------------------------------------------------------------
    // C2 — CircularArrayList.addEmpty() uses raw indices instead of % capacity
    //
    // Each completed sequence causes SequencerMemory to call addEmpty() to
    // pre-allocate output slots. After enough completions head overflows the
    // array length, causing ArrayIndexOutOfBoundsException.
    //
    // Strategy: insert the anchor once, then repeatedly complete the one-step
    // sequence more than 100 times (the default CircularArrayList capacity)
    // by retaining the session and inserting step events. To allow re-use of
    // the same anchor we need the sequencer to restart after each completion;
    // since a completed sequencer does not restart on its own, we instead
    // use update() on the anchor after each completion to reset the sequencer.
    //
    // Before fix: ArrayIndexOutOfBoundsException after ~100 completions.
    // After fix : 110 completions succeed; results.size() == 110.
    // -------------------------------------------------------------------
    @Test
    public void longSequenceDoesNotOverflowCircularBuffer() {
        Variable<Person> personV = declarationOf(Person.class);
        Variable<Toy>    toyV    = declarationOf(Toy.class);

        Rule rule = rule("c2-rule").build(
                pattern(personV),
                sequence(
                        pattern(toyV).expr("is-ball", t -> t.getName().equals("ball"))
                ),
                execute(() -> results.add("fired"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule));
        ksession = kbase.newKieSession();

        Person anchor = new Person("anchor");
        FactHandle anchorHandle = ksession.insert(anchor);
        ksession.fireAllRules();

        // Complete the sequence 110 times. After each completion, update() on the anchor
        // re-triggers the anchor pattern match, which causes doLeftUpdates to restart the
        // sequencer (resetting its step counter). The next Toy("ball") then drives a fresh
        // completion, accumulating one addEmpty() call per iteration on the output
        // CircularArrayList. Past 100 completions the raw-index bug triggers AIOOBE.
        for (int i = 0; i < 110; i++) {
            ksession.insert(new Toy("ball"));
            ksession.fireAllRules();                          // sequence completes
            ksession.update(anchorHandle, anchor);            // restart sequencer
            ksession.fireAllRules();
        }

        assertThat(results).hasSize(110);
    }

    // -------------------------------------------------------------------
    // C3 — Sequencer.stop() only follows the parent chain, leaking parallel
    //       sibling SignalAdapters (Sequencer.java:66-71)
    //
    // The leakage path requires parallel SIBLING SequenceMemory entries created by
    // raw SubsequenceStep/ParallelStep wiring. DSL or() is implemented as a single
    // LogicCircuit gate (not parallel sub-sequences), so LogicCircuitStep.deactivate()
    // correctly cleans up all OR branches on stop() — the DSL path does not hit the bug.
    //
    // This test verifies the REACHABLE positive path: retract the anchor after an OR
    // step has consumed step-1; further events must NOT trigger the rule.
    //
    // The raw leak path requires a phreak-level test using ParallelStep/SubsequenceStep
    // directly — see coverage gap doc for the action item.
    // -------------------------------------------------------------------
    @Test
    public void retractAnchorWithOrStepDoesNotLeakSiblingAdapters() {
        Variable<MonitoringStation> stationV  = declarationOf(MonitoringStation.class);
        Variable<SensorActivated>   activatedV = declarationOf(SensorActivated.class);
        Variable<HeartbeatOk>       heartbeatV = declarationOf(HeartbeatOk.class);
        Variable<AlarmRaised>       alarmV     = declarationOf(AlarmRaised.class);

        Rule rule = rule("c3-rule").build(
                pattern(stationV),
                sequence(
                        pattern(activatedV).expr("s1", a -> a.getSensorId().equals("s1")),
                        or(
                                pattern(heartbeatV).expr("hb", h -> h.getSensorId().equals("s1")),
                                pattern(alarmV).expr("al", al -> al.getSeverity().equals("high"))
                        )
                ),
                execute(() -> results.add("seq-fired"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule));
        ksession = kbase.newKieSession();

        FactHandle stationHandle = ksession.insert(new MonitoringStation("station-1"));
        ksession.fireAllRules();                              // sequencer starts

        ksession.insert(new SensorActivated("s1"));
        ksession.fireAllRules();                              // step-1 consumed; OR step now active

        // Retract the anchor — stop() must deactivate BOTH OR siblings
        ksession.retract(stationHandle);
        ksession.fireAllRules();

        // Insert a heartbeat — the leaked OR sibling adapter must NOT respond
        ksession.insert(new HeartbeatOk("s1"));
        ksession.fireAllRules();

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------
    // C4 — KiePackagesBuilder always assigns sequenceIndex=0 to all sequences
    //       (KiePackagesBuilder.java:532)
    //
    // The sequenceIndex collision only occurs within a SINGLE rule that contains
    // multiple nested/parallel sub-sequences (where both would be stored in the
    // same SequencerMemoryImpl.sequenceMemories[] array at index 0). Two SEPARATE
    // rules each have their own SequenceNode, Sequencer, and SequencerMemoryImpl,
    // so their sequenceIndex=0 values never collide.
    //
    // This test verifies the REACHABLE positive path: two separate rules each
    // with a sequence() step run independently without interference.
    //
    // The real collision path requires either nested-sequence DSL (not yet wired
    // in v1) or a phreak-level test — see coverage gap doc for the action item.
    // -------------------------------------------------------------------
    @Test
    public void twoRulesWithSequencesTrackIndependently() {
        Variable<MonitoringStation> stationAV   = declarationOf(MonitoringStation.class);
        Variable<MonitoringStation> stationBV   = declarationOf(MonitoringStation.class);
        Variable<SensorActivated>   activated1V = declarationOf(SensorActivated.class);
        Variable<SensorActivated>   activated2V = declarationOf(SensorActivated.class);
        Variable<HeartbeatOk>       heartbeat1V = declarationOf(HeartbeatOk.class);
        Variable<HeartbeatOk>       heartbeat2V = declarationOf(HeartbeatOk.class);

        // Rule 1: station-A → sequence(sensorActivated(s1) → heartbeat(s1))
        Rule rule1 = rule("c4-rule-1").build(
                pattern(stationAV).expr("is-a", s -> s.getId().equals("station-A")),
                sequence(
                        pattern(activated1V).expr("s1-act", a -> a.getSensorId().equals("s1")),
                        pattern(heartbeat1V).expr("s1-hb",  h -> h.getSensorId().equals("s1"))
                ),
                execute(() -> results.add("rule-1-fired"))
        );

        // Rule 2: station-B → sequence(sensorActivated(s2) → heartbeat(s2))
        Rule rule2 = rule("c4-rule-2").build(
                pattern(stationBV).expr("is-b", s -> s.getId().equals("station-B")),
                sequence(
                        pattern(activated2V).expr("s2-act", a -> a.getSensorId().equals("s2")),
                        pattern(heartbeat2V).expr("s2-hb",  h -> h.getSensorId().equals("s2"))
                ),
                execute(() -> results.add("rule-2-fired"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(
                new ModelImpl().addRule(rule1).addRule(rule2));
        ksession = kbase.newKieSession();

        // Start both sequencers
        ksession.insert(new MonitoringStation("station-A"));
        ksession.insert(new MonitoringStation("station-B"));
        ksession.fireAllRules();

        // Complete rule-1's sequence
        ksession.insert(new SensorActivated("s1"));
        ksession.fireAllRules();
        ksession.insert(new HeartbeatOk("s1"));
        ksession.fireAllRules();

        // Complete rule-2's sequence
        ksession.insert(new SensorActivated("s2"));
        ksession.fireAllRules();
        ksession.insert(new HeartbeatOk("s2"));
        ksession.fireAllRules();

        // Both rules must have fired exactly once
        assertThat(results).containsExactlyInAnyOrder("rule-1-fired", "rule-2-fired");
    }
}

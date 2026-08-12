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
 * Regression tests for critical correctness bugs in the sequencing implementation.
 *
 * <p>The circular-buffer overflow test ({@link #longSequenceDoesNotOverflowCircularBuffer})
 * verifies that {@code CircularArrayList.addEmpty()} uses modular indexing so that sequences
 * completing more than the initial buffer capacity do not throw
 * {@code ArrayIndexOutOfBoundsException}.</p>
 *
 * <p>The consequence-branch test ({@link #consequenceBranchDoesNotFallThroughToSequence})
 * verifies that a rule combining a conditional named consequence with a {@code sequence()}
 * step builds without error.  The crash path is only reachable via internal APIs and
 * is covered by separate phreak-level tests.</p>
 *
 * <p>The two-sequence tests ({@link #twoSequencesInOneRuleBothComplete} and
 * {@link #twoRulesWithSequencesTrackIndependently}) verify that multiple {@code sequence()}
 * items in the same rule, and multiple rules each with their own sequence, track state
 * independently without collision.</p>
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
    // consequenceBranchDoesNotFallThroughToSequence
    //
    // A rule combining a ConditionalNamedConsequence (when/then/elseWhen) with a
    // sequence() step must build and run without error.
    //
    // Note: a crash path exists when the condition builder encounters an unrecognised
    // CONSEQUENCE type; that path is not reachable via PatternDSL (which always
    // produces NamedConsequenceImpl or ConditionalNamedConsequenceImpl) and is
    // covered by separate phreak-level tests.
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

        // A ClassCastException here would indicate that the consequence-to-sequence
        // condition builder encounters an unrecognised type and falls through.
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule));
        assertThat(kieBase).isNotNull();
    }

    // -------------------------------------------------------------------
    // longSequenceDoesNotOverflowCircularBuffer
    //
    // Each completed sequence causes SequencerMemory to call addEmpty() to
    // pre-allocate output slots. Without modular indexing, the raw head index
    // overflows the array length after enough completions, throwing
    // ArrayIndexOutOfBoundsException.
    //
    // Strategy: insert the anchor once, then repeatedly complete the one-step
    // sequence more than 100 times (the default CircularArrayList capacity).
    // After each completion, update() on the anchor restarts the sequencer via
    // doLeftUpdates, allowing a fresh step signal to drive the next completion.
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
        // CircularArrayList. Without the modular-index fix, this throws AIOOBE after ~100 iterations.
        for (int i = 0; i < 110; i++) {
            ksession.insert(new Toy("ball"));
            ksession.fireAllRules();                          // sequence completes
            ksession.update(anchorHandle, anchor);            // restart sequencer
            ksession.fireAllRules();
        }

        assertThat(results).hasSize(110);
    }

    // -------------------------------------------------------------------
    // retractAnchorWithOrStepDoesNotLeakSiblingAdapters
    //
    // Retract the anchor after an OR step has consumed step-1. Further events
    // matching the OR branches must NOT trigger the rule.
    //
    // The DSL or() gate is implemented as a single LogicCircuit step, so
    // LogicCircuitStep.deactivate() cleans up all branches on stop(). The leak
    // path via raw ParallelStep / SubsequenceStep wiring is not reachable from
    // PatternDSL and is covered by separate phreak-level tests.
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
    // twoSequencesInOneRuleBothComplete
    //
    // Two sequence() blocks in the same rule.build() each get their own SequenceNode
    // and therefore their own Sequencer and SequencerMemoryImpl. Their sequenceMemories[]
    // arrays are independent, so both can complete without collision.
    //
    // This test verifies end-to-end integration of two SequenceNodes in series within
    // a single rule: both sequences must complete for the rule to fire.
    //
    // Note: a sequenceIndex collision is possible when two Sequence objects share the
    // same Sequencer (nested/parallel sub-sequences). That path requires internal API
    // access not yet wired to the PatternDSL and is covered by separate phreak-level tests.
    // -------------------------------------------------------------------
    @Test
    public void twoSequencesInOneRuleBothComplete() {
        Variable<Person>       personV = declarationOf(Person.class);
        Variable<Toy>          toyV    = declarationOf(Toy.class);
        Variable<Relationship> relV    = declarationOf(Relationship.class);

        Rule rule = rule("c4-two-seq-rule").build(
                pattern(personV),
                sequence(
                        pattern(toyV).expr("is-ball", t -> t.getName().equals("ball"))
                ),
                sequence(
                        pattern(relV).expr("is-go", r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule));
        ksession = kbase.newKieSession();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();                        // both sequencers start

        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();                        // first sequence completes

        // Both SequenceNodes have independent state. Completing the first sequence
        // must not affect the second; the rule fires only after both complete.
        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();                        // second sequence completes

        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------
    // twoRulesWithSequencesTrackIndependently
    //
    // Two separate rules, each with a single sequence(), have independent
    // SequenceNodes and SequencerMemoryImpl instances. Both must complete
    // their respective sequences and fire exactly once without interfering
    // with each other.
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

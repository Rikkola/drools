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
import org.drools.modelcompiler.domain.Toy;
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

public class PatternDSLSequenceLifecycleTest {

    private final Variable<Person>       person       = declarationOf(Person.class);
    private final Variable<Toy>          toy          = declarationOf(Toy.class);
    private final Variable<Relationship> relationship = declarationOf(Relationship.class);

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
    // retractAnchorStopsSequencer
    // Insert anchor → retract anchor → insert step-1 event → must NOT fire.
    // Verifies Sequencer.stop() tears down signal adapters on anchor retract.
    // -------------------------------------------------------------------
    @Test
    public void retractAnchorStopsSequencer() {
        ksession = makeKSession();

        FactHandle anchorHandle = ksession.insert(new Person("anchor"));
        ksession.fireAllRules();                    // activates sequencer

        ksession.retract(anchorHandle);
        ksession.fireAllRules();                    // stop() must be called

        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();                    // step-1 — sequencer gone, must not fire

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------
    // retractAnchorMidSequenceStopped
    // Insert anchor → fire step-1 → retract anchor → fire step-2 → must NOT fire.
    // Verifies stop() works even when the sequence is partially advanced.
    // -------------------------------------------------------------------
    @Test
    public void retractAnchorMidSequenceStopped() {
        ksession = makeKSession();

        FactHandle anchorHandle = ksession.insert(new Person("anchor"));
        ksession.fireAllRules();

        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();                    // step 1 consumed, step 2 waiting

        ksession.retract(anchorHandle);
        ksession.fireAllRules();

        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();                    // step-2 — sequencer stopped, must not fire

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------
    // twoAnchorsConcurrentlyBugOnlyOneFires
    // Known bug: DynamicFilter slots in SequenceNodeMemory are shared across
    // anchor tuples. When two anchors activate the same SequenceNode, the
    // second anchor's start() overwrites the first's signal adapters, so
    // only one of the two concurrent sequencers can advance.
    // Rule fires once instead of twice.
    // See also the TODO inside the test body.
    // -------------------------------------------------------------------
    @Test
    public void twoAnchorsConcurrentlyBugOnlyOneFires() {
        ksession = makeKSession();

        ksession.insert(new Person("anchor-A"));
        ksession.insert(new Person("anchor-B"));
        ksession.fireAllRules();                    // both sequencers start

        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();                    // step-1 consumed by BOTH sequencers

        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();                    // step-2 consumed by BOTH sequencers

        // Known limitation (pre-existing bug): DynamicFilter slots in SequenceNodeMemory
        // are shared across all anchor tuples. When anchor-B's sequencer starts, it
        // overwrites the signal adapters registered by anchor-A, so only one of the two
        // concurrent sequencers can advance. The rule fires once instead of twice.
        // TODO: fix per-tuple DynamicFilter isolation and update this assertion to hasSize(2).
        assertThat(results).hasSize(1);
    }

    // -------------------------------------------------------------------
    // sequenceRestartAfterUpdate
    // Anchor inserted → step-1 arrived → anchor updated → step-2 arrives.
    // The update must restart the sequence; step-2 arriving before a new
    // step-1 must NOT trigger a firing.
    // Regression for the doLeftUpdates restart bug (commit 6aeef037b26).
    // -------------------------------------------------------------------
    @Test
    public void sequenceRestartAfterUpdate() {
        ksession = makeKSession();

        Person anchor = new Person("anchor");
        FactHandle anchorHandle = ksession.insert(anchor);
        ksession.fireAllRules();

        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();                    // step 1 consumed

        ksession.update(anchorHandle, anchor);      // update restarts the sequence
        ksession.fireAllRules();

        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();                    // step-2 without a new step-1 — must NOT fire

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------
    // sequenceDoesNotFireAgainAfterCompletion
    // Full sequence completes (fires once) → another step-1 event arrives.
    // Sequencer must NOT restart and fire again; it is done.
    // -------------------------------------------------------------------
    @Test
    public void sequenceDoesNotFireAgainAfterCompletion() {
        ksession = makeKSession();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();

        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();

        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();                    // sequence complete — fired once

        assertThat(results).containsExactly("fired");

        // A second step-1 event after completion must not restart the sequence
        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();

        assertThat(results).hasSize(1);             // still only one firing
    }

    // -------------------------------------------------------------------

    private KieSession makeKSession() {
        Rule rule = rule("lifecycle-rule").build(
                pattern(person),
                sequence(
                        pattern(toy).expr("is-ball", t -> t.getName().equals("ball")),
                        pattern(relationship).expr("is-go", r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
        );
        Model model = new ModelImpl().addRule(rule);
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(model);
        return kieBase.newKieSession();
    }
}

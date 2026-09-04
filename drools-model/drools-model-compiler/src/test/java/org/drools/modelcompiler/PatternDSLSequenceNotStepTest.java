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
import org.drools.modelcompiler.domain.Toy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.DSL.not;
import static org.drools.model.PatternDSL.on;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;

/**
 * Tests for not() absence step inside sequence().
 *
 * Design contract: a not(P) step fires when it is activated (prior step
 * completed) AND no fact currently in the session matches P.  This is
 * point-in-time absence at activation — not a window.
 */
public class PatternDSLSequenceNotStepTest {

    // Domain types:
    //   Person  (name, age) — used as anchor or step match
    //   Toy     (name)      — used for positive steps
    //   Integer             — used as the not() target (negative integer == blocker)

    private final Variable<Person>  person  = declarationOf(Person.class);
    private final Variable<Toy>     toy     = declarationOf(Toy.class);
    private final Variable<Integer> number  = declarationOf(Integer.class);
    private final List<String>      results = new ArrayList<>();
    private KieSession              ksession;

    // -----------------------------------------------------------------
    // Rule: anchor = Person, step1 = Toy("ball"), not(Integer < 0), step3 = Toy("bat")
    // -----------------------------------------------------------------

    private Rule buildRule() {
        return rule("not-step")
                .build(
                    pattern(person),
                    sequence(
                        pattern(toy).expr("isBall", t -> t.getName().equals("ball")),
                        not(pattern(number).expr("isNeg", n -> n < 0)),
                        pattern(toy).expr("isBat", t -> t.getName().equals("bat"))
                    ),
                    execute(() -> results.add("fired"))
                );
    }

    @Test
    public void sequenceFiresWhenNotStepHasNoMatch() {
        // sequence: ball → not(negative int) → bat
        // When: ball inserted, no negative integer present, bat inserted → should fire
        ksession = makeKSession(buildRule());

        insertAndFire(new Person("anchor"));

        insertAndFire(new Toy("ball"));        // step 1 complete
        // no negative integer — not() step advances immediately
        insertAndFire(new Toy("bat"));         // step 3 complete

        assertThat(results).containsExactly("fired");
    }

    @Test
    public void sequenceDoesNotFireWhenNotStepHasMatch() {
        // When a negative integer is already present when step 2 (not) activates,
        // the step should block and the rule must NOT fire.
        ksession = makeKSession(buildRule());

        insertAndFire(new Person("anchor"));

        insertAndFire(new Toy("ball"), (Integer)(-5));  // step 1 complete; -5 is already in WM
        // not() step activates: -5 matches → step blocks
        insertAndFire(new Toy("bat"));                    // step 3 never activates

        assertThat(results).isEmpty();
    }

    @Test
    public void notStepAsFirstStepFiresWhenNoBlockerExists() {
        Rule rule = rule("not-first-step").build(
                pattern(person),
                sequence(
                        not(pattern(number).expr("isNeg", n -> n < 0)),
                        pattern(toy).expr("isBat", t -> t.getName().equals("bat"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = makeKSession(rule);
        insertAndFire(new Person("anchor"));
        insertAndFire(new Toy("bat"));

        assertThat(results).containsExactly("fired");
    }

    @Test
    public void notStepAsLastStepFiresWhenNoBlockerExists() {
        Rule rule = rule("not-last-step").build(
                pattern(person),
                sequence(
                        pattern(toy).expr("isBall", t -> t.getName().equals("ball")),
                        not(pattern(number).expr("isNeg", n -> n < 0))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = makeKSession(rule);
        insertAndFire(new Person("anchor"));
        insertAndFire(new Toy("ball"));

        assertThat(results).containsExactly("fired");
    }

    @Test
    public void standaloneNotStepFiresWhenNoBlockerExists() {
        Rule rule = rule("not-standalone-step").build(
                pattern(person),
                sequence(not(pattern(number).expr("isNeg", n -> n < 0))),
                execute(() -> results.add("fired"))
        );

        ksession = makeKSession(rule);
        insertAndFire(new Person("anchor"));

        assertThat(results).containsExactly("fired");
    }

    @Test
    public void notStepPatternVariableConstraintWorks() {
        Variable<Toy> notToyV = declarationOf(Toy.class);

        Rule rule = rule("not-step-pattern-var").build(
                pattern(person),
                sequence(
                        pattern(toy).expr("isBall", t -> t.getName().equals("ball")),
                        not(pattern(notToyV).expr("isBlocker", t -> t.getName().equals("blocker"))),
                        pattern(toy).expr("isBat", t -> t.getName().equals("bat"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = makeKSession(rule);
        insertAndFire(new Person("anchor"));
        insertAndFire(new Toy("ball"));
        insertAndFire(new Toy("bat"));
        assertThat(results).containsExactly("fired");

        results.clear();
        ksession.dispose();

        ksession = makeKSession(rule);
        insertAndFire(new Person("anchor"));
        insertAndFire(new Toy("ball"), new Toy("blocker"));
        insertAndFire(new Toy("bat"));
        assertThat(results).isEmpty();
    }

    @Test
    public void notStepMultiConstraintPatternUsesCombinedAlphaConstraint() {
        Variable<Toy> notToyV = declarationOf(Toy.class);

        Rule rule = rule("not-step-multi-constraint").build(
                pattern(person),
                sequence(
                        pattern(toy).expr("isBall", t -> t.getName().equals("ball")),
                        not(pattern(notToyV)
                                .expr("hasName",   t -> t.getName() != null)
                                .expr("isBlocker", t -> t.getName().equals("blocker"))),
                        pattern(toy).expr("isBat", t -> t.getName().equals("bat"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = makeKSession(rule);
        insertAndFire(new Person("anchor"));
        insertAndFire(new Toy("ball"));
        insertAndFire(new Toy("bat"));
        assertThat(results).containsExactly("fired");

        results.clear();
        ksession.dispose();

        ksession = makeKSession(rule);
        insertAndFire(new Person("anchor"));
        insertAndFire(new Toy("ball"), new Toy("blocker"));
        insertAndFire(new Toy("bat"));
        assertThat(results).isEmpty();
    }

    @Test
    public void notStepCrossVariableConstraintIsBlockedOnlyWhenNameMatches() {
        Variable<Person> personV  = declarationOf(Person.class);
        Variable<Toy>    stepToyV = declarationOf(Toy.class);
        Variable<Toy>    notToyV  = declarationOf(Toy.class);

        Rule rule = rule("not-step-cross-var").build(
                pattern(personV),
                sequence(
                        pattern(stepToyV).expr("isBall", t -> t.getName().equals("ball")),
                        not(pattern(notToyV).expr("nameMatchesAnchor",
                                personV, (t, p) -> t.getName().equals(p.getName()))),
                        pattern(stepToyV).expr("isBat", t -> t.getName().equals("bat"))
                ),
                on(personV).execute(p -> results.add("fired:" + p.getName()))
        );

        ksession = makeKSession(rule);
        insertAndFire(new Person("alice"));
        insertAndFire(new Toy("ball"), new Toy("bob"));
        insertAndFire(new Toy("bat"));
        assertThat(results).containsExactly("fired:alice");
        ksession.dispose();
        results.clear();

        ksession = makeKSession(rule);
        insertAndFire(new Person("alice"));
        insertAndFire(new Toy("ball"), new Toy("alice"));
        insertAndFire(new Toy("bat"));
        assertThat(results).isEmpty();
    }

    @AfterEach
    public void tearDown() {
        results.clear();
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

    private KieSession makeKSession(Rule rule) {
        final Model model = new ModelImpl().addRule(rule);
        final KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(model);
        return kieBase.newKieSession();
    }
}

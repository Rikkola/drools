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
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;

/**
 * Regression probes for bugs identified in the 2026-07-30 PR review.
 * Each test is named after the finding it covers.
 */
public class SequenceReviewProbeTest {

    private final List<String> results = new ArrayList<>();
    private KieSession ksession;

    @AfterEach
    public void tearDown() {
        results.clear();
        if (ksession != null) {
            ksession.dispose();
        }
    }

    // -------------------------------------------------------------------------
    // C1 — Adding a sequencing rule silently breaks unrelated rules on the same
    //       fact type.
    //
    // A plain rule on Toy fires when a Toy("ball") is inserted.
    // A sequencing rule also uses Toy as its step type.
    // The plain rule must still fire after both rules are in the same KieBase.
    // -------------------------------------------------------------------------
    @Test
    public void plainRuleAndSequenceRuleShareAnEventType() {
        Variable<Toy> toy = declarationOf(Toy.class);
        Variable<Person> person = declarationOf(Person.class);

        Rule plainRule = rule("plain-rule").build(
                pattern(toy).expr("is-ball", t -> t.getName().equals("ball")),
                execute(() -> results.add("plain:ball"))
        );

        Rule seqRule = rule("seq-rule").build(
                pattern(person),
                sequence(
                        pattern(toy).expr("seq-is-ball", t -> t.getName().equals("ball"))
                ),
                execute(() -> results.add("seq:ball"))
        );

        Model model = new ModelImpl().addRule(plainRule).addRule(seqRule);
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(model);
        ksession = kieBase.newKieSession();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();

        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();

        // The plain rule MUST fire. Before the C1 fix this list is empty.
        assertThat(results).contains("plain:ball");
    }

    // -------------------------------------------------------------------------
    // C2 — Any sequence reusing an event type in two steps crashes with AIOOBE.
    //
    // "ball then doll" — both are Toy — is the most ordinary sequencing shape.
    // activeFilters is sized by adapter count (distinct types = 1) but indexed
    // by filter/pattern index (0 for step 0, 1 for step 1), so step 1 blows up.
    // -------------------------------------------------------------------------
    @Test
    public void sameTypeInTwoSteps() {
        Variable<Toy>    toy    = declarationOf(Toy.class);
        Variable<Person> person = declarationOf(Person.class);

        Rule rule = rule("ball-then-doll").build(
                pattern(person),
                sequence(
                        pattern(toy).expr("is-ball", t -> t.getName().equals("ball")),
                        pattern(toy).expr("is-doll", t -> t.getName().equals("doll"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(
                new org.drools.model.impl.ModelImpl().addRule(rule))
                .newKieSession();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();

        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();

        ksession.insert(new Toy("doll"));
        ksession.fireAllRules();

        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------------
    // C3 — All but the first constraint on a step are silently dropped.
    //
    // PhreakNodeFactory.buildSequenceNode calls patterns[i].getConstraints().get(0),
    // discarding constraints 1..n. A step with two mutually exclusive exprs
    // (is-ball AND is-doll) is logically unsatisfiable — it must never fire.
    // Before the fix it fires because only "is-ball" is evaluated.
    // -------------------------------------------------------------------------
    @Test
    public void stepWithTwoConstraintsIsUnsatisfiable() {
        Variable<Toy>    toy    = declarationOf(Toy.class);
        Variable<Person> person = declarationOf(Person.class);

        // A step that requires name == "ball" AND name == "doll" simultaneously —
        // logically impossible, must never match any real Toy.
        Rule rule = rule("impossible-step").build(
                pattern(person),
                sequence(
                        pattern(toy)
                                .expr("is-ball", t -> t.getName().equals("ball"))
                                .expr("is-doll", t -> t.getName().equals("doll"))
                ),
                execute(() -> results.add("fired"))
        );

        ksession = KieBaseBuilder.createKieBaseFromModel(
                new org.drools.model.impl.ModelImpl().addRule(rule))
                .newKieSession();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();

        // A Toy("ball") satisfies "is-ball" but not "is-doll" — must NOT fire.
        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // C4 — A step with no constraint crashes KieBase construction.
    //
    // Same get(0) on Collections.EMPTY_LIST causes IndexOutOfBoundsException
    // at build time. pattern(toy) with no expr() is legal DSL.
    // -------------------------------------------------------------------------
    @Test
    public void stepWithNoConstraintThrowsDescriptiveError() {
        Variable<Toy>    toy    = declarationOf(Toy.class);
        Variable<Person> person = declarationOf(Person.class);

        Rule rule = rule("unconstrained-step").build(
                pattern(person),
                sequence(
                        pattern(toy)   // no expr — legal DSL, must not blow up
                ),
                execute(() -> results.add("fired"))
        );

        // Before the fix: IndexOutOfBoundsException: Index: 0 from Collections$EmptyList.get(0)
        // After the fix: IllegalArgumentException with a descriptive message.
        assertThat(org.assertj.core.api.Assertions.catchThrowableOfType(() ->
                KieBaseBuilder.createKieBaseFromModel(
                        new org.drools.model.impl.ModelImpl().addRule(rule)),
                Exception.class))
                .as("step with no constraint must throw a descriptive build error")
                .isNotNull()
                .isNotInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("sequence step");
    }

}

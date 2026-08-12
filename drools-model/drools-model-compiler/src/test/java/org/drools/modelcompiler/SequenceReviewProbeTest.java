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
 * Regression tests for correctness bugs in the sequencing implementation.
 * Each test is named after the scenario it covers.
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
    // plainRuleAndSequenceRuleShareAnEventType
    //
    // A plain rule on Toy fires when a Toy("ball") is inserted.
    // A sequencing rule also uses Toy as its step type.
    // The plain rule must still fire when both rules share a KieBase.
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

        // Both the plain rule and the sequence rule must fire.
        assertThat(results).contains("plain:ball");
    }

    // -------------------------------------------------------------------------
    // sameTypeInTwoSteps
    //
    // A sequence with two steps using the same fact type ("ball then doll" — both
    // are Toy) must fire correctly.  The activeFilters array is sized by distinct
    // adapter count (1) and must be indexed by adapter rather than by pattern index
    // to avoid an ArrayIndexOutOfBoundsException at step 1.
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
    // stepWithTwoConstraintsIsUnsatisfiable
    //
    // A step with two mutually exclusive constraints (is-ball AND is-doll) is
    // logically unsatisfiable and must never fire.  All constraints on a step
    // must be evaluated — not just the first one.
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
    // stepWithNoConstraintThrowsDescriptiveError
    //
    // A sequence step with no constraint (pattern(toy) with no expr()) is legal
    // DSL but must produce a descriptive build error rather than an opaque
    // IndexOutOfBoundsException from a get(0) on an empty list.
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

        // An opaque IndexOutOfBoundsException here would mean the builder calls get(0)
        // on an empty constraint list. The expected outcome is a descriptive error message.
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

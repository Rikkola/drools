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
import java.util.concurrent.TimeUnit;

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
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.time.SessionPseudoClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;
import static org.drools.model.PatternDSL.within;

/**
 * Compiler-layer tests for the whole-sequence {@code .completeWithin(Duration)} decorator.
 * Uses a pseudo clock so time can be advanced deterministically.
 */
public class PatternDSLSequenceCompleteWithinTest {

    private final Variable<Person>       person = declarationOf(Person.class);
    private final Variable<Toy>          toy    = declarationOf(Toy.class);
    private final Variable<Relationship> rel    = declarationOf(Relationship.class);

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

    // -------------------------------------------------------------------------
    // Happy path: sequence completes before the global deadline
    // -------------------------------------------------------------------------

    @Test
    public void sequenceCompletesBeforeDeadlineFires() {
        ksession = makeKSession(Duration.ofSeconds(10));
        SessionPseudoClock clock = ksession.getSessionClock();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();                  // activates sequencer

        clock.advanceTime(2, TimeUnit.SECONDS);

        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();                  // step 0 matches

        clock.advanceTime(3, TimeUnit.SECONDS);   // total 5 s — still within 10 s deadline

        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();                  // step 1 matches → sequence complete → rule fires

        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------------
    // Timeout: global deadline fires before sequence completes → no consequence
    // -------------------------------------------------------------------------

    @Test
    public void globalDeadlineExpiresBeforeCompletionNoFire() {
        ksession = makeKSession(Duration.ofSeconds(5));
        SessionPseudoClock clock = ksession.getSessionClock();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();                  // activates sequencer

        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();                  // step 0 matches; step 1 still waiting

        // Advance past the whole-sequence deadline without inserting the Relationship
        clock.advanceTime(6, TimeUnit.SECONDS);
        ksession.fireAllRules();                  // deadline timer fires → sequence fails

        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();                  // too late — sequence already failed

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // DSL validation: zero / negative duration rejected at model-build time
    // -------------------------------------------------------------------------

    @Test
    public void completeWithinRejectsZeroDuration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> sequence(pattern(toy)).completeWithin(Duration.ZERO))
                .withMessageContaining("positive");
    }

    @Test
    public void completeWithinRejectsNegativeDuration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> sequence(pattern(toy)).completeWithin(Duration.ofSeconds(-1)))
                .withMessageContaining("positive");
    }

    @Test
    public void completeWithinRejectsDuplicateCall() {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                        sequence(pattern(toy))
                                .completeWithin(Duration.ofSeconds(5))
                                .completeWithin(Duration.ofSeconds(10))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already set");
    }

    // -------------------------------------------------------------------------
    // Compose: within + completeWithin — the earlier deadline decides the outcome
    // -------------------------------------------------------------------------

    @Test
    public void withinAndCompleteWithinComposedStepDeadlineWins() {
        // Step timeout = 3 s, global deadline = 10 s — step times out first
        ksession = makeKSessionWithStepTimeout(Duration.ofSeconds(3), Duration.ofSeconds(10));
        SessionPseudoClock clock = ksession.getSessionClock();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();

        // Advance past step-0 deadline (3 s) without inserting Toy
        clock.advanceTime(4, TimeUnit.SECONDS);
        ksession.fireAllRules();                 // step-0 timer fires → sequence fails

        ksession.insert(new Toy("ball"));
        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();

        assertThat(results).isEmpty();
    }

    @Test
    public void withinAndCompleteWithinComposedGlobalDeadlineWins() {
        // Step timeout = 10 s, global deadline = 3 s — global times out first
        ksession = makeKSessionWithStepTimeout(Duration.ofSeconds(10), Duration.ofSeconds(3));
        SessionPseudoClock clock = ksession.getSessionClock();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();

        // Advance past global deadline (3 s) without completing the sequence
        clock.advanceTime(4, TimeUnit.SECONDS);
        ksession.fireAllRules();                 // global-deadline timer fires → sequence fails

        ksession.insert(new Toy("ball"));
        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private KieSession makeKSession(Duration globalDeadline) {
        Rule r = rule("completewithin-rule").build(
                pattern(person),
                sequence(
                        pattern(toy).expr("is-ball", t -> t.getName().equals("ball")),
                        pattern(rel).expr("is-go",   rel -> rel.getStart().equals("go"))
                ).completeWithin(globalDeadline),
                execute(() -> results.add("fired"))
        );
        return buildSession(r);
    }

    private KieSession makeKSessionWithStepTimeout(Duration stepTimeout, Duration globalDeadline) {
        Rule r = rule("composed-rule").build(
                pattern(person),
                sequence(
                        within(stepTimeout, pattern(toy).expr("is-ball", t -> t.getName().equals("ball"))),
                        pattern(rel).expr("is-go", rel -> rel.getStart().equals("go"))
                ).completeWithin(globalDeadline),
                execute(() -> results.add("fired"))
        );
        return buildSession(r);
    }

    private KieSession buildSession(Rule r) {
        Model model = new ModelImpl().addRule(r);
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(model);
        KieSessionConfiguration conf = org.kie.api.KieServices.get().newKieSessionConfiguration();
        conf.setOption(ClockTypeOption.get("pseudo"));
        return kieBase.newKieSession(conf, null);
    }
}

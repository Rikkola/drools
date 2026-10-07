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
 * Compiler-layer tests for the per-step {@code within(Duration, step)} decorator.
 * Uses a pseudo clock so time can be advanced deterministically.
 */
public class PatternDSLSequenceWithinTest {

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
    // Happy path: step matches before its deadline
    // -------------------------------------------------------------------------

    @Test
    public void stepFiresBeforeDeadlineSequenceContinues() {
        ksession = makeKSession(Duration.ofSeconds(10));
        SessionPseudoClock clock = ksession.getSessionClock();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();                 // activates sequencer (step 0 active)

        ksession.insert(new Toy("ball"));
        ksession.fireAllRules();                 // step 0 matches — well before 10 s deadline

        clock.advanceTime(2, TimeUnit.SECONDS);  // still within deadline

        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();                 // step 1 matches — rule fires

        assertThat(results).containsExactly("fired");
    }

    // -------------------------------------------------------------------------
    // Timeout: step does NOT match before its deadline → sequence fails silently
    // -------------------------------------------------------------------------

    @Test
    public void stepDeadlineExpiresBeforeMatchNoFire() {
        ksession = makeKSession(Duration.ofSeconds(5));
        SessionPseudoClock clock = ksession.getSessionClock();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();                 // activates sequencer (step 0 active)

        // Advance past the per-step deadline without inserting the Toy
        clock.advanceTime(6, TimeUnit.SECONDS);
        ksession.fireAllRules();                 // timer job fires → sequence fails

        // Step 1 event arrives after timeout — sequence already failed, must not fire
        ksession.insert(new Toy("ball"));
        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // DSL validation: zero / negative durations rejected at model-build time
    // -------------------------------------------------------------------------

    @Test
    public void withinRejectsZeroDuration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> within(Duration.ZERO, pattern(toy)))
                .withMessageContaining("positive");
    }

    @Test
    public void withinRejectsNegativeDuration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> within(Duration.ofSeconds(-1), pattern(toy)))
                .withMessageContaining("positive");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private KieSession makeKSession(Duration stepTimeout) {
        Rule r = rule("within-rule").build(
                pattern(person),
                sequence(
                        within(stepTimeout, pattern(toy).expr("is-ball",  t -> t.getName().equals("ball"))),
                        pattern(rel).expr("is-go", rel -> rel.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
        );
        Model model = new ModelImpl().addRule(r);
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(model);
        KieSessionConfiguration conf = org.kie.api.KieServices.get().newKieSessionConfiguration();
        conf.setOption(ClockTypeOption.get("pseudo"));
        return kieBase.newKieSession(conf, null);
    }
}

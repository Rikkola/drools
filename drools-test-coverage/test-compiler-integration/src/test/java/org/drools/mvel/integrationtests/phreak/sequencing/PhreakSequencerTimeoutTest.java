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
package org.drools.mvel.integrationtests.phreak.sequencing;

import java.util.concurrent.TimeUnit;

import org.drools.base.rule.Pattern;
import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.Sequence.TimeoutController;
import org.drools.base.reteoo.sequencing.signalprocessors.Gates;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate.TimeoutTimer;
import org.drools.base.reteoo.sequencing.signalprocessors.TerminatingSignalProcessor;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.core.time.impl.DurationTimer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.time.SessionPseudoClock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runtime-layer tests for per-step {@code within} timeouts (wired as {@link TimeoutTimer})
 * and whole-sequence {@code completeWithin} deadlines (wired as {@link TimeoutController}).
 *
 * Builds a one-step sequence: a single AND gate consuming filter 0 (BEvent).
 * Time is controlled via the session's pseudo clock.
 */
public class PhreakSequencerTimeoutTest extends AbstractPhreakSequencerSubsequenceTest {

    /**
     * Per-step timeout gate (5 000 ms).
     * Layout: gate0 accepts bpattern (filter index 0), outputs to TerminatingSignalProcessor.
     * A TimeoutTimer is attached: if the step doesn't match within 5 s the sequence fails.
     */
    @BeforeEach
    public void setup() {
        initKBaseWithEmptyRule();

        LogicGate gate0 = new LogicGate(Gates::and, 0,
                                        new int[]{0}, // bpattern
                                        new int[]{0}, // signal adapter 0
                                        0);
        gate0.setOutput(TerminatingSignalProcessor.get());
        gate0.setPropagationTimer(new TimeoutTimer(gate0, new DurationTimer(5_000L)));

        LogicCircuit circuit = new LogicCircuit(gate0);

        seq0 = new Sequence(0, Step.of(circuit));
        seq0.setFilters(new Pattern[]{bpattern});
        rule.addSequence(seq0);
        kbase.addPackage(pkg);
    }

    // -------------------------------------------------------------------------
    // Per-step timeout: step matches before deadline — sequence terminates normally
    // -------------------------------------------------------------------------

    @Test
    public void stepMatchesBeforeDeadlineSequenceTerminates() {
        createSession();   // pseudo clock starts at 0 ms
        SessionPseudoClock clock = (SessionPseudoClock) session.getSessionClock();

        // Advance 2 s — still within the 5 s deadline
        clock.advanceTime(2_000, TimeUnit.MILLISECONDS);

        // Insert B("b") → gate0 fires → TerminatingSignalProcessor → step -1 (terminated)
        session.insert(new BEvent(0, "b"));
        session.fireAllRules();

        SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);
        assertThat(sequenceMemory.getStep()).isEqualTo(seq0.getSteps().length);  // normal termination
    }

    // -------------------------------------------------------------------------
    // Per-step timeout: deadline fires before match — sequence fails silently
    // -------------------------------------------------------------------------

    @Test
    public void stepDeadlineExpiresSequenceFails() {
        createSession();
        SessionPseudoClock clock = (SessionPseudoClock) session.getSessionClock();

        // Advance 6 s — past the 5 s per-step deadline without inserting B
        clock.advanceTime(6_000, TimeUnit.MILLISECONDS);
        session.fireAllRules();   // processes the pending timer action

        SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);
        // Sequence failed silently — step pointer is at steps.length (terminal)
        assertThat(sequenceMemory.getStep()).isEqualTo(seq0.getSteps().length);

        // A late B event must NOT advance or re-activate the sequence
        session.insert(new BEvent(0, "b"));
        session.fireAllRules();
        assertThat(sequenceMemory.getStep()).isEqualTo(seq0.getSteps().length);
    }

    // -------------------------------------------------------------------------
    // Whole-sequence deadline: sequence completes before the global deadline
    // -------------------------------------------------------------------------

    @Test
    public void sequenceCompletesBeforeGlobalDeadlineTerminates() {
        initKBaseWithEmptyRule();

        // Rebuild seq0 WITHOUT per-step timeout, but WITH a TimeoutController (global deadline = 10 s)
        LogicGate gate0 = new LogicGate(Gates::and, 0,
                                        new int[]{0},
                                        new int[]{0},
                                        0);
        gate0.setOutput(TerminatingSignalProcessor.get());

        LogicCircuit circuit = new LogicCircuit(gate0);

        seq0 = new Sequence(0, Step.of(circuit));
        seq0.setFilters(new Pattern[]{bpattern});
        seq0.setController(new TimeoutController(new DurationTimer(10_000L)));
        rule.addSequence(seq0);
        kbase.addPackage(pkg);

        createSession();
        SessionPseudoClock clock = (SessionPseudoClock) session.getSessionClock();

        // Advance 3 s — still within 10 s global deadline
        clock.advanceTime(3_000, TimeUnit.MILLISECONDS);

        // Insert B("b") → gate0 fires → terminates; deadline job must be cancelled
        session.insert(new BEvent(0, "b"));
        session.fireAllRules();

        SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);
        assertThat(sequenceMemory.getStep()).isEqualTo(seq0.getSteps().length);  // normal termination

        // Advance well past the original deadline — no spurious timeout should fire
        clock.advanceTime(15_000, TimeUnit.MILLISECONDS);
        session.fireAllRules();
        // Step unchanged — no secondary failure
        assertThat(sequenceMemory.getStep()).isEqualTo(seq0.getSteps().length);
    }

    // -------------------------------------------------------------------------
    // Whole-sequence deadline: deadline fires before sequence completes
    // -------------------------------------------------------------------------

    @Test
    public void globalDeadlineExpiresSequenceFails() {
        initKBaseWithEmptyRule();

        // Rebuild seq0 WITHOUT per-step timeout, WITH a TimeoutController (global deadline = 5 s)
        LogicGate gate0 = new LogicGate(Gates::and, 0,
                                        new int[]{0},
                                        new int[]{0},
                                        0);
        gate0.setOutput(TerminatingSignalProcessor.get());

        LogicCircuit circuit = new LogicCircuit(gate0);

        seq0 = new Sequence(0, Step.of(circuit));
        seq0.setFilters(new Pattern[]{bpattern});
        seq0.setController(new TimeoutController(new DurationTimer(5_000L)));
        rule.addSequence(seq0);
        kbase.addPackage(pkg);

        createSession();
        SessionPseudoClock clock = (SessionPseudoClock) session.getSessionClock();

        // Advance 6 s past global deadline without completing the sequence
        clock.advanceTime(6_000, TimeUnit.MILLISECONDS);
        session.fireAllRules();   // processes the pending timer action

        SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);
        assertThat(sequenceMemory.getStep()).isEqualTo(seq0.getSteps().length);  // failed silently

        // Late event must not re-advance
        session.insert(new BEvent(0, "b"));
        session.fireAllRules();
        assertThat(sequenceMemory.getStep()).isEqualTo(seq0.getSteps().length);
    }
}

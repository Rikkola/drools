/**
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

import org.drools.base.rule.Pattern;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.signalprocessors.SignalStatus;
import org.drools.core.common.InternalFactHandle;
import org.drools.base.reteoo.sequencing.signalprocessors.Gates;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate.DelayFromActivatedTimer;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate.DelayFromMatchTimer;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate.TimeoutTimer;
import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.base.reteoo.sequencing.signalprocessors.TerminatingSignalProcessor;
import org.drools.core.time.impl.DurationTimer;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.drools.mvel.integrationtests.phreak.B;
import org.drools.mvel.integrationtests.phreak.C;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class PhreakSequencerSignalProcessorTimerTest extends AbstractPhreakSequencerSubsequenceTest {


    @BeforeEach
    public void setup() {
        initKBaseWithEmptyRule();
    }

    @Test
    public void testTimeout() {
        LogicGate gate1 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask),0,
                                        new int[] {0, 1}, // B and C
                                        new int[] {0, 1}, // Each SignalAdapter must be in a unique index  for the Sequence
                                        0);

        gate1.setPropagationTimer(new TimeoutTimer(gate1, new DurationTimer(1000)));

        gate1.setOutput(TerminatingSignalProcessor.get());

        LogicCircuit circuit1 = new LogicCircuit(gate1);

        Sequence seq = new Sequence(0, Step.of(circuit1));
        seq.setFilters(new Pattern[]{bpattern, cpattern});
        rule.addSequence(seq);
        kbase.addPackage(pkg);

        createSession();
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(0); // step 0
        InternalFactHandle   fhB0   = (InternalFactHandle) session.insert(new B(0, "b"));
        PseudoClockScheduler pseudo = (PseudoClockScheduler) session.getTimerService();
        assertThat(pseudo.getQueue().size()).isEqualTo(1);
        pseudo.advanceTime(2000, TimeUnit.MILLISECONDS);
        session.fireAllRules(); // if the rest of the system is immediate, why isn't this?
        assertThat(pseudo.getQueue().size()).isEqualTo(0);
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(0); // timed out → never advanced past step 0

        createSession();
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(0); // step 0
        fhB0   = (InternalFactHandle) session.insert(new B(0, "b"));
        pseudo = (PseudoClockScheduler) session.getTimerService();
        assertThat(pseudo.getQueue().size()).isEqualTo(1);

        InternalFactHandle fhC0   = (InternalFactHandle) session.insert(new C(0, "c"));
        pseudo = (PseudoClockScheduler) session.getTimerService();
        assertThat(pseudo.getQueue().peek().isCanceled()).isTrue(); // cancelled timers, stay on the queue until they fire (where they noop)
        pseudo.advanceTime(2000, TimeUnit.MILLISECONDS);
        session.fireAllRules(); // if the rest of the system is immediate, why isn't this?
    }

    @Test
    public void testDelayFromActivation() {
        LogicGate gate1 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask),0,
                                        new int[] {0, 1}, // B and C
                                        new int[] {0, 1}, // Each SignalAdapter must be in a unique index  for the Sequence
                                        0);

        gate1.setPropagationTimer(new DelayFromActivatedTimer(gate1, new DurationTimer(1000)));

        gate1.setOutput(TerminatingSignalProcessor.get());

        LogicCircuit circuit1 = new LogicCircuit(gate1);

        Sequence seq = new Sequence(0, Step.of(circuit1));
        seq.setFilters(new Pattern[]{bpattern, cpattern});
        rule.addSequence(seq);
        kbase.addPackage(pkg);

        createSession();

        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(0); // step 0
        InternalFactHandle   fhB0   = (InternalFactHandle) session.insert(new B(0, "b"));
        InternalFactHandle fhC0   = (InternalFactHandle) session.insert(new C(0, "c"));
        PseudoClockScheduler pseudo = (PseudoClockScheduler) session.getTimerService();
        assertThat(pseudo.getQueue().size()).isEqualTo(1);
        pseudo.advanceTime(2000, TimeUnit.MILLISECONDS);
        session.fireAllRules(); // if the rest of the system is immediate, why isn't this?
        assertThat(pseudo.getQueue().size()).isEqualTo(0);

        createSession();

        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(0); // step 0
        fhB0   = (InternalFactHandle) session.insert(new B(0, "b"));
        pseudo = (PseudoClockScheduler) session.getTimerService();
        assertThat(pseudo.getQueue().size()).isEqualTo(1);
        pseudo.advanceTime(2000, TimeUnit.MILLISECONDS);
        session.fireAllRules(); // if the rest of the system is immediate, why isn't this?
        assertThat(pseudo.getQueue().size()).isEqualTo(0);
    }

    @Test
    public void testDelayFromMatch() {
        LogicGate gate1 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask),0,
                                        new int[] {0, 1}, // B and C
                                        new int[] {0, 1}, // Each SignalAdapter must be in a unique index  for the Sequence
                                        0);

        gate1.setPropagationTimer(new DelayFromMatchTimer(gate1, new DurationTimer(1000)));

        gate1.setOutput(TerminatingSignalProcessor.get());

        LogicCircuit circuit1 = new LogicCircuit(gate1);

        Sequence seq = new Sequence(0, Step.of(circuit1));
        seq.setFilters(new Pattern[]{bpattern, cpattern});
        rule.addSequence(seq);
        kbase.addPackage(pkg);

        createSession();

        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(0); // step 0
        InternalFactHandle   fhB0   = (InternalFactHandle) session.insert(new B(0, "b"));
        PseudoClockScheduler pseudo = (PseudoClockScheduler) session.getTimerService();
        assertThat(pseudo.getQueue().size()).isEqualTo(0); // not created activation, only on match
        InternalFactHandle fhC0   = (InternalFactHandle) session.insert(new C(0, "c"));
        assertThat(pseudo.getQueue().size()).isEqualTo(1);
        pseudo.advanceTime(500, TimeUnit.MILLISECONDS);
        session.fireAllRules(); // if the rest of the system is immediate, why isn't this?
        assertThat(pseudo.getQueue().size()).isEqualTo(1); // still 1
        pseudo.advanceTime(1000, TimeUnit.MILLISECONDS);
        session.fireAllRules();
        assertThat(pseudo.getQueue().size()).isEqualTo(0);
    }

    /**
     * Verifies the post-Task-5 DELAY-branch polarity in LogicGateTimerAction:
     * when a DELAY job fires after the gate's status has reverted to non-MATCHED,
     * the action must be a no-op — neither gate.propagate() nor Sequence.fail() is called.
     *
     * This is the only end-to-end verification of that path because the DSL-level
     * retract path through AlphaAdapter is a no-op today (see IDEAS.md 2026-05-25).
     *
     * Two observable effects distinguish no-op from fail:
     *   - gate.propagate() would increment the step past 0; no-op leaves it at 0.
     *   - Sequence.fail() → stop() → gate.deactivate() nulls all activeSignalAdapters;
     *     no-op leaves them intact (gate keeps listening).
     */
    @Test
    public void testDelayBranchNoOpOnRevert() {
        LogicGate gate1 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask), 0,
                                        new int[] {0, 1}, // B and C
                                        new int[] {0, 1}, // Each SignalAdapter must be in a unique index for the Sequence
                                        0);

        gate1.setPropagationTimer(new DelayFromMatchTimer(gate1, new DurationTimer(1000)));
        gate1.setOutput(TerminatingSignalProcessor.get());

        LogicCircuit circuit1 = new LogicCircuit(gate1);

        Sequence seq = new Sequence(0, Step.of(circuit1));
        seq.setFilters(new Pattern[]{bpattern, cpattern});
        rule.addSequence(seq);
        kbase.addPackage(pkg);

        createSession();

        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(0); // step 0

        // Drive gate to MATCHED — this schedules the DELAY job.
        session.insert(new B(0, "b"));
        session.insert(new C(0, "c"));
        PseudoClockScheduler pseudo = (PseudoClockScheduler) session.getTimerService();
        assertThat(pseudo.getQueue().size()).isEqualTo(1); // DELAY job is queued

        // Revert the gate's status to UNMATCHED before the DELAY fires.
        // This simulates a retract that removes the MATCHED condition.
        // The job is still in the queue — it will fire, but status is no longer MATCHED.
        SequenceMemory seqMem = sequencerMemory.getChildSequenceMemory();
        assertThat(seqMem.getLogicGateSignalStatus(0)).isEqualTo(SignalStatus.MATCHED);
        seqMem.setLogicGateSignalStatus(0, SignalStatus.UNMATCHED);

        // Fire the DELAY job. With post-Task-5 code this is a no-op (status != MATCHED).
        pseudo.advanceTime(1500, TimeUnit.MILLISECONDS);
        session.fireAllRules();
        assertThat(pseudo.getQueue().size()).isEqualTo(0); // job fired and was consumed

        // gate.propagate() was NOT called: step did not advance past 0.
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(0);

        // Sequence.fail() → stop() → gate.deactivate() was NOT called:
        // the active signal adapters for B (index 0) and C (index 1) are still wired.
        // If fail had run, deactivateSignalAdapter() would have nulled them.
        assertThat(seqMem.getActiveSignalAdapters()[0]).isNotNull(); // B adapter still active
        assertThat(seqMem.getActiveSignalAdapters()[1]).isNotNull(); // C adapter still active
    }

}

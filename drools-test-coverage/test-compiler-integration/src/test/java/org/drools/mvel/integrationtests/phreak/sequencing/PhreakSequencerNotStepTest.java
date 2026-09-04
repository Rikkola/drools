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

import org.drools.base.rule.Pattern;
import org.drools.base.reteoo.SignalAdapter;
import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.signalprocessors.Gates;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.reteoo.sequencing.signalprocessors.TerminatingSignalProcessor;
import org.drools.base.reteoo.sequencing.signalprocessors.VetoSignalProcessor;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.mvel.integrationtests.phreak.B;
import org.drools.mvel.integrationtests.phreak.C;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runtime-layer tests for the continuous absence guard (ADR 0002).
 *
 * Builds a Sequence directly with the folded composite layout:
 *   - absenceLeaf gate: filterIndex=0 (bpattern), outputs to VetoSignalProcessor
 *   - positiveLeaf gate: filterIndex=1 (cpattern), outputs to TerminatingSignalProcessor
 *
 * Veto tests: insert B("b") → hits filter 0 → VetoSignalProcessor fires.
 * Advance test: insert C("c") → hits filter 1 → TerminatingSignalProcessor fires.
 */
public class PhreakSequencerNotStepTest extends AbstractPhreakSequencerSubsequenceTest {

    @BeforeEach
    public void setup() {
        initKBaseWithEmptyRule();

        // Absence leaf gate (filter 0 = bpattern): fires → VetoSignalProcessor
        LogicGate absenceLeaf = new LogicGate(Gates::and, 0,
                                              new int[]{0},  // filter index 0 (bpattern)
                                              new int[]{0},  // signal adapter index 0
                                              0);
        absenceLeaf.setOutput(VetoSignalProcessor.get());
        absenceLeaf.setVetoGate(true);

        // Positive leaf gate (filter 1 = cpattern): fires → TerminatingSignalProcessor
        LogicGate positiveLeaf = new LogicGate(Gates::and, 1,
                                               new int[]{1},  // filter index 1 (cpattern)
                                               new int[]{1},  // signal adapter index 1
                                               0);
        positiveLeaf.setOutput(TerminatingSignalProcessor.get());

        LogicCircuit circuit = new LogicCircuit(absenceLeaf, positiveLeaf);

        seq0 = new Sequence(0, Step.of(circuit));
        seq0.setFilters(new Pattern[]{bpattern, cpattern});
        rule.addSequence(seq0);
        kbase.addPackage(pkg);
    }

    @Test
    public void absenceGuardAdvancesWhenNoBlocker() {
        // No B inserted — positive C fires → TerminatingSignalProcessor → sequence terminates.
        createSession();
        SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);

        assertThat(sequenceMemory.isStepVetoed()).isFalse();

        // Insert C to fire positiveLeaf → TerminatingSignalProcessor → sequence terminates
        session.insert(new C(0, "c"));
        session.fireAllRules();

        assertThat(sequenceMemory.isStepVetoed()).isFalse();
        // -1 means the sequence has terminated (no active leaf sequences)
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1);
    }

    @Test
    public void absenceGuardVetoesOnLiveInsert() {
        // B inserted after activation → absence gate fires → veto flag set.
        createSession();
        SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);

        assertThat(sequenceMemory.isStepVetoed()).isFalse();

        // Insert B — the live signal adapter fires into VetoSignalProcessor.
        session.insert(new B(0, "b"));
        session.fireAllRules();

        assertThat(sequenceMemory.isStepVetoed()).isTrue();
        // Step did not advance to completion — sequence is still active (step 0), not terminated
        assertThat(getCurrentStep(sequencerMemory)).isNotEqualTo(-1);
    }

    @Test
    public void absenceGuardDeactivatesAdaptersOnVeto() {
        // After veto, all active signal adapters must be nulled out by deactivate.
        createSession();
        SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);

        session.insert(new B(0, "b"));
        session.fireAllRules();

        assertThat(sequenceMemory.isStepVetoed()).isTrue();
        // All active signal adapters should be null after veto-triggered deactivate.
        for (SignalAdapter adapter : sequenceMemory.getActiveSignalAdapters()) {
            assertThat(adapter).isNull();
        }
    }
}

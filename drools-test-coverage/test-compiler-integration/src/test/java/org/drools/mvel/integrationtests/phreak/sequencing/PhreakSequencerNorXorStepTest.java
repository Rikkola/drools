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

import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.signalprocessors.Gates;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.reteoo.sequencing.signalprocessors.TerminatingSignalProcessor;
import org.drools.base.reteoo.sequencing.signalprocessors.VetoSignalProcessor;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.base.rule.Pattern;
import org.drools.mvel.integrationtests.phreak.B;
import org.drools.mvel.integrationtests.phreak.C;
import org.drools.mvel.integrationtests.phreak.D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runtime-layer tests for multi-pattern NOR and XOR sequence steps.
 *
 * NOR multi-pattern: folded into N individual veto gates + positive terminal gate.
 * Any blocker inserted vetoes the sequence.
 *
 * XOR multi-pattern: runtime semantics deferred.
 */
public class PhreakSequencerNorXorStepTest extends AbstractPhreakSequencerSubsequenceTest {

    @BeforeEach
    public void setup() {
        initKBaseWithEmptyRule();

        // NOR blocker 1 (filter 0 = bpattern): fires -> VetoSignalProcessor
        LogicGate absenceLeaf1 = new LogicGate(Gates::and, 0,
                                               new int[]{0},  // filter index 0 (bpattern)
                                               new int[]{0},  // signal adapter index 0
                                               0);
        absenceLeaf1.setOutput(VetoSignalProcessor.get());
        absenceLeaf1.setVetoGate(true);

        // NOR blocker 2 (filter 1 = cpattern): fires -> VetoSignalProcessor
        LogicGate absenceLeaf2 = new LogicGate(Gates::and, 1,
                                               new int[]{1},  // filter index 1 (cpattern)
                                               new int[]{1},  // signal adapter index 1
                                               0);
        absenceLeaf2.setOutput(VetoSignalProcessor.get());
        absenceLeaf2.setVetoGate(true);

        // Positive leaf gate (filter 2 = dpattern): fires -> TerminatingSignalProcessor
        LogicGate positiveLeaf = new LogicGate(Gates::and, 2,
                                               new int[]{2},  // filter index 2 (dpattern)
                                               new int[]{2},  // signal adapter index 2
                                               0);
        positiveLeaf.setOutput(TerminatingSignalProcessor.get());

        LogicCircuit circuit = new LogicCircuit(absenceLeaf1, absenceLeaf2, positiveLeaf);

        seq0 = new Sequence(0, Step.of(circuit));
        seq0.setFilters(new Pattern[]{bpattern, cpattern, dpattern});
        rule.addSequence(seq0);
        kbase.addPackage(pkg);
    }

    @Test
    public void norTwoPatterns_neitherPresent_fires() {
        // Neither B nor C present; inserting positive D terminates the sequence successfully.
        createSession();
        SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);

        assertThat(sequenceMemory.isStepVetoed()).isFalse();

        session.insert(new D(0, "d"));
        session.fireAllRules();

        assertThat(sequenceMemory.isStepVetoed()).isFalse();
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1);
    }

    @Test
    public void norTwoPatterns_firstPatternVetoes() {
        // First blocker B inserted after activation -> hits absenceLeaf1 -> vetoes.
        createSession();
        SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);

        assertThat(sequenceMemory.isStepVetoed()).isFalse();

        session.insert(new B(0, "b"));
        session.fireAllRules();

        assertThat(sequenceMemory.isStepVetoed()).isTrue();
        assertThat(getCurrentStep(sequencerMemory)).isNotEqualTo(-1);
    }

    @Test
    public void norTwoPatterns_secondPatternPreexisting_vetoes() {
        // Second blocker C inserted -> hits absenceLeaf2 -> vetoes.
        createSession();
        SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);

        assertThat(sequenceMemory.isStepVetoed()).isFalse();

        session.insert(new C(0, "c"));
        session.fireAllRules();

        assertThat(sequenceMemory.isStepVetoed()).isTrue();
        assertThat(getCurrentStep(sequencerMemory)).isNotEqualTo(-1);
    }

    @Test
    @Disabled("xor() runtime semantics planned but not yet implemented — see plans/2026-09-05-nor-xor-multipattern.md")
    public void xorTwoPatterns_exactlyOne_fires() {
        // Planned: exactly one match in XOR step allows advance
    }

    @Test
    @Disabled("xor() runtime semantics planned but not yet implemented — see plans/2026-09-05-nor-xor-multipattern.md")
    public void xorTwoPatterns_none_doesNotFire() {
        // Planned: 0 matches in XOR step does not advance
    }

    @Test
    @Disabled("xor() runtime semantics planned but not yet implemented — see plans/2026-09-05-nor-xor-multipattern.md")
    public void xorTwoPatterns_both_doesNotFire() {
        // Planned: 2 matches in XOR step reverts gate and does not advance
    }
}

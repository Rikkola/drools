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
import org.drools.core.common.InternalFactHandle;
import org.drools.base.reteoo.sequencing.signalprocessors.Gates;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.signalprocessors.TerminatingSignalProcessor;
import org.drools.mvel.integrationtests.phreak.B;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PhreakSequencerSubsequenceTest extends AbstractPhreakSequencerSubsequenceTest {

    @BeforeEach
    public void setup() {
        initKBaseWithEmptyRule();

        LogicGate gate1 = get1InputLogicGate();
        gate1.setOutput(TerminatingSignalProcessor.get());
        LogicCircuit circuit1 = new LogicCircuit(gate1);

        LogicGate gate2 = get1InputLogicGate();
        gate2.setOutput(TerminatingSignalProcessor.get());
        LogicCircuit circuit2 = new LogicCircuit(gate2);

        LogicGate gate3 = get1InputLogicGate();
        gate3.setOutput(TerminatingSignalProcessor.get());
        LogicCircuit circuit3 = new LogicCircuit(gate3);

        LogicGate gate4 = get1InputLogicGate();
        gate4.setOutput(TerminatingSignalProcessor.get());
        LogicCircuit circuit4 = new LogicCircuit(gate4);

        seq1 = new Sequence(1, Step.of(circuit1), Step.of(circuit2));
        seq2 = new Sequence(2, Step.of(circuit3), Step.of(circuit4));

        seq0 = new Sequence(0, Step.of(seq1), Step.of(seq2));
        seq0.setFilters(new Pattern[]{bpattern});
        rule.addSequence(seq0);
        kbase.addPackage(pkg);

        createSession();
    }

    private static LogicGate get1InputLogicGate() {
        LogicGate gate1 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask), 0,
                                        new int[]{0}, // B
                                        new int[]{0}, //
                                        0);
        return gate1;
    }

    @Test
    public void testSubSequence() {
        // seq0 is at step 0 (Step.of(seq1)), seq1 is at step 0 (circuit1)
        SequenceMemory seq0Memory = sequencerMemory.getChildSequenceMemory();
        assertThat(seq0Memory.getSequence()).isSameAs(seq0);
        assertThat(seq0Memory.getStep()).isEqualTo(0); // seq0 blocked at sub-sequence step for seq1

        SequenceMemory seq1Memory = sequencerMemory.getSequenceMemory(seq1);
        assertThat(seq1Memory.getSequence()).isSameAs(seq1);
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(0); // leaf: seq1 step 0

        // B0 satisfies circuit1 in seq1 → seq1 advances to step 1 (circuit2)
        InternalFactHandle fhB0 = (InternalFactHandle) session.insert(new B(0, "b"));
        assertThat(seq0Memory.getStep()).isEqualTo(0); // seq0 still blocked at sub-sequence step for seq1
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(1); // leaf: seq1 step 1

        // B1 satisfies circuit2 in seq1 → seq1 completes → seq0 advances to step 1 (Step.of(seq2)) → seq2 starts at step 0
        InternalFactHandle fhB1 = (InternalFactHandle) session.insert(new B(0, "b"));
        assertThat(seq0Memory.getStep()).isEqualTo(1); // seq0 now at sub-sequence step for seq2

        SequenceMemory seq2Memory = sequencerMemory.getSequenceMemory(seq2);
        assertThat(seq2Memory.getSequence()).isSameAs(seq2);
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(0); // leaf: seq2 step 0

        // B2 satisfies circuit3 in seq2 → seq2 advances to step 1 (circuit4)
        InternalFactHandle fhB2 = (InternalFactHandle) session.insert(new B(0, "b"));
        assertThat(seq0Memory.getStep()).isEqualTo(1); // seq0 still blocked at sub-sequence step for seq2
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(1); // leaf: seq2 step 1

        // B3 satisfies circuit4 in seq2 → seq2 completes → seq0 completes → sequencer terminates
        InternalFactHandle fhB3 = (InternalFactHandle) session.insert(new B(0, "b"));
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1); // terminated
    }

}

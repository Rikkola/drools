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

import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.signalprocessors.Gates;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.reteoo.sequencing.signalprocessors.TerminatingSignalProcessor;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.base.rule.Pattern;
import org.drools.mvel.integrationtests.phreak.B;
import org.drools.mvel.integrationtests.phreak.C;
import org.drools.mvel.integrationtests.phreak.D;
import org.drools.mvel.integrationtests.phreak.E;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PhreakSequencerParallelTest extends AbstractPhreakSequencerSubsequenceTest {

    @BeforeEach
    public void setup() {
        initKBaseWithEmptyRule();

        // Branch 1: B then C
        LogicGate b1 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask), 0,
                                     new int[] {0}, new int[] {0}, 0); // B
        b1.setOutput(TerminatingSignalProcessor.get());
        LogicGate c1 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask), 1,
                                     new int[] {1}, new int[] {1}, 0); // C
        c1.setOutput(TerminatingSignalProcessor.getMatch());
        seq1 = new Sequence(1, Step.of(new LogicCircuit(b1)), Step.of(new LogicCircuit(c1)));
        seq1.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});
        seq1.setOutputSize(1);

        // Branch 2: B then D
        LogicGate b2 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask), 0,
                                     new int[] {0}, new int[] {0}, 0); // B, adapter slot 0
        b2.setOutput(TerminatingSignalProcessor.get());
        LogicGate d2 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask), 1,
                                     new int[] {2}, new int[] {1}, 0); // D (filter 2), adapter slot 1
        d2.setOutput(TerminatingSignalProcessor.getMatch());
        seq2 = new Sequence(2, Step.of(new LogicCircuit(b2)), Step.of(new LogicCircuit(d2)));
        seq2.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});
        seq2.setOutputSize(1);

        // Branch 3: B then E
        LogicGate b3 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask), 0,
                                     new int[] {0}, new int[] {0}, 0); // B, adapter slot 0
        b3.setOutput(TerminatingSignalProcessor.get());
        LogicGate e3 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask), 1,
                                     new int[] {3}, new int[] {1}, 0); // E (filter 3), adapter slot 1
        e3.setOutput(TerminatingSignalProcessor.getMatch());
        seq3 = new Sequence(3, Step.of(new LogicCircuit(b3)), Step.of(new LogicCircuit(e3)));
        seq3.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});
        seq3.setOutputSize(1);

        seq0 = new Sequence(0, Step.of(seq1, seq2, seq3));
        seq0.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});

        rule.addSequence(seq0);
        kbase.addPackage(pkg);
    }

    @Test
    public void testParallelOutputs() {
        createSession();

        SequenceMemory seq0Memory = sequencerMemory.getSequenceMemory(seq0);
        assertThat(seq0Memory.getStep()).isEqualTo(0);   // parent parked on the parallel step
        assertThat(seq0Memory.getCount()).isEqualTo(0);  // no branch completed yet

        // B advances all three branches from step 0 to step 1 (each branch's first gate listens on B)
        session.insert(new B(0, "b"));
        assertThat(sequencerMemory.getSequenceMemory(seq1).getStep()).isEqualTo(1);
        assertThat(sequencerMemory.getSequenceMemory(seq2).getStep()).isEqualTo(1);
        assertThat(sequencerMemory.getSequenceMemory(seq3).getStep()).isEqualTo(1);
        assertThat(seq0Memory.getCount()).isEqualTo(0);  // no branch has reached its terminal trigger

        // C completes branch 1 only
        session.insert(new C(0, "c"));
        assertThat(seq0Memory.getCount()).isEqualTo(1);
        assertThat(seq0Memory.getStep()).isEqualTo(0);   // parent still parked: branches 2 and 3 unfinished

        // D completes branch 2 only
        session.insert(new D(0, "d"));
        assertThat(seq0Memory.getCount()).isEqualTo(2);
        assertThat(seq0Memory.getStep()).isEqualTo(0);   // parent still parked: branch 3 unfinished

        // E completes branch 3 — the join fires and the parent (whose only step is the parallel step) terminates
        session.insert(new E(0, "e"));
        assertThat(seq0Memory.getCount()).isEqualTo(3);
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1); // terminated
    }

}

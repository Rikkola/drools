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
package org.drools.base.reteoo.sequencing;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.steps.ParallelStep;
import org.drools.base.reteoo.sequencing.steps.SubsequenceStep;
import org.drools.base.reteoo.sequencing.steps.Step;

import java.util.ArrayList;
import java.util.List;

public class Sequencer {

    private final Sequence     sequence;

    private final Sequence[] sequences;

    public Sequencer(Sequence sequence) {
        this.sequence = sequence;
        this.sequences = populateSequences(sequence, new ArrayList<>()).stream().toArray(Sequence[]::new);
    }

    public static List<Sequence> populateSequences(Sequence sequence, List<Sequence> list) {
        list.add(sequence);
        for (Step step  : sequence.getSteps()) {
            if (step instanceof SubsequenceStep) {
                populateSequences(((SubsequenceStep)step).getSubsequence(), list);
            } else if (step instanceof ParallelStep) {
                for (SubsequenceStep subseqStep : ((ParallelStep)step).getSubsequenceSteps()) {
                    populateSequences(subseqStep.getSubsequence(), list);
                }
            }
        }

        return list;
    }

    public Sequence[] getSequences() {
        return sequences;
    }

    public void start(SequencerMemory memory, ValueResolver valueResolver) {
        SequenceMemory sequenceMemory = memory.getOrCreateSequenceMemory(null, sequence, memory.getData());
        memory.setChildSequenceMemory(sequenceMemory);
        sequence.start(sequenceMemory, valueResolver);
    }

    public void stop(SequenceMemory memory, ValueResolver valueResolver) {
        while (memory != null) {
            Sequence sequence = memory.getSequence();
            int step = memory.getStep();
            if (step < sequence.getSteps().length) {
                sequence.getSteps()[step].deactivate(memory, valueResolver);
            }
            // Mark terminal so a subsequent stop() (e.g. retract after completion) is a no-op.
            memory.setStep(sequence.getSteps().length);
            memory = memory.getParent();
        }
    }

    public Sequence getSequence() {
        return sequence;
    }


}

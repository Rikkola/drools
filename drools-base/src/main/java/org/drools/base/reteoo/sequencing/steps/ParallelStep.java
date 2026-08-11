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
package org.drools.base.reteoo.sequencing.steps;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.SequencerMemory;
import org.drools.base.util.CircularArrayList;

import java.util.Arrays;

public class ParallelStep extends AbstractStep implements Step {
    private SubsequenceStep[] subsequenceSteps;

    private int outputSize;

    public ParallelStep(int index, int outputSize, Sequence parentSequence, SubsequenceStep... subsequenceSteps) {
        super(StepType.PARALLEL, index, parentSequence);
        this.outputSize       = outputSize;
        this.subsequenceSteps = subsequenceSteps;
        Arrays.stream(subsequenceSteps).forEach(s -> {
            if (s.getSubsequence().getOutputSize() > outputSize) {
                throw new IllegalArgumentException("The subprocess output size must not be more than the parallel output size");
            }
        });
    }

    public SubsequenceStep[] getSubsequenceSteps() {
        return subsequenceSteps;
    }

    @Override
    public void activate(SequenceMemory seqMem, ValueResolver valueResolver) {
        CircularArrayList<Object> data = seqMem.getData();
        SequencerMemory seqrMemory = seqMem.getSequencerMemory();
        data.addEmpty(outputSize);

        int memStartPos = data.size()-1;
        data.addEmpty(subsequenceSteps.length); // element for each SequenceMemory

        // make space for each
        for (int i = 0; i < this.subsequenceSteps.length; i++) {
            SequenceMemory subSequenceMemory = seqrMemory.getOrCreateSequenceMemory(seqMem, subsequenceSteps[i].getSubsequence(), i == 0 ? seqMem.getData() : null);
            data.set(memStartPos+i, subSequenceMemory);

            CircularArrayList<Object> subSeqData = subSequenceMemory.getData();
            subSeqData.addEmpty(outputSize);
            int eventsStartPosition = subSeqData.size() - (i == 0 ? subsequenceSteps.length : 0);
            subSequenceMemory.setEventsStartPosition(eventsStartPosition);
        }

        for (int i = 0; i < this.subsequenceSteps.length; i++) {
            subsequenceSteps[i].getSubsequence().start(seqrMemory.getSequenceMemory(subsequenceSteps[i].getSubsequence()), valueResolver);
        }
    }

    @Override
    public void deactivate(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
        CircularArrayList<Object> data = sequenceMemory.getData();

        // we only need the first sub process to get the start position
        SequenceMemory subSequenceMemory = sequenceMemory.getSequencerMemory().getSequenceMemory(subsequenceSteps[0].getSubsequence());
        data.resetHeadByOffset(data.size() - subSequenceMemory.getEventsStartPosition());
    }
}

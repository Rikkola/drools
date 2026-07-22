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
import org.drools.base.util.CircularArrayList;
import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;

public class SubsequenceStep extends AbstractStep implements Step {
    protected Sequence subsequence;

    public SubsequenceStep(int index, Sequence sequence, Sequence subsequence) {
        super(StepType.SUB_SEQUENCE, index, sequence);
        this.subsequence = subsequence;
    }

    public Sequence getSubsequence() {
        return subsequence;
    }

    @Override
    public void activate(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
        if (sequence != null) {
            // reserved for any context or return data
            // Also in the future it could be used to optional collect and hold nested array of subevents for later reference.
            CircularArrayList<Object> data = sequenceMemory.getData();

            SequenceMemory subSequenceMemory = sequenceMemory.getSequencerMemory().getOrCreateSequenceMemory(sequenceMemory, subsequence, data);
            data.addEmpty(subSequenceMemory.getSequence().getOutputSize());
            subSequenceMemory.setEventsStartPosition(sequenceMemory.getData().size());
            data.add(subSequenceMemory);
        }
        SequenceMemory subSequenceMemory = sequenceMemory.getSequencerMemory().getSequenceMemory(subsequence);

        subsequence.start(subSequenceMemory, valueResolver);
    }

    @Override
    public void deactivate(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
        if (sequence != null) {
            SequenceMemory            subSequenceMemory = sequenceMemory.getSequencerMemory().getSequenceMemory(subsequence);
            CircularArrayList<Object> events            = sequenceMemory.getData();
            events.resetHeadByOffset(events.size() - subSequenceMemory.getEventsStartPosition());
        }
    }
}

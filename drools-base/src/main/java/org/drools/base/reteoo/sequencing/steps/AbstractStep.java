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

public abstract class AbstractStep implements Step {

    protected final int                index;
    protected final Sequence           sequence; // the sequence the step is in
    protected       StepFailureHandler failureHandler = FailStackFailureHandler.getInstance();
    protected final StepType           type;

    public AbstractStep(StepType type, int index, Sequence sequence) {
        this.type = type;
        this.index    = index;
        this.sequence = sequence;
    }

    public StepType getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void onFail(SequenceMemory memory, ValueResolver valueResolver) {
        failureHandler.onFail(this, memory, valueResolver);
    }

    public interface StepFailureHandler {
        void onFail(Step step, SequenceMemory memory, ValueResolver valueResolver);
    }

    public static class FailStackFailureHandler implements StepFailureHandler {

        public static final FailStackFailureHandler INSTANCE = new FailStackFailureHandler();

        public static FailStackFailureHandler getInstance() {
            return INSTANCE;
        }

        @Override
        public void onFail(Step step, SequenceMemory sequenceMemory, ValueResolver valueResolver) {
            SequencerMemory sequencerMemory = sequenceMemory.getSequencerMemory();
            step.deactivate(sequenceMemory, valueResolver);
            sequencerMemory.getSequencer().stop(sequenceMemory, valueResolver);
        }
    }
}

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

/**
 * A sequence step that advances immediately on activation if no live fact
 * currently in the session matches the given filter index — i.e., the
 * absence of a match is the signal.
 *
 * Semantics: point-in-time absence at activation.
 * If a fact matching the filter is present when the step activates, the step
 * blocks and is never re-evaluated (retraction-driven re-activation is
 * deferred to a future version).
 */
public class AbsenceStep extends AbstractStep {

    private final int filterIndex;

    private AbsenceStep(Sequence sequence, int filterIndex) {
        super(sequence);
        this.filterIndex = filterIndex;
    }

    @Override
    public void activate(SequenceMemory memory, ValueResolver valueResolver) {
        SequencerMemory sequencerMemory = memory.getSequencerMemory();
        boolean absent = !sequencerMemory.hasActiveMatch(filterIndex, sequencerMemory.getLeftTuple(), valueResolver);
        if (absent) {
            // Nothing matches — absence confirmed; advance immediately.
            sequence.next(memory, valueResolver);
        }
        // If something matches, remain inactive — the sequence stalls here.
        // (Retraction-driven re-activation is deferred to a future version.)
    }

    @Override
    public void deactivate(SequenceMemory memory, ValueResolver valueResolver) {
        // Nothing to deactivate — absence steps don't register dynamic filters
        // or signal adapters in the live signal chain.
    }

    /**
     * Factory for AbsenceStep.  Extends StepFactory so it can be stored in the
     * same StepFactory[] array as the standard LogicCircuit-based factories.
     */
    public static class Factory extends Step.StepFactory {
        private final int filterIndex;

        public Factory(int filterIndex) {
            this.filterIndex = filterIndex;
        }

        @Override
        public Step createStep(Sequence sequence) {
            return new AbsenceStep(sequence, filterIndex);
        }
    }
}

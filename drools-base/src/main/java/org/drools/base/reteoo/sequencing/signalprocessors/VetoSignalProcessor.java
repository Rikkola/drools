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
package org.drools.base.reteoo.sequencing.signalprocessors;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;

/**
 * Output processor for the absence leaf gate inside a folded composite step.
 * When the absence pattern fires (a matching fact is inserted), this processor
 * resets the sequence: deactivates the current step, resets to step 0, and
 * re-activates step 0 so the sequence starts listening again from the beginning.
 * Uses a re-entry guard via {@link SequenceMemory#isStepVetoed()} to prevent infinite
 * recursion if {@code activate()} encounters a pre-existing blocker.
 */
public class VetoSignalProcessor extends SignalProcessor {

    private static final VetoSignalProcessor INSTANCE = new VetoSignalProcessor();

    private VetoSignalProcessor() {}

    public static VetoSignalProcessor get() {
        return INSTANCE;
    }

    @Override
    public void consume(SequenceMemory memory, ValueResolver valueResolver) {
        if (memory.isStepVetoed()) {
            // Re-entry guard: activate() found a pre-existing blocker and re-triggered us.
            // The step is already being reset — do nothing.
            return;
        }
        int step = memory.getStep();
        memory.getSequence().getSteps()[step].deactivate(memory, valueResolver);
        memory.setStep(0);
        memory.setStepVetoed(true); // guard against re-entry from LogicCircuitStep.activate()
        memory.getSequence().getSteps()[0].activate(memory, valueResolver);
        memory.setStepVetoed(false); // clear: activation completed without recursion
    }

    @Override
    public void consume(int signalBitIndex, SequenceMemory memory, ValueResolver valueResolver) {
        consume(memory, valueResolver);
    }

    @Override
    protected void reset(SequenceMemory memory, ValueResolver valueResolver) {
        // Nothing to reset — reset-to-step-0 is performed directly in consume().
    }
}

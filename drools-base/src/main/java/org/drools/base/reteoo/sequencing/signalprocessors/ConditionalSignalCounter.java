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
import org.drools.base.util.index.ConstraintTypeOperator;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;

import java.util.function.Consumer;
import java.util.function.LongPredicate;

public class ConditionalSignalCounter extends SignalProcessor {
    private final int signalIndex;
    private final int counterIndex;
    private final LongPredicate constraint;

    private SignalProcessor output;

    public ConditionalSignalCounter(int signalIndex, int counterIndex, LongPredicate constraint) {
        this.signalIndex = signalIndex;
        this.counterIndex = counterIndex;
        this.constraint   = constraint;
    }

    public ConditionalSignalCounter(int signalIndex, int counterIndex, ConstraintTypeOperator operator, long cardinal) {
        this.signalIndex = signalIndex;
        this.counterIndex = counterIndex;
        this.constraint        = c -> {
            switch (operator) {
                case EQUAL:
                    return c == cardinal;
                case NOT_EQUAL:
                    return c != cardinal;
                case GREATER_THAN:
                    return c > cardinal;
                case GREATER_OR_EQUAL:
                    return c >= cardinal;
                case LESS_THAN:
                    return c < cardinal;
                case LESS_OR_EQUAL:
                    return c <= cardinal;

            }
            throw new IllegalStateException("Unknown operator: " + operator);
        };
    }

    public int getSignalIndex() {
        return signalIndex;
    }

    public int getCounterIndex() {
        return counterIndex;
    }

    public SignalProcessor getOutput() {
        return output;
    }

    public void setOutput(SignalProcessor output) {
        this.output = output;
    }

    @Override
    public void consume(SignalStatus incomingSignalStatus, SequenceMemory memory, ValueResolver valueResolver) {
        consume(incomingSignalStatus, memory,
                (SignalStatus status) -> output.consume(status, memory, valueResolver), valueResolver);
    }

    @Override
    public void consume(int signalBitIndex, SignalStatus incomingSignalStatus, SequenceMemory memory, ValueResolver valueResolver) {
        consume(incomingSignalStatus, memory,
                (SignalStatus status) -> output.consume(signalBitIndex, incomingSignalStatus, memory, valueResolver), valueResolver);
    }

    private void consume(SignalStatus inputSignalStatus, SequenceMemory memory, Consumer<SignalStatus> propagator, ValueResolver valueResolver) {
        SignalStatus status = memory.getCounterSignalStatus(counterIndex);

        SignalStatus priorStatus   = status;
        long         originalCount = memory.getCounterMemories()[counterIndex];
        long         newCount      = ++originalCount;
        memory.getCounterMemories()[counterIndex] = newCount;

        boolean matched = constraint.test(newCount);
        if (matched) {
            status = SignalStatus.MATCHED;
        } else if (priorStatus == SignalStatus.MATCHED) {
            // was matched, now unmatched, so it has failed.
            status = SignalStatus.FAILED;
        }

        memory.setCounterSignalStatus(counterIndex, status);

        if (status == SignalStatus.FAILED) {
            memory.getSequence().fail(memory, valueResolver);
        } else if (priorStatus != status) {
            propagator.accept(status);
        }
    }

    public void reset(SequenceMemory memory, ValueResolver valueResolver) {
        memory.resetSignalCounterMemory(counterIndex);
    }
}

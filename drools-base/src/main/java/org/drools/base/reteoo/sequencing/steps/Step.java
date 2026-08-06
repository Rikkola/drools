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
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit;
import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;

import java.util.function.Consumer;

public interface Step {
    void activate(SequenceMemory memory, ValueResolver valueResolver);

    void deactivate(SequenceMemory memory, ValueResolver valueResolver);

    void onFail(SequenceMemory memory, ValueResolver valueResolver);

    StepType getType();

    static StepFactory of(LogicCircuit circuit) {
        StepFactory factory = new StepFactory(StepType.LOGIC_CIRCUIT);
        factory.setCircuit(circuit);
        return factory;
    }

    static StepFactory of(Sequence subsequence) {
        StepFactory factory = new StepFactory(StepType.SUB_SEQUENCE);
        factory.setSubsequence(subsequence);
        return factory;
    }

    static StepFactory of(Sequence... subsequences) {
        StepFactory factory = new StepFactory(StepType.PARALLEL);
        factory.setSubsequences(subsequences);
        return factory;
    }

    static StepFactory of(Sequence sequence, Consumer<SequenceMemory> function) {
        StepFactory factory = new StepFactory(StepType.AGGREGATOR);
        factory.setAggregator(function);
        factory.setSubsequence(sequence);
        return factory;
    }

    static StepFactory of(Consumer<SequenceMemory> function) {
        StepFactory factory = new StepFactory(StepType.ACTION);
        factory.setAction(function);
        return factory;
    }

    Sequence getSequence();


    enum StepType {
        LOGIC_CIRCUIT, SUB_SEQUENCE, AGGREGATOR, ACTION, PARALLEL
    }

    class StepFactory {
        private final StepType     type;
        private       LogicCircuit circuit;
        private Sequence                 subsequence;
        private Sequence[]               subsequences;
        private Consumer<SequenceMemory> aggregator;
        private Consumer<SequenceMemory> action;

        public StepFactory(StepType type) {
            this.type = type;
        }

        public StepType getType() {
            return type;
        }

        public LogicCircuit getCircuit() {
            return circuit;
        }

        public void setCircuit(LogicCircuit circuit) {
            this.circuit = circuit;
        }

        public void setSubsequence(Sequence subsequence) {
            this.subsequence = subsequence;
        }

        public void setSubsequences(Sequence[] subsequences) {
            this.subsequences = subsequences;
        }

        public Consumer<SequenceMemory> getAggregator() {
            return aggregator;
        }

        public void setAggregator(Consumer<SequenceMemory> aggregator) {
            this.aggregator = aggregator;
        }

        public Consumer<SequenceMemory> getAction() {
            return action;
        }

        public void setAction(Consumer<SequenceMemory> action) {
            this.action = action;
        }

        public Step createStep(int index, Sequence sequence) {
            switch (type) {
                case LOGIC_CIRCUIT:
                    return new LogicCircuitStep(index, sequence, circuit);
                case AGGREGATOR:
                    return new AggregatorStep(index, sequence, this.subsequence, aggregator);
                case SUB_SEQUENCE:
                    return new SubsequenceStep(index, sequence, this.subsequence);
                case ACTION:
                    return new ActionStep(index, sequence, action);
                case PARALLEL:
                    SubsequenceStep[] steps = new SubsequenceStep[subsequences.length];
                    for ( int i = 0; i < steps.length; i++ ) {
                        steps[i] = (SubsequenceStep) Step.of(subsequences[i]).createStep(i, sequence);
                    }
                    int outputSize = 0;
                    // Parallel output size must be the same size as the largest output.
                    for (int i = 0; i < subsequences.length; i++ ) {
                        Sequence s = subsequences[i];
                        s.setSubsequenceIndex(i);
                        if (outputSize < s.getOutputSize() ) {
                            outputSize = s.getOutputSize();
                        }
                    }
                    return new ParallelStep(index, outputSize, sequence, steps);
            }
            throw new IllegalArgumentException("Unsupported step type: " + type);
        }
    }

}

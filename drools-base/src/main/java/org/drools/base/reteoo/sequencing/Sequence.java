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
import org.drools.base.phreak.actions.AbstractPropagationEntry;
import org.drools.base.reteoo.DynamicFilter;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.reteoo.sequencing.signalprocessors.SignalStatus;
import org.drools.base.reteoo.sequencing.steps.LogicCircuitStep;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.base.reteoo.sequencing.steps.Step.StepFactory;
import org.drools.base.reteoo.sequencing.steps.SubsequenceStep;
import org.drools.base.rule.Declaration;
import org.drools.base.rule.Pattern;
import org.drools.base.rule.RuleConditionElement;
import org.drools.base.time.JobHandle;
import org.drools.base.time.Trigger;
import org.drools.base.time.Timer;
import org.drools.base.reteoo.SignalAdapter;
import org.drools.base.time.Job;
import org.drools.base.time.JobContext;
import org.drools.base.util.CircularArrayList;
import org.kie.api.runtime.rule.FactHandle;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Sequence implements RuleConditionElement {
    private final int sequenceIndex;

    private final SubsequenceStep parentStep;


    private Pattern[] filters;

    private Step[] steps;

    private LogicGate[] gates;

    private SequenceController controller;

    private Consumer<SequenceMemory> onStart;
    private Consumer<SequenceMemory> onEnd;

    private int outputSize;

    private int subsequenceIndex = -1; // -1 is for when this is not parallel

    public Sequence(int sequenceIndex, SubsequenceStep parentStep, StepFactory... stepFactories) {
        this.steps = new Step[stepFactories.length];
        this.sequenceIndex = sequenceIndex;
        this.parentStep = parentStep;
        this.controller = new DefaultController();

        for ( int i = 0; i < steps.length; i++ ) {
            steps[i] = stepFactories[i].createStep(i,this);
        }
        populateLogicGates();
    }

    public Sequence(int sequenceIndex, StepFactory... stepFactories) {
        this(sequenceIndex, null, stepFactories);
    }

    public Pattern[] getFilters() {
        return filters;
    }

    public void setFilters(Pattern[] filters) {
        this.filters = filters;
    }

    public int getOutputSize() {
        return outputSize;
    }

    public int getSubsequenceIndex() {
        return subsequenceIndex;
    }

    public void setSubsequenceIndex(int subsequenceIndex) {
        this.subsequenceIndex = subsequenceIndex;
    }

    public void setOutputSize(int outputSize) {
        this.outputSize = outputSize;
    }

    public Consumer<SequenceMemory> getOnStart() {
        return onStart;
    }

    public void setOnStart(Consumer<SequenceMemory> onStart) {
        this.onStart = onStart;
    }

    public Consumer<SequenceMemory> getOnEnd() {
        return onEnd;
    }

    public void setOnEnd(Consumer<SequenceMemory> onEnd) {
        this.onEnd = onEnd;
    }

    public void populateLogicGates() {
        List<LogicGate> list = new ArrayList<>();
        for (Step step : getSteps()) {
            if ( step instanceof LogicCircuitStep) {
                Arrays.stream(((LogicCircuitStep) step).getCircuit().getGates()).forEach( g -> list.add(g));
            }
        }

        gates = list.toArray(new LogicGate[0]);
    }

    public int getSequenceIndex() {
        return sequenceIndex;
    }

    public Step[] getSteps() {
        return steps;
    }

    public void setSteps(Step[] steps) {
        this.steps = steps;
    }

    public LogicGate[] getGates() {
        return gates;
    }


    public SequenceController getController() {
        return controller;
    }

    public void setController(SequenceController controller) {
        this.controller = controller;
    }

    @Override
    public Map<String, Declaration> getInnerDeclarations() {
        return Map.of();
    }

    @Override
    public Map<String, Declaration> getOuterDeclarations() {
        return Map.of();
    }

    @Override
    public Declaration resolveDeclaration(String identifier) {
        return null;
    }

    @Override
    public RuleConditionElement clone() {
        throw new UnsupportedOperationException("Sequence does not support clone()");
    }

    @Override
    public List<? extends RuleConditionElement> getNestedElements() {
        return List.of();
    }

    @Override
    public boolean isPatternScopeDelimiter() {
        return false;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        throw new UnsupportedOperationException("Sequence does not support serialization");
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("Sequence does not support serialization");
    }

    public void start(SequenceMemory memory, ValueResolver valueResolver) {
        controller.start(memory, valueResolver);
    }

    private void restart(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
        sequenceMemory.setStep(0);
        sequenceMemory.getData().resetHeadByOffset(sequenceMemory.getSequencerMemory().getData().size() - sequenceMemory.getEventsStartPosition());
        getSteps()[0].activate(sequenceMemory, valueResolver);
    }

    public void next(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
         controller.next(sequenceMemory, valueResolver);
    }

    public interface SequenceController {
        default void start(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
            sequenceMemory.setStep(0);
            sequenceMemory.sequence.steps[0].activate(sequenceMemory, valueResolver);
            if(sequenceMemory.sequence.onStart != null) {
                sequenceMemory.sequence.onStart.accept(sequenceMemory);
            }
        }

        default void restart(SequenceMemory memory, ValueResolver valueResolver) {

        }

        default void next(SequenceMemory memory, ValueResolver valueResolver) {

        }

        void end(SequenceMemory memory, ValueResolver valueResolver);
    }

    public static class DefaultController implements SequenceController {
        private static final DefaultController INSTANCE = new DefaultController();

        public static DefaultController getInstance() {
            return INSTANCE;
        }

        public void next(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
            Sequence sequence = sequenceMemory.getSequence();
            int step = sequenceMemory.getStep();

            sequence.steps[step].deactivate(sequenceMemory, valueResolver);
            step = sequenceMemory.incrementStep();

            if (step < sequenceMemory.getSequence().getSteps().length) {
                sequence.steps[step].activate(sequenceMemory, valueResolver);
            } else {
                if(sequence.onEnd != null) {
                    sequence.onEnd.accept(sequenceMemory);
                }
                end(sequenceMemory, valueResolver);
            }
        }

        @Override
        public void end(SequenceMemory sequenceMemory, ValueResolver valueResolver)  {
            SequenceMemory parent = sequenceMemory.getParent();
            if (parent != null) {
                SequenceMemory parentSeqMemory = parent.getSequencerMemory().getSequenceMemory(parent.getSequence());
                parent.getSequence().next(parentSeqMemory, valueResolver);
            } else {
                sequenceMemory.getSequencerMemory().match(valueResolver);
            }
        }

        @Override
        public String toString() {
            return "DefaultController{}";
        }
    }

    public void fail(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
        int index = sequenceMemory.getStep();
        Step step = sequenceMemory.getSequence().getSteps()[index];
        step.onFail(sequenceMemory, valueResolver);
    }

    public static class SequenceMemory {
        private SequenceMemory parent;

        private final Sequence sequence;

        private int step;

        private int count;

        private final SequencerMemory sequencerMemory;

        private final SignalAdapter[] signalAdapters;

        private final SignalAdapter[] activeSignalAdapters;

        private final long[] gateMemory;

        private final long[] counterMemories;

        private JobHandle[] jobHandles;

        private JobHandle jobHandle;

        private final SignalStatus[] signalStatuses;

        private int eventsStartPosition;

        private CircularArrayList<Object> data;

        public SequenceMemory(SequencerMemory sequencerMemory, Sequence sequence, CircularArrayList<Object> data,
                              SignalAdapter[] signalAdapters, SignalAdapter[] activeSignalAdapters,
                              long[] gateMemory, long[] counterMemories) {
            this(sequencerMemory, null, sequence, data, signalAdapters, activeSignalAdapters, gateMemory, counterMemories);
        }

        public SequenceMemory(SequencerMemory sequencerMemory, SequenceMemory parent, Sequence sequence, CircularArrayList<Object> data,
                              SignalAdapter[] signalAdapters, SignalAdapter[] activeSignalAdapters,
                              long[] gateMemory, long[] counterMemories) {
            this.sequencerMemory      = sequencerMemory;
            this.parent               = parent;
            this.data                 = data;
            this.sequence             = sequence;
            this.signalAdapters       = signalAdapters;
            this.activeSignalAdapters = activeSignalAdapters;
            this.gateMemory           = gateMemory;
            this.counterMemories      = counterMemories;
            this.signalStatuses       = new SignalStatus[gateMemory.length + counterMemories.length];
        }

        public SequenceMemory getParent() {
            return parent;
        }

        public void setParent(SequenceMemory parent) {
            this.parent = parent;
        }

        public Sequence getSequence() {
            return sequence;
        }

        public SequencerMemory getSequencerMemory() {
            return sequencerMemory;
        }


        public SignalStatus getCounterSignalStatus(int index) {
            return signalStatuses[gateMemory.length + index];
        }

        public void setCounterSignalStatus(int index, SignalStatus status) {
            signalStatuses[gateMemory.length + index] = status;
        }

        public SignalStatus getLogicGateSignalStatus(int index) {
            return signalStatuses[index];
        }

        public SignalStatus[] getLogicGateSignalStatus() {
            return signalStatuses;
        }

        public void setLogicGateSignalStatus(int index, SignalStatus status) {
            signalStatuses[index] = status;
        }

        public SignalAdapter[] getSignalAdapters() {
            return signalAdapters;
        }

        public SignalAdapter[] getActiveSignalAdapters() {
            return activeSignalAdapters;
        }

        public long[] getLogicGateMemory() {
            return gateMemory;
        }

        public long[] getCounterMemories() {
            return counterMemories;
        }

        public JobHandle[] getJobHandles() {
            return jobHandles;
        }

        public JobHandle getJobHandle() {
            return jobHandle;
        }

        public void setJobHandle(JobHandle jobHandle) {
            this.jobHandle = jobHandle;
        }

        public SignalStatus[] getSignalStatuses() {
            return signalStatuses;
        }

        public int incrementStep() {
            return ++step;
        }

        public int getStep() {
            return step;
        }

        public void setStep(int step) {
            this.step = step;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getEventsStartPosition() {
            return eventsStartPosition;
        }

        public void addData(FactHandle handle) {
            data.add(handle);
        }

        public CircularArrayList<Object> getData() {
            return data;
        }

        public int getOutputStartPosition() {
            if (sequence.getSubsequenceIndex() == -1) {
                return eventsStartPosition - sequence.getOutputSize();
            } else {
                int i = eventsStartPosition - ((sequence.getSubsequenceIndex() + 1) * sequence.getOutputSize());
                return i;
            }
        }

        public void setEventsStartPosition(int eventsStartPosition) {
            this.eventsStartPosition = eventsStartPosition;
        }

        public SignalAdapter activateSignalAdapter(int filterIndex, LogicGate gate, int signalAdapterIndex, int signalBitIndex) {
            if (activeSignalAdapters[signalAdapterIndex] != null) {
                throw new RuntimeException("Defensive coding, this should not be re-entrant");
            }

            SignalAdapter signalAdapter = signalAdapters[signalAdapterIndex];

            if (signalAdapter == null) {
                signalAdapter = new SignalAdapter(gate, signalBitIndex, this);
                signalAdapters[signalAdapterIndex] = signalAdapter;
            }

            activeSignalAdapters[signalAdapterIndex] = signalAdapter;

            DynamicFilter filter = sequencerMemory.getActiveDynamicFilter(filterIndex);
            filter.addSignalAdapter(signalAdapter);

            return signalAdapter;
        }

        public void setJobHandle(int index, JobHandle handle) {
            if (jobHandles == null) {
                // lazily create
                jobHandles = new JobHandle[gateMemory.length]; // each gate can potentially have a job handle
            }
            jobHandles[index] = handle;
        }


        public JobHandle getJobHandle(int index) {
            return jobHandles != null ? jobHandles[index] : null;
        }

        public void deactivateSignalAdapter(int filterIndex, LogicGate gate, int signalAdapterIndex) {
            SignalAdapter signalAdapter = activeSignalAdapters[signalAdapterIndex];
            if (signalAdapter == null) {
                // Already deactivated — e.g. FailStackFailureHandler called deactivate()
                // before Sequencer.stop() reaches the same step. No-op to prevent a
                // double-remove from the linked filter list.
                return;
            }
            activeSignalAdapters[signalAdapterIndex] = null;

            DynamicFilter filter = sequencerMemory.getActiveDynamicFilter(filterIndex);
            filter.removeSignalAdapter(signalAdapter);

            if (filter.getSignalAdapters().isEmpty()) {
                sequencerMemory.removeActiveFilter(filter);
            }
        }

        public void resetLogicGateMemory(int gateIndex, ValueResolver valueResolver) {
            gateMemory[gateIndex]     = 0;
            signalStatuses[gateIndex] = null;
        }

        public void resetSignalCounterMemory(int counterIndex) {
            signalStatuses[gateMemory.length + counterIndex] = null;
            counterMemories[counterIndex]                    = 0;
        }

        public void cancelJobHandle(int gateIndex, ValueResolver valueResolver) {
            if (jobHandles != null) {
                JobHandle handle = jobHandles[gateIndex];
                valueResolver.getTimerService().removeJob(handle);
                jobHandles[gateIndex] = null;
            }
        }

        public void clearJobHandle(int gateIndex, ValueResolver valueResolver) {
            jobHandles[gateIndex] = null;
        }

        @Override
        public String toString() {
            return "SequenceMemory{" +
                   "sequence=" + sequence.getSequenceIndex() +
                   ", step=" + step +
                   ", count=" + count +
                   '}';
        }
    }

    @Override
    public String toString() {
        return "Sequence{" +
               "sequenceIndex=" + sequenceIndex +
               ", steps=" + Arrays.toString(steps) +
               ", controller=" + controller +
               '}';
    }
}

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
import org.drools.base.reteoo.DynamicFilter;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.reteoo.sequencing.steps.LogicCircuitStep;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.base.reteoo.sequencing.steps.Step.StepFactory;
import org.drools.base.rule.Declaration;
import org.drools.base.rule.Pattern;
import org.drools.base.rule.RuleConditionElement;
import org.drools.base.reteoo.SignalAdapter;
import org.drools.base.time.Job;
import org.drools.base.time.JobContext;
import org.drools.base.time.JobHandle;
import org.drools.base.time.Trigger;
import org.drools.base.time.impl.Timer;
import org.drools.base.util.CircularArrayList;
import org.kie.api.runtime.rule.FactHandle;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Sequence implements RuleConditionElement {
    private final int sequenceIndex;

    private Pattern[] filters;

    private Step[] steps;

    private LogicGate[] gates;

    private SequenceController controller;

    public Sequence(int sequenceIndex, StepFactory... stepFactories) {
        this.steps = new Step[stepFactories.length];
        this.sequenceIndex = sequenceIndex;
        this.controller = DefaultController.getInstance();

        for ( int i = 0; i < steps.length; i++ ) {
            steps[i] = stepFactories[i].createStep(this);
        }
        populateLogicGates();
    }

    public Pattern[] getFilters() {
        return filters;
    }

    public void setFilters(Pattern[] filters) {
        this.filters = filters;
    }

    public void populateLogicGates() {
        List<LogicGate> list = new ArrayList<>();
        for (Step step : getSteps()) {
            if (step instanceof LogicCircuitStep) {
                LogicGate[] circuitGates = ((LogicCircuitStep) step).getCircuit().getGates();
                for (int i = 0; i < circuitGates.length; i++) {
                    list.add(circuitGates[i]);
                }
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

    public LogicGate[] getGates() {
        return gates;
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
        Sequence clone = new Sequence(sequenceIndex);
        clone.steps      = this.steps;
        clone.gates      = this.gates;
        clone.filters    = this.filters;
        clone.controller = this.controller;
        return clone;
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

    public void setController(SequenceController controller) {
        this.controller = controller;
    }

    public void start(SequenceMemory memory, ValueResolver valueResolver) {
        controller.start(memory, valueResolver);
    }

    public void next(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
        controller.next(sequenceMemory, valueResolver);
    }

    /**
     * Fail the sequence: deactivate the current step and mark the memory as terminal
     * (step == steps.length) without firing the consequence.
     * Called by timeout jobs when a deadline expires before the sequence completes.
     */
    public void fail(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
        int step = sequenceMemory.getStep();
        if (step < steps.length) {
            steps[step].deactivate(sequenceMemory, valueResolver);
            sequenceMemory.setStep(steps.length); // mark terminal
        }
        // No consequence fires — silent failure.
    }

    public interface SequenceController {
        default void start(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
            sequenceMemory.setStep(0);
            sequenceMemory.sequence.steps[0].activate(sequenceMemory, valueResolver);
        }

        default void next(SequenceMemory memory, ValueResolver valueResolver) {

        }

        void end(SequenceMemory memory, ValueResolver valueResolver);
    }

    public static class DefaultController implements SequenceController {
        private static final DefaultController INSTANCE = new DefaultController();

        private DefaultController() {}

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
                end(sequenceMemory, valueResolver);
            }
        }

        @Override
        public void end(SequenceMemory sequenceMemory, ValueResolver valueResolver)  {
            sequenceMemory.getSequencerMemory().match(valueResolver);
        }

        @Override
        public String toString() {
            return "DefaultController{}";
        }
    }

    public static class SequenceMemory {
        private final Sequence sequence;

        private int step;

        private final SequencerMemory sequencerMemory;

        private final SignalAdapter[] signalAdapters;

        private final SignalAdapter[] activeSignalAdapters;

        private final long[] gateMemory;

        private final boolean[] gateMatched;

        private CircularArrayList<Object> data;

        /** Whole-sequence deadline job handle, set by {@link TimeoutController}. */
        private JobHandle jobHandle;

        /** Per-gate timeout job handles, lazily created by {@link LogicGate.TimeoutTimer}. */
        private JobHandle[] jobHandles;

        public SequenceMemory(SequencerMemory sequencerMemory, Sequence sequence, CircularArrayList<Object> data,
                              SignalAdapter[] signalAdapters, SignalAdapter[] activeSignalAdapters,
                              long[] gateMemory, long[] counterMemories) {
            this.sequencerMemory      = sequencerMemory;
            this.data                 = data;
            this.sequence             = sequence;
            this.signalAdapters       = signalAdapters;
            this.activeSignalAdapters = activeSignalAdapters;
            this.gateMemory           = gateMemory;
            this.gateMatched          = new boolean[gateMemory.length + counterMemories.length];
        }

        public Sequence getSequence() {
            return sequence;
        }

        public SequencerMemory getSequencerMemory() {
            return sequencerMemory;
        }

        public boolean isLogicGateMatched(int index) {
            return gateMatched[index];
        }

        public void setLogicGateMatched(int index, boolean matched) {
            gateMatched[index] = matched;
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

        public int incrementStep() {
            return ++step;
        }

        public int getStep() {
            return step;
        }

        public void setStep(int step) {
            this.step = step;
        }

        public void setData(int filterIndex, FactHandle handle) {
            data.put(filterIndex, handle);
        }

        public SignalAdapter activateSignalAdapter(int filterIndex, LogicGate gate, int signalAdapterIndex, int signalBitIndex) {
            if (activeSignalAdapters[signalAdapterIndex] != null) {
                throw new RuntimeException("Defensive coding, this should not be re-entrant");
            }

            SignalAdapter signalAdapter = signalAdapters[signalAdapterIndex];

            if (signalAdapter == null) {
                signalAdapter = new SignalAdapter(gate, signalBitIndex, filterIndex, this);
                signalAdapters[signalAdapterIndex] = signalAdapter;
            }

            activeSignalAdapters[signalAdapterIndex] = signalAdapter;

            DynamicFilter filter = sequencerMemory.getActiveDynamicFilter(filterIndex);
            filter.addSignalAdapter(signalAdapter);

            return signalAdapter;
        }

        public void deactivateSignalAdapter(int filterIndex, int signalAdapterIndex) {
            SignalAdapter signalAdapter = activeSignalAdapters[signalAdapterIndex];
            if (signalAdapter == null) {
                // Already deactivated — no-op to prevent a double-remove from the linked filter list.
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
            gateMatched[gateIndex]    = false;
        }

        // ---- whole-sequence timeout job handle ----

        public JobHandle getJobHandle() {
            return jobHandle;
        }

        public void setJobHandle(JobHandle jobHandle) {
            this.jobHandle = jobHandle;
        }

        // ---- per-gate timeout job handles ----

        public void setJobHandle(int gateIndex, JobHandle handle) {
            if (jobHandles == null) {
                jobHandles = new JobHandle[gateMemory.length];
            }
            jobHandles[gateIndex] = handle;
        }

        public JobHandle getJobHandle(int gateIndex) {
            return jobHandles != null ? jobHandles[gateIndex] : null;
        }

        public void cancelJobHandle(int gateIndex, ValueResolver valueResolver) {
            if (jobHandles != null) {
                JobHandle handle = jobHandles[gateIndex];
                if (handle != null) {
                    valueResolver.getTimerService().removeJob(handle);
                    jobHandles[gateIndex] = null;
                }
            }
        }

        public void clearJobHandle(int gateIndex) {
            if (jobHandles != null) {
                jobHandles[gateIndex] = null;
            }
        }

        @Override
        public String toString() {
            return "SequenceMemory{" +
                   "sequence=" + sequence.getSequenceIndex() +
                   ", step=" + step +
                   '}';
        }
    }

    /**
     * A whole-sequence deadline controller.
     * Extends {@link DefaultController} so {@code next()} → {@code end()} routes through
     * {@link TimeoutController#end}, which cancels the deadline job on normal completion.
     */
    public static class TimeoutController extends DefaultController {
        private final Timer timer;

        public TimeoutController(Timer timer) {
            this.timer = timer;
        }

        @Override
        public void start(SequenceMemory memory, ValueResolver valueResolver) {
            super.start(memory, valueResolver);
            Trigger trigger = timer.createTrigger(valueResolver.getTimerService().getCurrentTime(), null, null);
            SequenceTimerJobContext ctx = new SequenceTimerJobContext(trigger, valueResolver, memory);
            JobHandle jobHandle = valueResolver.getTimerService().scheduleJob(SequenceJob.getInstance(), ctx, trigger);
            memory.setJobHandle(jobHandle);
        }

        @Override
        public void end(SequenceMemory memory, ValueResolver valueResolver) {
            // Cancel the deadline: sequence completed normally.
            if (memory.getJobHandle() != null) {
                valueResolver.getTimerService().removeJob(memory.getJobHandle());
                memory.setJobHandle(null);
            }
            super.end(memory, valueResolver);
        }

        @Override
        public String toString() {
            return "TimeoutController{timer=" + timer + '}';
        }
    }

    /**
     * The timer-service job that fires when the whole-sequence deadline expires.
     */
    public static class SequenceJob implements Job {
        private static final SequenceJob INSTANCE = new SequenceJob();

        public static SequenceJob getInstance() {
            return INSTANCE;
        }

        @Override
        public void execute(JobContext ctx) {
            SequenceTimerJobContext timerJobCtx = (SequenceTimerJobContext) ctx;
            ValueResolver valueResolver = timerJobCtx.getValueResolver();
            valueResolver.addTimerAction(new SequenceTimerAction(timerJobCtx));
        }
    }

    /**
     * Context passed to the {@link SequenceJob} when a whole-sequence deadline is scheduled.
     */
    public static class SequenceTimerJobContext implements JobContext {
        private static final long serialVersionUID = 1L;

        private       JobHandle     jobHandle;
        private final Trigger       trigger;
        private final ValueResolver valueResolver;
        private final SequenceMemory sequenceMemory;

        public SequenceTimerJobContext(Trigger trigger, ValueResolver valueResolver, SequenceMemory sequenceMemory) {
            this.trigger        = trigger;
            this.valueResolver  = valueResolver;
            this.sequenceMemory = sequenceMemory;
        }

        @Override
        public JobHandle getJobHandle() {
            return jobHandle;
        }

        @Override
        public ValueResolver getValueResolver() {
            return valueResolver;
        }

        @Override
        public void setJobHandle(JobHandle jobHandle) {
            this.jobHandle = jobHandle;
        }

        public Trigger getTrigger() {
            return trigger;
        }

        public SequenceMemory getSequenceMemory() {
            return sequenceMemory;
        }
    }

    /**
     * Action executed on the session thread when a whole-sequence deadline fires.
     */
    public static class SequenceTimerAction implements ValueResolver.TimerAction {

        private final SequenceTimerJobContext jobCtx;

        public SequenceTimerAction(SequenceTimerJobContext jobCtx) {
            this.jobCtx = jobCtx;
        }

        @Override
        public void execute(ValueResolver valueResolver) {
            SequenceMemory sequenceMemory = jobCtx.getSequenceMemory();

            // Clear the handle first — the job is firing, not being cancelled.
            sequenceMemory.setJobHandle(null);

            // Match-monotonicity: if the sequence already completed, do nothing.
            if (sequenceMemory.getStep() < sequenceMemory.getSequence().getSteps().length) {
                sequenceMemory.getSequence().fail(sequenceMemory, valueResolver);
            }
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

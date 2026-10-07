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
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit.LongBiPredicate;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.time.Job;
import org.drools.base.time.JobContext;
import org.drools.base.time.JobHandle;
import org.drools.base.time.Trigger;
import org.drools.base.time.impl.Timer;

public class LogicGate extends SignalProcessor {
    protected long allMatched;

    private SignalProcessor output;

    private final LongBiPredicate predicate;

    private final int gateIndex;

    private LogicGate[] inputGates = EMPTY_INPUT_GATES;

    private final int[] filterIndexes;

    private int[] signalAdapterIndexes;

    private boolean statusCanRevert;

    private static final LogicGate[] EMPTY_INPUT_GATES = new LogicGate[0];

    private PropagationTimer propagationTimer;

    public LogicGate(LongBiPredicate predicate, int gateIndex, int[] filterIndexes, int[] signalAdapterIndexes, int nbrOfInputGates) {
        this.predicate = predicate;

        this.filterIndexes        = filterIndexes;
        this.signalAdapterIndexes = signalAdapterIndexes;

        int totalInputs = signalAdapterIndexes.length + nbrOfInputGates;
        if (totalInputs > Long.SIZE) {
            throw new IllegalArgumentException(
                    "LogicGate supports at most " + Long.SIZE + " inputs; got " + totalInputs);
        }
        for (int i = 0; i < totalInputs; i++) {
            allMatched = allMatched | (1L << i);
        }

        this.gateIndex = gateIndex;
    }

    public LogicGate(LongBiPredicate predicate, int gateIndex, int[] filterIndexes,
                     int[] signalAdapterIndexes, int nbrOfInputGates, boolean statusCanRevert) {
        this(predicate, gateIndex, filterIndexes, signalAdapterIndexes, nbrOfInputGates);
        this.statusCanRevert = statusCanRevert;
    }

    public void setPropagationTimer(PropagationTimer propagationTimer) {
        this.propagationTimer = propagationTimer;
    }

    public int[] getSignalAdapterIndexes() {
        return signalAdapterIndexes;
    }

    public void setInputGates(LogicGate... inputGates) {
        this.inputGates = inputGates;
    }

    public void setOutput(SignalProcessor output) {
        this.output = output;
    }

    public SignalProcessor getOutput() {
        return output;
    }

    public int getGateIndex() {
        return gateIndex;
    }

    @Override
    public void consume(SequenceMemory memory, ValueResolver valueResolver) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void consume(int signalBitIndex, SequenceMemory memory, ValueResolver valueResolver) {
        long currentMatched = memory.getLogicGateMemory()[gateIndex] | (1L << (signalBitIndex - 1));
        memory.getLogicGateMemory()[gateIndex] = currentMatched;

        boolean matched = predicate.test(currentMatched, allMatched);

        if (matched && !memory.isLogicGateMatched(gateIndex)) {
            memory.setLogicGateMatched(gateIndex, true);
            if (propagationTimer != null) {
                propagationTimer.matched(memory, valueResolver);
            } else {
                propagate(memory, valueResolver);
            }
        } else if (!matched && statusCanRevert) {
            if (memory.isLogicGateMatched(gateIndex)) {
                // Gate was previously MATCHED and is now reverting — clear the parent gate's
                // contribution bit so the parent AND/OR gate no longer sees this gate as matched.
                memory.setLogicGateMatched(gateIndex, false);
                if (output instanceof LogicGateOutputSignalProcessor) {
                    ((LogicGateOutputSignalProcessor) output).clearParentBit(memory);
                }
            }
        }
    }

    public void propagate(SequenceMemory memory, ValueResolver valueResolver) {
        resetPrior(memory, valueResolver);
        output.consume(memory, valueResolver);
    }

    public void resetPrior(SequenceMemory memory, ValueResolver valueResolver) {
        for (LogicGate gate : inputGates) {
            gate.reset(memory, valueResolver);
        }

        if (!statusCanRevert) {
            memory.resetLogicGateMemory(gateIndex, valueResolver);
        }
    }

    public void reset(SequenceMemory memory, ValueResolver valueResolver) {
        resetPrior(memory, valueResolver);
        output.reset(memory, valueResolver);
    }

    public void activate(SequenceMemory memory, ValueResolver valueResolver) {
        for (int i = 0; i < filterIndexes.length; i++) {
            memory.activateSignalAdapter(filterIndexes[i], this, signalAdapterIndexes[i], i + 1); // bit indexes start at 1
        }
        if (predicate.test(0L, allMatched)) {
            memory.setLogicGateMatched(gateIndex, true);
            if (propagationTimer != null) {
                propagationTimer.matched(memory, valueResolver);
            } else {
                propagate(memory, valueResolver);
            }
        } else if (propagationTimer != null) {
            propagationTimer.activated(memory, valueResolver);
        }
    }

    public void deactivate(SequenceMemory memory, ValueResolver valueResolver) {
        for (int i = 0; i < filterIndexes.length; i++) {
            memory.deactivateSignalAdapter(filterIndexes[i], signalAdapterIndexes[i]);
        }
        if (propagationTimer != null) {
            propagationTimer.failed(memory, valueResolver);
        }

        memory.resetLogicGateMemory(gateIndex, valueResolver);
    }

    /**
     * Timer attached to a gate that fires when the gate has been activated but not yet
     * matched within a deadline.  Used to implement {@code within(Duration, step)}.
     */
    public interface PropagationTimer {
        /** Called when the gate activates (step becomes active). */
        default void activated(SequenceMemory memory, ValueResolver valueResolver) {}

        /** Called when the gate matches (step fires). */
        default void matched(SequenceMemory memory, ValueResolver valueResolver) {
            // default: propagate immediately
        }

        /** Called when the gate deactivates without having matched (timeout or step reset). */
        default void failed(SequenceMemory memory, ValueResolver valueResolver) {}
    }

    /**
     * A {@link PropagationTimer} that schedules a timeout job when the gate activates.
     * If the gate matches before the deadline, the job is cancelled.
     * If the deadline fires first, the sequence fails.
     */
    public static class TimeoutTimer implements PropagationTimer {
        private final LogicGate gate;
        private final Timer     timer;

        public TimeoutTimer(LogicGate gate, Timer timer) {
            this.gate  = gate;
            this.timer = timer;
        }

        @Override
        public void activated(SequenceMemory memory, ValueResolver valueResolver) {
            Trigger trigger = timer.createTrigger(valueResolver.getTimerService().getCurrentTime(), null, null);
            LogicGateTimerJobContext ctx = new LogicGateTimerJobContext(trigger, valueResolver, gate, memory);
            JobHandle jobHandle = valueResolver.getTimerService().scheduleJob(LogicGateJob.getInstance(), ctx, trigger);
            memory.setJobHandle(gate.getGateIndex(), jobHandle);
        }

        @Override
        public void matched(SequenceMemory memory, ValueResolver valueResolver) {
            memory.cancelJobHandle(gate.getGateIndex(), valueResolver);
            gate.propagate(memory, valueResolver);
        }

        @Override
        public void failed(SequenceMemory memory, ValueResolver valueResolver) {
            memory.cancelJobHandle(gate.getGateIndex(), valueResolver);
        }
    }

    /**
     * The job executed by the timer service when a per-step timeout fires.
     */
    public static class LogicGateJob implements Job {
        private static final LogicGateJob INSTANCE = new LogicGateJob();

        public static LogicGateJob getInstance() {
            return INSTANCE;
        }

        @Override
        public void execute(JobContext ctx) {
            LogicGateTimerJobContext timerJobCtx = (LogicGateTimerJobContext) ctx;
            ValueResolver valueResolver = timerJobCtx.getValueResolver();
            valueResolver.addTimerAction(new LogicGateTimerAction(timerJobCtx));
        }
    }

    /**
     * Context passed to the {@link LogicGateJob} when a per-step timeout is scheduled.
     */
    public static class LogicGateTimerJobContext implements JobContext {
        private static final long serialVersionUID = 1L;

        private       JobHandle     jobHandle;
        private final Trigger       trigger;
        private final ValueResolver valueResolver;
        private final LogicGate     gate;
        private final SequenceMemory sequenceMemory;

        public LogicGateTimerJobContext(Trigger trigger, ValueResolver valueResolver,
                                        LogicGate gate, SequenceMemory sequenceMemory) {
            this.trigger        = trigger;
            this.valueResolver  = valueResolver;
            this.gate           = gate;
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

        public LogicGate getGate() {
            return gate;
        }

        public SequenceMemory getSequenceMemory() {
            return sequenceMemory;
        }
    }

    /**
     * Action executed on the session thread when a per-step timeout fires.
     */
    public static class LogicGateTimerAction implements ValueResolver.TimerAction {

        private final LogicGateTimerJobContext jobCtx;

        public LogicGateTimerAction(LogicGateTimerJobContext jobCtx) {
            this.jobCtx = jobCtx;
        }

        @Override
        public void execute(ValueResolver valueResolver) {
            LogicGate gate = jobCtx.getGate();
            SequenceMemory sequenceMemory = jobCtx.getSequenceMemory();

            // Clear the handle first — the job is firing, not being cancelled.
            sequenceMemory.clearJobHandle(gate.getGateIndex());

            // Match-monotonicity: if the gate already matched (step advanced), do nothing.
            if (!sequenceMemory.isLogicGateMatched(gate.getGateIndex())) {
                // Timeout: fail the sequence silently.
                sequenceMemory.getSequence().fail(sequenceMemory, valueResolver);
            }
        }
    }

}

package org.drools.base.reteoo.sequencing.signalprocessors;

import org.drools.base.base.ValueResolver;
import org.drools.base.phreak.actions.AbstractPropagationEntry;
import org.drools.base.time.JobHandle;
import org.drools.base.time.Trigger;
import org.drools.base.time.Timer;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit.LongBiPredicate;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.time.Job;
import org.drools.base.time.JobContext;

public class LogicGate extends SignalProcessor {
    protected long allMatched;

    private SignalProcessor output;

    private final LongBiPredicate predicate;

    private final int gateIndex;

    private final boolean vacuousMatchAtActivation;

    private final boolean statusCanRevert;

    private LogicGate[] inputGates = EMPTY_INPUT_GATES;

    private final int[] filterIndexes;

    private int[] signalAdapterIndexes;

    private static final LogicGate[] EMPTY_INPUT_GATES = new LogicGate[0];

    private static final ConditionalSignalCounter[] EMPTY_CONDITIONAL_SIGNAL_COUNTERS = new ConditionalSignalCounter[0];

    private ConditionalSignalCounter[] inputSignalCounters = EMPTY_CONDITIONAL_SIGNAL_COUNTERS;

    private PropagationTimer propagationTimer;

    public LogicGate(LongBiPredicate predicate, int gateIndex, int[] filterIndexes, int[] signalAdapterIndexes, int nbrOfInputGates) {
        this(predicate, gateIndex, filterIndexes, signalAdapterIndexes, nbrOfInputGates, false, false);
    }

    public LogicGate(LongBiPredicate predicate, int gateIndex, int[] filterIndexes, int[] signalAdapterIndexes, int nbrOfInputGates, boolean vacuousMatchAtActivation, boolean statusCanRevert) {
        if (vacuousMatchAtActivation && (filterIndexes.length + nbrOfInputGates) == 0) {
            throw new IllegalArgumentException(
                    "vacuousMatchAtActivation requires at least one input (filter or input gate)");
        }

        this.predicate = predicate;

        this.filterIndexes        = filterIndexes;
        this.signalAdapterIndexes = signalAdapterIndexes;

        for (int i = 0; i < (signalAdapterIndexes.length + nbrOfInputGates); i++) {
            allMatched = allMatched | (1L << i);
        }

        this.gateIndex                = gateIndex;
        this.vacuousMatchAtActivation = vacuousMatchAtActivation;
        this.statusCanRevert          = statusCanRevert;
    }

    public int getGateIndex() {
        return gateIndex;
    }

    public int[] getFilterIndexes() {
        return filterIndexes;
    }

    public PropagationTimer getPropagationTimer() {
        return propagationTimer;
    }

    public void setPropagationTimer(PropagationTimer propagationTimer) {
        this.propagationTimer = propagationTimer;
    }

    public int[] getSignalAdapterIndexes() {
        return signalAdapterIndexes;
    }

    public void setSignalAdapterIndexes(int[] signalAdapterIndexes) {
        this.signalAdapterIndexes = signalAdapterIndexes;
    }

    public LogicGate[] getInputGates() {
        return inputGates;
    }

    public void setInputGates(LogicGate... inputGates) {
        this.inputGates = inputGates;
    }

    public ConditionalSignalCounter[] getInputSignalCounters() {
        return inputSignalCounters;
    }

    public void setInputSignalCounters(ConditionalSignalCounter[] inputSignalCounters) {
        this.inputSignalCounters = inputSignalCounters;
    }

    public SignalProcessor getOutput() {
        return output;
    }

    public void setOutput(SignalProcessor output) {
        this.output = output;
    }

    @Override
    public void consume(SignalStatus signalStatus, SequenceMemory memory, ValueResolver valueResolver) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void consume(int signalBitIndex, SignalStatus signalStatus, SequenceMemory memory, ValueResolver valueResolver) {
        SignalStatus status = memory.getLogicGateSignalStatus(gateIndex);

        if (status == SignalStatus.FAILED) {
            throw new RuntimeException("Defensive Programming: LogicGate " + gateIndex + " failed");
        }

        SignalStatus priorStatus = status;

        long currentMatched = memory.getLogicGateMemory()[gateIndex];

        switch (signalStatus) {
            case MATCHED:
                currentMatched = currentMatched | (1L << (signalBitIndex - 1)); // ensures position is on, if it wasn't before. If it was on before, it remains on.
                break;
            case UNMATCHED:
                currentMatched = currentMatched & ~(1L << (signalBitIndex - 1)); // ensures position is off, if it wasn't before. If it was off before, it remains off.
                break;
        }

        memory.getLogicGateMemory()[gateIndex] = currentMatched;

        boolean matched = predicate.test(currentMatched, allMatched);

        if (matched) {
            status = SignalStatus.MATCHED;
        } else if (statusCanRevert) {
            status = SignalStatus.UNMATCHED;
        }

        memory.setLogicGateSignalStatus(gateIndex, status);
        if (priorStatus != status) {
            if (propagationTimer != null) {
                propagationTimer.matched(memory, valueResolver, status);
            } else {
                propagate(memory, valueResolver, status);
            }
        }
    }

    public void propagate(SequenceMemory memory, ValueResolver valueResolver, SignalStatus status) {
        resetPrior(memory, valueResolver);
        output.consume(status, memory, valueResolver);
    }

    public void resetPrior(SequenceMemory memory, ValueResolver valueResolver) {
        for (LogicGate gate : inputGates) {
            gate.reset(memory, valueResolver);
        }

        for (ConditionalSignalCounter counter : inputSignalCounters) {
            counter.reset(memory, valueResolver);
        }
    }

    public void reset(SequenceMemory memory, ValueResolver valueResolver) {
        resetPrior(memory, valueResolver);
        memory.resetLogicGateMemory(gateIndex, valueResolver);
        output.reset(memory, valueResolver);
    }

    public void activate(SequenceMemory memory, ValueResolver valueResolver) {
        if (memory.getLogicGateSignalStatus()[gateIndex] == null) {
            memory.getLogicGateSignalStatus()[gateIndex] = SignalStatus.UNMATCHED;
        }
        for (int i = 0; i < filterIndexes.length; i++) {
            memory.activateSignalAdapter(filterIndexes[i], this, signalAdapterIndexes[i], i + 1); // bit indexes start at 1
        }

        if (propagationTimer != null) {
            propagationTimer.activated(memory, valueResolver);
        }

        if (vacuousMatchAtActivation && predicate.test(0L, allMatched)) {
            memory.setLogicGateSignalStatus(gateIndex, SignalStatus.MATCHED);
            propagate(memory, valueResolver, SignalStatus.MATCHED);
        }
    }

    public void deactivate(SequenceMemory memory, ValueResolver valueResolver) {
        for (int i = 0; i < filterIndexes.length; i++) {
            memory.deactivateSignalAdapter(filterIndexes[i], this, signalAdapterIndexes[i]);
        }

        memory.resetLogicGateMemory(gateIndex, valueResolver);
    }

    public interface PropagationTimer {
        default void activated(SequenceMemory memory, ValueResolver valueResolver)  {

        }

        default void matched(SequenceMemory memory, ValueResolver valueResolver, SignalStatus status)  {

        }

        default void failed(SequenceMemory memory, ValueResolver valueResolver)  {

        }
    }

    public static class TimeoutTimer implements PropagationTimer {
        private final LogicGate gate;
        private final Timer     timer;

        public TimeoutTimer(LogicGate gate, Timer timer) {
            this.gate  = gate;
            this.timer = timer;
        }

        @Override
        public void activated(SequenceMemory memory, ValueResolver valueResolver)  {
            Trigger trigger = timer.createTrigger(valueResolver.getTimerService().getCurrentTime(), null, null);
            LogicGateTimerJobContext ctx = new LogicGateTimerJobContext(LogicGateTimerJobContext.TIMEOUT, trigger, valueResolver, gate, memory);
            JobHandle jobHandle = valueResolver.getTimerService().scheduleJob(LogicGateJob.getINSTANCE(), ctx, trigger);
            memory.setJobHandle(gate.getGateIndex(), jobHandle);
        }

        @Override
        public void matched(SequenceMemory memory, ValueResolver valueResolver, SignalStatus status)  {
            memory.cancelJobHandle(gate.getGateIndex(), valueResolver);
            gate.propagate(memory, valueResolver, status);
        }

        @Override
        public void failed(SequenceMemory memory, ValueResolver valueResolver)  {
            memory.cancelJobHandle(gate.getGateIndex(), valueResolver);
        }
    }

    public static class DelayFromActivatedTimer implements PropagationTimer  {
        private final LogicGate gate;
        private final Timer     timer;

        public DelayFromActivatedTimer(LogicGate gate, Timer timer) {
            this.gate  = gate;
            this.timer = timer;
        }

        @Override
        public void activated(SequenceMemory memory, ValueResolver valueResolver)  {
            Trigger trigger = timer.createTrigger(valueResolver.getTimerService().getCurrentTime(), null, null);
            LogicGateTimerJobContext ctx = new LogicGateTimerJobContext(LogicGateTimerJobContext.ARM_AFTER_EXPIRED, trigger, valueResolver, gate, memory);
            JobHandle jobHandle = valueResolver.getTimerService().scheduleJob(LogicGateJob.getINSTANCE(), ctx, trigger);
            memory.setJobHandle(gate.getGateIndex(), jobHandle);
        }

        @Override
        public void matched(SequenceMemory memory, ValueResolver valueResolver, SignalStatus status)  {
            if (memory.isGateArmed(gate.getGateIndex())) {
                gate.propagate(memory, valueResolver, status);
            }
            // else: gate not yet armed — drop this match.
        }

        @Override
        public void failed(SequenceMemory memory, ValueResolver valueResolver)  {
            memory.cancelJobHandle(gate.getGateIndex(), valueResolver);
        }
    }

    public static class DelayFromMatchTimer implements PropagationTimer  {
        private final LogicGate gate;
        private final Timer     timer;

        public DelayFromMatchTimer(LogicGate gate, Timer timer) {
            this.gate  = gate;
            this.timer = timer;
        }

        @Override
        public void activated(SequenceMemory memory, ValueResolver valueResolver)  {

        }

        @Override
        public void matched(SequenceMemory memory, ValueResolver valueResolver, SignalStatus status)  {
            memory.cancelJobHandle(gate.getGateIndex(), valueResolver);   // cancel any pending DELAY job before scheduling fresh
            Trigger trigger = timer.createTrigger(valueResolver.getTimerService().getCurrentTime(), null, null);
            LogicGateTimerJobContext ctx = new LogicGateTimerJobContext(LogicGateTimerJobContext.DELAY, trigger, valueResolver, gate, memory);
            JobHandle jobHandle = valueResolver.getTimerService().scheduleJob(LogicGateJob.getINSTANCE(), ctx, trigger);
            memory.setJobHandle(gate.getGateIndex(), jobHandle);
        }

        @Override
        public void failed(SequenceMemory memory, ValueResolver valueResolver)  {
            memory.cancelJobHandle(gate.getGateIndex(), valueResolver);
        }
    }

    public static class LogicGateJob
            implements
            Job {
        private static final LogicGateJob INSTANCE = new LogicGateJob();

        public static LogicGateJob getINSTANCE() {
            return INSTANCE;
        }

        public void execute(JobContext ctx) {
            LogicGateTimerJobContext timerJobCtx   = (LogicGateTimerJobContext) ctx;
            ValueResolver       resolver = timerJobCtx.getValueResolver();
            resolver.addPropagation( new LogicGateTimerAction(timerJobCtx ));
        }
    }

    public static class LogicGateTimerJobContext
            implements
            JobContext {
        private static final int DELAY   = 0;
        private static final int TIMEOUT = 1;
        private static final int ARM_AFTER_EXPIRED = 2;

        private       JobHandle     jobHandle;
        private final Trigger       trigger;
        private final ValueResolver valueResolver;

        private final LogicGate gate;
        private final SequenceMemory sequenceMemory;

        private final int actionType;

        public LogicGateTimerJobContext(int actionType, Trigger trigger, ValueResolver valueResolver, LogicGate gate, SequenceMemory sequenceMemory) {
            this.trigger         = trigger;
            this.valueResolver   = valueResolver;
            this.gate            = gate;
            this.sequenceMemory = sequenceMemory;
            this.actionType = actionType;
        }

        public JobHandle getJobHandle() {
            return this.jobHandle;
        }

        @Override
        public ValueResolver getValueResolver() {
            return valueResolver;
        }

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

        public int getActionType() {
            return actionType;
        }
    }

    public static class LogicGateTimerAction
            extends AbstractPropagationEntry {

        private final LogicGateTimerJobContext jobCtx;

        private LogicGateTimerAction( LogicGateTimerJobContext jobCtx) {
            this.jobCtx = jobCtx;
        }

        @Override
        public boolean requiresImmediateFlushing() {
            return true;
        }

        @Override
        public void internalExecute(final ValueResolver valueResolver) {
            execute( valueResolver, false );
        }

        public void execute( final ValueResolver valueResolver, boolean needEvaluation ) {
            LogicGate gate = jobCtx.getGate();
            SequenceMemory sequenceMemory = jobCtx.getSequenceMemory();

            SignalStatus status = sequenceMemory.getLogicGateSignalStatus(gate.getGateIndex());

            sequenceMemory.clearJobHandle(gate.getGateIndex(), valueResolver); // clear rather than cancel, as it's actually firing

            switch (jobCtx.getActionType()) {
                case LogicGateTimerJobContext.DELAY:
                    if (status == SignalStatus.MATCHED) {
                        // transition
                        gate.propagate(sequenceMemory, valueResolver, status);
                    }
                    // else: status reverted before delay expired — drop silently; gate keeps listening,
                    // next match schedules a fresh DELAY (see DelayFromMatchTimer.matched cancel-before-schedule).
                    break;
                case LogicGateTimerJobContext.TIMEOUT:
                    // fail, if not already transitioned
                    if (status != SignalStatus.MATCHED) {
                        // fail
                        sequenceMemory.getSequence().fail(sequenceMemory, valueResolver);
                    }
                    break;
                case LogicGateTimerJobContext.ARM_AFTER_EXPIRED:
                    sequenceMemory.resetLogicGateMemory(gate.getGateIndex(), valueResolver);
                    sequenceMemory.armGate(gate.getGateIndex());
                    // gate is now armed; next match propagates via DelayFromActivatedTimer.matched.
                    // No propagate, no fail — the gate listens passively for the next match.
                    break;
            }

            // Logic is satsified and waiting to transition
            // Logic is not satsified and has run out of time.
        }
    }
}

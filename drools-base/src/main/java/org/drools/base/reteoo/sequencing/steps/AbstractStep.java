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

    @Override
    public void childEnded(SequenceMemory parentMemory, ValueResolver valueResolver) {
        parentMemory.getSequence().next(parentMemory, valueResolver);
    }

    @Override
    public void childFailed(SequenceMemory parentMemory, ValueResolver valueResolver) {
        // The active child failed, so this sequence cannot continue: fail it too,
        // which routes to its own parent (or terminates the sequencer at the root).
        parentMemory.getSequencerMemory().getSequencer().failSequence(parentMemory, valueResolver);
    }

    /** Unlink a branch's currently-active step so it can never fire again. No-op if terminated. */
    protected static void tearDownBranch(SequenceMemory branchMem, ValueResolver valueResolver) {
        Sequence branchSeq = branchMem.getSequence();
        int step = branchMem.getStep();
        if (step < 0 || step >= branchSeq.getSteps().length) {
            return; // already terminated — nothing active to tear down
        }
        // LogicCircuitStep.deactivate() → LogicGate.deactivate() → unlinks this branch's adapters
        branchSeq.getSteps()[step].deactivate(branchMem, valueResolver);
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
            sequenceMemory.getSequencerMemory().getSequencer().failSequence(sequenceMemory, valueResolver);
        }
    }
}

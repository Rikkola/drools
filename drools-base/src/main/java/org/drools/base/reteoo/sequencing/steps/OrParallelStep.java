package org.drools.base.reteoo.sequencing.steps;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.SequencerMemory;
import org.drools.base.util.CircularArrayList;

import java.util.Arrays;

/**
 * Fork / first-wins join (OR-join). All branches start together when this step
 * activates; the parent advances past the step as soon as the FIRST branch
 * completes, and the remaining ("losing") branches are torn down so they can
 * never fire again.
 *
 * Mirrors {@link ParallelStep} (the AND-join) for activation; differs in the
 * join policy ({@link #childEnded}) and adds teardown ({@link #deactivate}).
 * The activation logic is duplicated from ParallelStep for now; if a third
 * parallel flavour appears, lift the shared parts into a common abstract base.
 */
public class OrParallelStep extends AbstractStep implements Step {
    private final SubsequenceStep[] subsequenceSteps;
    private final int               outputSize;

    public OrParallelStep(int index, int outputSize, Sequence parentSequence, SubsequenceStep... subsequenceSteps) {
        super(StepType.OR_PARALLEL, index, parentSequence);
        this.outputSize       = outputSize;
        this.subsequenceSteps = subsequenceSteps;
        Arrays.stream(subsequenceSteps).forEach(s -> {
            if (s.getSubsequence().getOutputSize() > outputSize) {
                throw new IllegalArgumentException("The subprocess output size must not be more than the parallel output size");
            }
        });
    }

    public SubsequenceStep[] getSubsequenceSteps() {
        return subsequenceSteps;
    }

    public int getOutputSize() {
        return outputSize;
    }

    @Override
    public void activate(SequenceMemory seqMem, ValueResolver valueResolver) {
        CircularArrayList<Object> data = seqMem.getData();
        SequencerMemory seqrMemory = seqMem.getSequencerMemory();
        data.addEmpty(outputSize);

        int memStartPos = data.size() - 1;
        data.addEmpty(subsequenceSteps.length); // element for each SequenceMemory

        for (int i = 0; i < this.subsequenceSteps.length; i++) {
            SequenceMemory subSequenceMemory = seqrMemory.getOrCreateSequenceMemory(seqMem, subsequenceSteps[i].getSubsequence(), i == 0 ? seqMem.getData() : null);
            data.set(memStartPos + i, subSequenceMemory);

            CircularArrayList<Object> subSeqData = subSequenceMemory.getData();
            subSeqData.addEmpty(outputSize);
            int eventsStartPosition = subSeqData.size() - (i == 0 ? subsequenceSteps.length : 0);
            subSequenceMemory.setEventsStartPosition(eventsStartPosition);
        }

        for (int i = 0; i < this.subsequenceSteps.length; i++) {
            subsequenceSteps[i].getSubsequence().start(seqrMemory.getSequenceMemory(subsequenceSteps[i].getSubsequence()), valueResolver);
        }
        seqMem.setCount(0); // OR-join flag: 0 = open, 1 = joined
    }

    @Override
    public void childEnded(SequenceMemory parentMemory, ValueResolver valueResolver) {
        if (parentMemory.getCount() != 0) {
            return; // already joined this step (defensive; the hardened filter pass prevents a second completion)
        }
        parentMemory.setCount(1);
        // advance once; next() will invoke this step's deactivate() below, which tears the branches down
        parentMemory.getSequence().next(parentMemory, valueResolver);
    }

    @Override
    public void deactivate(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
        SequencerMemory seqrMemory = sequenceMemory.getSequencerMemory();

        // Teardown: unlink every branch's currently-active step so losers can never fire again.
        // The winner branch has already terminated, so tearDownBranch is a no-op for it.
        for (SubsequenceStep branch : subsequenceSteps) {
            SequenceMemory branchMem = seqrMemory.getSequenceMemory(branch.getSubsequence());
            tearDownBranch(branchMem, valueResolver);
        }

        // Reset the data head, mirroring ParallelStep.deactivate.
        CircularArrayList<Object> data = sequenceMemory.getData();
        SequenceMemory firstBranchMemory = seqrMemory.getSequenceMemory(subsequenceSteps[0].getSubsequence());
        data.resetHeadByOffset(data.size() - firstBranchMemory.getEventsStartPosition());
    }

    private void tearDownBranch(SequenceMemory branchMem, ValueResolver valueResolver) {
        Sequence branchSeq = branchMem.getSequence();
        int step = branchMem.getStep();
        if (step < 0 || step >= branchSeq.getSteps().length) {
            return; // terminated (e.g. the winner) — nothing active to tear down
        }
        // LogicCircuitStep.deactivate() → LogicGate.deactivate() → unlinks this branch's adapters
        branchSeq.getSteps()[step].deactivate(branchMem, valueResolver);
    }
}

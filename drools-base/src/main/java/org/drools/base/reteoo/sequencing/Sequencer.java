package org.drools.base.reteoo.sequencing;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.steps.OrParallelStep;
import org.drools.base.reteoo.sequencing.steps.ParallelStep;
import org.drools.base.reteoo.sequencing.steps.SubsequenceStep;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.base.util.CircularArrayList;

import java.util.ArrayList;
import java.util.List;

public class Sequencer {

    private final Sequence     sequence;

    private final Sequence[] sequences;

    public Sequencer(Sequence sequence) {
        this.sequence = sequence;
        this.sequences = populateSequences(sequence, new ArrayList<>()).stream().toArray(Sequence[]::new);
    }

    public static List<Sequence> populateSequences(Sequence sequence, List<Sequence> list) {
        list.add(sequence);
        for (Step step  : sequence.getSteps()) {
            if (step instanceof SubsequenceStep) {
                populateSequences(((SubsequenceStep)step).getSubsequence(), list);
            } else if (step instanceof ParallelStep) {
                for (SubsequenceStep subseqStep : ((ParallelStep)step).getSubsequenceSteps()) {
                    populateSequences(subseqStep.getSubsequence(), list);
                }
            } else if (step instanceof OrParallelStep) {
                for (SubsequenceStep subseqStep : ((OrParallelStep)step).getSubsequenceSteps()) {
                    populateSequences(subseqStep.getSubsequence(), list);
                }
            }
        }

        return list;
    }

    public Sequence[] getSequences() {
        return sequences;
    }

    public void start(SequencerMemory memory, ValueResolver valueResolver) {
        SequenceMemory sequenceMemory = memory.getOrCreateSequenceMemory(null, sequence, memory.getData());
        memory.setChildSequenceMemory(sequenceMemory);
        sequence.start(sequenceMemory, valueResolver);
    }


    public void tips(SequencerMemory seqrMem, ValueResolver valueResolver) {
        Sequence seq = seqrMem.getSequencer().getSequence();
        SequenceMemory seqMem = seqrMem.getSequenceMemory(seq);
        CircularArrayList<Object> data =  seqMem.getData();
        Step step = seq.getSteps()[seqMem.getStep()];
        switch (step.getType()) {
            case SUB_SEQUENCE:
                SubsequenceStep subseqStep = (SubsequenceStep) step;
                subseqStep.getIndex();
            case PARALLEL:
        }
    }

    public void stop(SequenceMemory memory, ValueResolver valueResolver) {
        while (memory != null) {
            Sequence sequence = memory.getSequence();
            sequence.getSteps()[memory.getStep()].deactivate(memory, valueResolver);
            // Mark this sequence terminal: advance the step past the last index, mirroring how
            // DefaultController.next ends a completed sequence. getCurrentStep()'s leaf-walk treats
            // step >= steps.length as "no active leaf" and reports -1.
            memory.setStep(sequence.getSteps().length);
            memory = memory.getParent();
        }
    }

    /**
     * Fail one sequence level and route the failure to its parent's policy.
     * Mirrors DefaultController.end's parent routing: cancel any controller-held
     * timer, deactivate the active step, mark this level terminal (step == length,
     * the Option A contract), then hand off to the parent step's childFailed — or,
     * at the root, leave the sequencer terminal with no match (the rule does not fire).
     */
    public void failSequence(SequenceMemory memory, ValueResolver valueResolver) {
        Sequence sequence = memory.getSequence();
        sequence.getController().failed(memory, valueResolver);
        int step = memory.getStep();
        if (step >= 0 && step < sequence.getSteps().length) {
            sequence.getSteps()[step].deactivate(memory, valueResolver);
        }
        memory.setStep(sequence.getSteps().length); // terminal: getCurrentStep()'s leaf-walk reports -1

        SequenceMemory parent = memory.getParent();
        if (parent != null) {
            SequenceMemory parentSeqMemory = parent.getSequencerMemory().getSequenceMemory(parent.getSequence());
            Step parentStep = parentSeqMemory.getSequence().getSteps()[parentSeqMemory.getStep()];
            parentStep.childFailed(parentSeqMemory, valueResolver);
        }
        // parent == null → root failed: sequencer stays terminal, no match() is called.
    }

//    public void next(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
//        SequenceMemory sequenceMemory = sequencerMemory.getCurrentSequence();
//        if (sequenceMemory != null) {
//            sequenceMemory.getSequence().next(sequenceMemory, valueResolver);
//        } else {
//            // the root sequence has completed
//            sequencerMemory.match();
//        }
//    }

    public Sequence getSequence() {
        return sequence;
    }


}

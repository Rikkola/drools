package org.drools.base.reteoo.sequencing;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.steps.ParallelStep;
import org.drools.base.reteoo.sequencing.steps.SubsequenceStep;
import org.drools.base.reteoo.sequencing.steps.Step;

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

    public void stop(SequenceMemory memory, ValueResolver valueResolver) {
        while (memory != null) {
            memory.getSequence().getSteps()[memory.getStep()].deactivate(memory, valueResolver);
            memory = memory.getParent();
        }
    }

    public Sequence getSequence() {
        return sequence;
    }


}

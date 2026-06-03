package org.drools.mvel.integrationtests.phreak.sequencing;

import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.signalprocessors.Gates;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.reteoo.sequencing.signalprocessors.TerminatingSignalProcessor;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.base.rule.Pattern;
import org.drools.mvel.integrationtests.phreak.B;
import org.drools.mvel.integrationtests.phreak.C;
import org.drools.mvel.integrationtests.phreak.D;
import org.drools.mvel.integrationtests.phreak.E;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PhreakSequencerOrParallelTest extends AbstractPhreakSequencerSubsequenceTest {

    @BeforeEach
    public void setup() {
        initKBaseWithEmptyRule();
    }

    // A two-step branch: B (filter 0) then a distinct trigger (filter triggerFilterIndex).
    private Sequence twoStepBranch(int id, int triggerFilterIndex) {
        LogicGate g0 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask), 0,
                                     new int[] {0}, new int[] {0}, 0); // B
        g0.setOutput(TerminatingSignalProcessor.get());
        LogicGate g1 = new LogicGate((inputMask, sourceMask) -> Gates.and(inputMask, sourceMask), 1,
                                     new int[] {triggerFilterIndex}, new int[] {1}, 0); // trigger
        g1.setOutput(TerminatingSignalProcessor.getMatch());
        Sequence seq = new Sequence(id, Step.of(new LogicCircuit(g0)), Step.of(new LogicCircuit(g1)));
        seq.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});
        seq.setOutputSize(1);
        return seq;
    }

    // Branches with distinct triggers: B→C, B→D, B→E, joined by an OR step.
    private void buildDistinctTriggerOr() {
        seq1 = twoStepBranch(1, 1); // B then C
        seq2 = twoStepBranch(2, 2); // B then D
        seq3 = twoStepBranch(3, 3); // B then E
        seq0 = new Sequence(0, Step.anyOf(seq1, seq2, seq3));
        seq0.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});
        rule.addSequence(seq0);
        kbase.addPackage(pkg);
    }

    // Two branches with the SAME trigger: one C would complete both. The OR-join
    // must advance exactly once and not let the torn-down sibling fire on the same fact.
    @Test
    public void sameFactCompletingTwoBranchesAdvancesOnce() {
        seq1 = twoStepBranch(1, 1); // B then C
        seq2 = twoStepBranch(2, 1); // B then C  → both register a C adapter on the same filter
        seq0 = new Sequence(0, Step.anyOf(seq1, seq2));
        seq0.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});
        rule.addSequence(seq0);
        kbase.addPackage(pkg);

        createSession();
        SequenceMemory seq0Memory = sequencerMemory.getSequenceMemory(seq0);

        session.insert(new B(0, "b"));
        assertThat(sequencerMemory.getSequenceMemory(seq1).getStep()).isEqualTo(1);
        assertThat(sequencerMemory.getSequenceMemory(seq2).getStep()).isEqualTo(1);

        // One C satisfies BOTH branches' terminals in a single delivery.
        session.insert(new C(0, "c"));
        assertThat(seq0Memory.getCount()).isEqualTo(1);            // exactly one join, not two
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1); // terminated once
    }

    @Test
    public void firstBranchWinsAndParentAdvances() {
        buildDistinctTriggerOr();
        createSession();

        SequenceMemory seq0Memory = sequencerMemory.getSequenceMemory(seq0);
        assertThat(seq0Memory.getStep()).isEqualTo(0);   // parked on the OR step
        assertThat(seq0Memory.getCount()).isEqualTo(0);  // join flag open

        // B advances all three branches from step 0 to step 1
        session.insert(new B(0, "b"));
        assertThat(sequencerMemory.getSequenceMemory(seq1).getStep()).isEqualTo(1);
        assertThat(sequencerMemory.getSequenceMemory(seq2).getStep()).isEqualTo(1);
        assertThat(sequencerMemory.getSequenceMemory(seq3).getStep()).isEqualTo(1);
        assertThat(seq0Memory.getCount()).isEqualTo(0);  // none reached its terminal trigger

        // C completes branch 1 — first win: the parent advances past the OR step and terminates,
        // WITHOUT waiting for branches 2 and 3 (the AND-join would still be parked here at count 1).
        session.insert(new C(0, "c"));
        assertThat(seq0Memory.getCount()).isEqualTo(1);                // join flag set
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1);     // terminated on the first completion
        // the losing branches were still unfinished when the parent advanced
        assertThat(sequencerMemory.getSequenceMemory(seq2).getStep()).isEqualTo(1);
        assertThat(sequencerMemory.getSequenceMemory(seq3).getStep()).isEqualTo(1);

        // Teardown proof: the losing branches were unlinked, so their own triggers
        // arriving later are inert — no second join, parent stays terminated.
        session.insert(new D(0, "d"));
        session.insert(new E(0, "e"));
        assertThat(seq0Memory.getCount()).isEqualTo(1);
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1);
        assertThat(sequencerMemory.getSequenceMemory(seq2).getStep()).isEqualTo(1);
        assertThat(sequencerMemory.getSequenceMemory(seq3).getStep()).isEqualTo(1);
    }

    // Order independence: whichever branch reaches its terminal first wins.
    // Drive D first (branch 2) instead of C, and the outcome is identical.
    @Test
    public void anyBranchCanWinFirst() {
        buildDistinctTriggerOr();
        createSession();

        SequenceMemory seq0Memory = sequencerMemory.getSequenceMemory(seq0);

        session.insert(new B(0, "b"));
        session.insert(new D(0, "d")); // branch 2 (B→D) completes first
        assertThat(seq0Memory.getCount()).isEqualTo(1);
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1);
        assertThat(sequencerMemory.getSequenceMemory(seq1).getStep()).isEqualTo(1); // branch 1 (C) did not win
        assertThat(sequencerMemory.getSequenceMemory(seq3).getStep()).isEqualTo(1); // branch 3 (E) did not win
    }
}

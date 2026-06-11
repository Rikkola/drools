package org.drools.mvel.integrationtests.phreak.sequencing;

import java.util.concurrent.TimeUnit;

import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.Sequence.TimeoutController;
import org.drools.base.reteoo.sequencing.signalprocessors.Gates;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.reteoo.sequencing.signalprocessors.TerminatingSignalProcessor;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.base.rule.Pattern;
import org.drools.core.time.impl.DurationTimer;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.drools.mvel.integrationtests.phreak.B;
import org.drools.mvel.integrationtests.phreak.C;
import org.drools.mvel.integrationtests.phreak.D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PhreakSequencerChildFailedTest extends AbstractPhreakSequencerSubsequenceTest {

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

    private PseudoClockScheduler clock() {
        return (PseudoClockScheduler) session.getTimerService();
    }

    @Test
    public void orBranchFailsAndSiblingStillWins() {
        seq1 = twoStepBranch(1, 1); // B then C
        seq1.setController(new TimeoutController(new DurationTimer(1000)));
        seq2 = twoStepBranch(2, 2); // B then D
        seq0 = new Sequence(0, Step.anyOf(seq1, seq2));
        seq0.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});
        rule.addSequence(seq0);
        kbase.addPackage(pkg);

        createSession();
        SequenceMemory seq0Memory = sequencerMemory.getSequenceMemory(seq0);

        session.insert(new B(0, "b"));                    // both branches advance to step 1
        clock().advanceTime(2000, TimeUnit.MILLISECONDS); // branch 1 misses its 1s deadline -> fails
        session.fireAllRules();

        assertThat(seq0Memory.getCount()).isEqualTo(0);                 // not yet joined
        assertThat(getCurrentStep(sequencerMemory)).isNotEqualTo(-1);   // sequencer still live

        session.insert(new D(0, "d"));                    // branch 2 completes -> OR wins
        assertThat(seq0Memory.getCount()).isEqualTo(1);                 // joined via the survivor
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1);

        session.insert(new C(0, "c"));                    // branch 1 inert
        assertThat(seq0Memory.getCount()).isEqualTo(1);
    }

    @Test
    public void orAllBranchesFailAbortsTheRule() {
        seq1 = twoStepBranch(1, 1);
        seq1.setController(new TimeoutController(new DurationTimer(1000)));
        seq2 = twoStepBranch(2, 2);
        seq2.setController(new TimeoutController(new DurationTimer(1000)));
        seq0 = new Sequence(0, Step.anyOf(seq1, seq2));
        seq0.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});
        rule.addSequence(seq0);
        kbase.addPackage(pkg);

        createSession();
        SequenceMemory seq0Memory = sequencerMemory.getSequenceMemory(seq0);

        session.insert(new B(0, "b"));                    // both branches advance to step 1
        clock().advanceTime(2000, TimeUnit.MILLISECONDS); // both miss the deadline -> both fail
        session.fireAllRules();

        assertThat(seq0Memory.getCount()).isEqualTo(0);                 // never joined -> failed, not won
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1);      // sequencer terminal
    }

    @Test
    public void andBranchFailsFastTearingDownSiblings() {
        // AND of two branches: branch 1 (B→C) has a 1s deadline, branch 2 (B→D) has none.
        // Branch 1 missing its deadline must doom the whole AND immediately and tear down branch 2.
        seq1 = twoStepBranch(1, 1); // B then C
        seq1.setController(new TimeoutController(new DurationTimer(1000)));
        seq2 = twoStepBranch(2, 2); // B then D
        seq0 = new Sequence(0, Step.of(seq1, seq2)); // Step.of(Sequence...) → PARALLEL (AND-join)
        seq0.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});
        rule.addSequence(seq0);
        kbase.addPackage(pkg);

        createSession();
        SequenceMemory seq0Memory = sequencerMemory.getSequenceMemory(seq0);

        session.insert(new B(0, "b"));                    // both branches advance to step 1
        clock().advanceTime(2000, TimeUnit.MILLISECONDS); // branch 1 misses its deadline → fails
        session.fireAllRules();

        // Fail-fast: the AND can never complete, so the whole sequencer is terminal.
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1);
        assertThat(seq0Memory.getCount()).isLessThan(2);  // never reached all-branches-completed join

        // Branch 2 was torn down — its trigger arriving late is inert (no resurrection).
        session.insert(new D(0, "d"));
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1);
    }

    @Test
    public void nestedTimeoutInsideOrBranchLosesOnlyThatBranch() {
        // Branch 1 of the OR is itself sequence(innerSeq), where innerSeq (B→C) carries
        // the 1s deadline. The inner timeout must walk inner → branch1 → OR (default
        // SUB_SEQUENCE policy), and the OR must lose only branch 1.
        Sequence inner = twoStepBranch(3, 1); // B then C, with the deadline
        inner.setController(new TimeoutController(new DurationTimer(1000)));
        seq1 = new Sequence(1, Step.of(inner)); // SUB_SEQUENCE wrapping the timed inner sequence
        seq1.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});
        seq1.setOutputSize(1);

        seq2 = twoStepBranch(2, 2); // B then D, no deadline
        seq0 = new Sequence(0, Step.anyOf(seq1, seq2));
        seq0.setFilters(new Pattern[]{bpattern, cpattern, dpattern, epattern});
        rule.addSequence(seq0);
        kbase.addPackage(pkg);

        createSession();
        SequenceMemory seq0Memory = sequencerMemory.getSequenceMemory(seq0);

        session.insert(new B(0, "b"));                    // drives inner branch and branch 2 to step 1
        clock().advanceTime(2000, TimeUnit.MILLISECONDS); // inner misses the 1s deadline → fails
        session.fireAllRules();

        // The inner failure propagated up to the OR, which lost only branch 1.
        assertThat(seq0Memory.getCount()).isEqualTo(0);                 // OR still open
        assertThat(getCurrentStep(sequencerMemory)).isNotEqualTo(-1);

        session.insert(new D(0, "d"));                    // branch 2 still wins
        assertThat(seq0Memory.getCount()).isEqualTo(1);
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1);
    }
}

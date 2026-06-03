package org.drools.base.reteoo;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.signalprocessors.SignalProcessor;
import org.drools.base.reteoo.sequencing.signalprocessors.SignalStatus;
import org.drools.base.util.AbstractLinkedListNode;
import org.kie.api.runtime.rule.FactHandle;

public class SignalAdapter extends AbstractLinkedListNode<SignalAdapter> {
    private SignalProcessor output;
    private int             signalBitIndex;
    private SequenceMemory  memory;
    private boolean         linked;

    public boolean isLinked() {
        return linked;
    }

    public void setLinked(boolean linked) {
        this.linked = linked;
    }

    public SignalAdapter(SignalProcessor output, int signalBitIndex, SequenceMemory memory) {
        this.output         = output;
        this.signalBitIndex = signalBitIndex;
        this.memory         = memory;
    }

    public void receive(ValueResolver reteEvaluator, FactHandle factHandle) {
        memory.addData(factHandle);
        output.consume(signalBitIndex, SignalStatus.MATCHED, memory, reteEvaluator);
    }
}

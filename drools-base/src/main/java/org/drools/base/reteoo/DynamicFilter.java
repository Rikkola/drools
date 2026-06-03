package org.drools.base.reteoo;

import java.util.ArrayList;
import java.util.List;

import org.drools.base.base.ValueResolver;
import org.drools.base.rule.constraint.AlphaNodeFieldConstraint;
import org.drools.base.util.AbstractLinkedListNode;
import org.drools.base.util.LinkedList;
import org.kie.api.runtime.rule.FactHandle;

public class DynamicFilter extends AbstractLinkedListNode<DynamicFilter> {
    private AlphaNodeFieldConstraint  constraint;
    private LinkedList<SignalAdapter> signalAdapters;
    private int                       otnIndex;

    public DynamicFilter(DynamicFilterProto proto) {
        this.constraint     = proto.getConstraint();
        this.otnIndex       = proto.getOtnIndex();
        this.signalAdapters = new LinkedList<>();
    }

    public AlphaNodeFieldConstraint getConstraint() {
        return constraint;
    }

    public int getOtnIndex() {
        return otnIndex;
    }

    public void addSignalAdapter(SignalAdapter signalAdapter) {
        signalAdapter.setLinked(true);
        signalAdapters.add(signalAdapter);
    }

    public void removeSignalAdapter(SignalAdapter signalAdapter) {
        if (signalAdapter == null) {
            return;
        }
        signalAdapters.remove(signalAdapter);
        signalAdapter.setLinked(false);
    }

    public LinkedList<SignalAdapter> getSignalAdapters() {
        return signalAdapters;
    }

    public void assertObject(final FactHandle factHandle,
                             final ValueResolver valueResolver) {
        if (constraint.isAllowed(factHandle, valueResolver)) {
            // Snapshot first: receive() can unlink adapters mid-pass — not only the current
            // one (self-deactivation), but a SIBLING, when an OR-join tears down its losing
            // branches. Iterate the snapshot and skip any adapter unlinked earlier in this pass.
            List<SignalAdapter> snapshot = new ArrayList<>();
            for (SignalAdapter s = signalAdapters.getFirst(); s != null; s = s.getNext()) {
                snapshot.add(s);
            }
            for (SignalAdapter signal : snapshot) {
                if (signal.isLinked()) {
                    signal.receive(valueResolver, factHandle);
                }
            }
        }
    }
}

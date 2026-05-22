package org.drools.base.reteoo;

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
        signalAdapters.add(signalAdapter);
    }

    public void removeSignalAdapter(SignalAdapter signalAdapter) {
        signalAdapters.remove(signalAdapter);
    }

    public LinkedList<SignalAdapter> getSignalAdapters() {
        return signalAdapters;
    }

    public void assertObject(final FactHandle factHandle,
                             final ValueResolver valueResolver) {
        if (constraint.isAllowed(factHandle, valueResolver)) {
            for (SignalAdapter signal = signalAdapters.getFirst(); signal != null; signal = signal.getNext()) {
                signal.receive(valueResolver, factHandle);
            }
        }
    }
}

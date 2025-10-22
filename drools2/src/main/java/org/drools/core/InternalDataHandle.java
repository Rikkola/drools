package org.drools.core;

import org.drools.api.data.DataHandle;

public interface InternalDataHandle<T> extends DataHandle<T> {
    public void addLastLeftTuple(TupleImpl tuple);

    InternalDataHandle getLinkedFactHandle();

    void removeLeftTuple(TupleImpl tuple);
}

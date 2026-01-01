package org.drools.core;

import org.drools.api.data.DataHandle;

public interface InternalDataHandle<T> extends DataHandle<T> {
    void setObject(T t);

    void addLastLeftTuple(TupleImpl tuple);

    InternalDataHandle getLinkedFactHandle();

    void removeLeftTuple(TupleImpl tuple);
}

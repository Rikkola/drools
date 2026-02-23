package org.drools.core;

import org.drools.api.data.ObjectHandle;

public interface InternalDataHandle<T> extends ObjectHandle<T> {
    void setObject(T t);

    void addLastLeftTuple(TupleImpl tuple);

    InternalDataHandle getLinkedFactHandle();

    void removeLeftTuple(TupleImpl tuple);
}

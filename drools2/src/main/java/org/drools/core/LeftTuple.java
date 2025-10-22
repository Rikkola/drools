package org.drools.core;

public class LeftTuple extends TupleImpl {
    @Override
    public void reAdd() {

    }

    @Override
    public ObjectTypeNodeId getInputOtnId() {
        return null;
    }

    @Override
    public boolean isLeftTuple() {
        return false;
    }
}

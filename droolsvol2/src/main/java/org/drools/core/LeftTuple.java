package org.drools.core;

public class LeftTuple extends TupleImpl {

    public LeftTuple() {
        super();
    }

    public LeftTuple(DataHandleImpl handle, Sink sink, boolean leftTupleMemoryEnabled) {
        super(handle, sink, leftTupleMemoryEnabled);
    }

    public LeftTuple(DataHandleImpl factHandle, TupleImpl leftTuple, Sink sink) {
        super(factHandle, leftTuple, sink);
    }

    public LeftTuple(TupleImpl leftTuple, Sink sink, PropagationContext pctx, boolean leftTupleMemoryEnabled) {
        super(leftTuple, sink, pctx, leftTupleMemoryEnabled);
    }

    public LeftTuple(TupleImpl leftTuple, TupleImpl rightTuple, Sink sink) {
        super(leftTuple, rightTuple, sink);
    }

    public LeftTuple(TupleImpl leftTuple, TupleImpl rightTuple, TupleImpl currentLeftChild, TupleImpl currentRightChild, Sink sink, boolean leftTupleMemoryEnabled) {
        super(leftTuple, rightTuple, currentLeftChild, currentRightChild, sink, leftTupleMemoryEnabled);
    }

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

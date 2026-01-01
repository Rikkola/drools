package org.drools.core;

public class RightTuple extends TupleImpl {

    public RightTuple() {
    }

    public RightTuple(DataHandleImpl handle, Sink sink, boolean leftTupleMemoryEnabled) {
        super(handle, sink, leftTupleMemoryEnabled);
    }

    public RightTuple(DataHandleImpl factHandle, TupleImpl leftTuple, Sink sink) {
        super(factHandle, leftTuple, sink);
    }

    public RightTuple(TupleImpl leftTuple, Sink sink, PropagationContext pctx, boolean leftTupleMemoryEnabled) {
        super(leftTuple, sink, pctx, leftTupleMemoryEnabled);
    }

    public RightTuple(TupleImpl leftTuple, TupleImpl rightTuple, Sink sink) {
        super(leftTuple, rightTuple, sink);
    }

    public RightTuple(TupleImpl leftTuple, TupleImpl rightTuple, TupleImpl currentLeftChild, TupleImpl currentRightChild, Sink sink, boolean leftTupleMemoryEnabled) {
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

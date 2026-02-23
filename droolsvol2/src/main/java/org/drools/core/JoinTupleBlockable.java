package org.drools.core;

public class JoinTupleBlockable<T> extends JoinTuple<T>{
    private TupleImpl<T>          blocker;
    private JoinTupleBlockable    blockedPrevious;
    private JoinTupleBlockable    blockedNext;

    public JoinTupleBlockable(TupleImpl leftParent, TupleImpl<T> rightParent, NetworkNode node) {
        super(leftParent, rightParent, node);
    }

    public JoinTupleBlockable(TupleImpl leftParent, TupleImpl<T> rightParent, TupleImpl currentLeftChild, TupleImpl<T> currentRightChild, NetworkNode node) {
        super(leftParent, rightParent, currentLeftChild, currentRightChild, node);
    }

    public void clearBlocker() {
        this.blockedPrevious = null;
        this.blockedNext = null;
        this.blocker = null;
    }


    @Override
    public void setBlocker(TupleImpl blocker) {
        this.blocker = blocker;
    }

    @Override
    public TupleImpl getBlocker() {
        return this.blocker;
    }

    @Override
    public JoinTupleBlockable getBlockedPrevious() {
        return this.blockedPrevious;
    }

    @Override
    public void setBlockedPrevious(JoinTupleBlockable blockerPrevious) {
        this.blockedPrevious = blockerPrevious;
    }

    @Override
    public JoinTupleBlockable getBlockedNext() {
        return this.blockedNext;
    }

    @Override
    public void setBlockedNext(JoinTupleBlockable blockerNext) {
        this.blockedNext = blockerNext;
    }
}

package org.drools.core;

import org.drools.api.data.ObjectHandle;

public class JoinTuple<T> extends LeftTuple<T> {
    private ObjectHandle<T> handle;

    private TupleImpl<T> parent;

    private TupleImpl rightParent;
    private TupleImpl rightParentPrevious;
    private TupleImpl rightParentNext;

    public JoinTuple(TupleImpl leftParent, TupleImpl<T> rightParent,
                     NetworkNode node) {
        super(leftParent, node);
        this.parent = leftParent.getNextParentWithHandle();
        this.handle = rightParent.getHandle();
        this.rightParent = rightParent;

        // insert at the end of the list
        addToRightParent(rightParent);
    }

    public JoinTuple(TupleImpl leftParent, TupleImpl<T> rightParent,
                     TupleImpl currentLeftChild, TupleImpl<T> currentRightChild,
                     NetworkNode node) {
        super(leftParent, currentLeftChild, node);
        this.handle = rightParent.getHandle();
        this.rightParent = rightParent;

        if( currentRightChild == null ) {
            // insert at the end of the list
            addToRightParent(rightParent);
        } else {
            // insert before current child
            this.rightParentNext = currentRightChild;
            this.rightParentPrevious = currentRightChild.getRightParentPrevious();
            currentRightChild.setRightParentPrevious( this );
            if( this.rightParentPrevious == null ) {
                this.rightParent.setFirstChild( this );
            } else {
                this.rightParentPrevious.setRightParentNext( this );
            }
        }
    }

    private void addToRightParent(TupleImpl<T> rightParent) {
        if (rightParent.getLastChild() != null ) {
            this.rightParentPrevious = rightParent.getLastChild();
            this.rightParentPrevious.setRightParentNext( this );
        } else {
            rightParent.setFirstChild(this);
        }
        rightParent.setLastChild(this);
    }

    @Override
    public T get() {
        return handle.get();
    }

    @Override
    public T getObject() {
        return handle.get();
    }

    @Override
    public ObjectHandle<T> getObjectHandle() {
        return handle;
    }

    public ObjectHandle getHandle() {
        return handle;
    }

    @Override
    public TupleImpl getParent() {
        return parent;

    }

    @Override
    public TupleImpl getNextParentWithHandle() {
        return this;
    }

    @Override
    public TupleImpl getRightParent() {
        return this.rightParent;
    }

    @Override
    public void setRightParent(TupleImpl rightParent){
        this.rightParent = rightParent;
    }

    @Override
    public TupleImpl getRightParentPrevious() {
        return this.rightParentPrevious;
    }

    @Override
    public void setRightParentPrevious(TupleImpl rightParentPrevious) {
        this.rightParentPrevious = rightParentPrevious;
    }

    @Override
    public TupleImpl getRightParentNext() {
        return this.rightParentNext;
    }

    @Override
    public void setRightParentNext(TupleImpl rightParentNext){
        this.rightParentNext = rightParentNext;
    }

    @Override
    public boolean hasRightParent() {
        return true;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null) {return false;}
        if (!super.equals(o)) {return false;}

        JoinTuple<?> joinTuple = (JoinTuple<?>) o;
        return getHandle().equals(joinTuple.getHandle());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getHandle().hashCode();
        return result;
    }
}

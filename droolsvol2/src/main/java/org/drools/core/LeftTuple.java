package org.drools.core;

public class LeftTuple<T> extends TupleImpl<T> {
    /**
     * The left parent linked list
     */
    private TupleImpl leftParent;
    private TupleImpl leftPrevious;
    private TupleImpl leftNext;


    public LeftTuple() {
        super();
    }

    public LeftTuple(TupleImpl leftParent,
                     NetworkNode node) {
        setNetworkNode(node);
        this.leftParent = leftParent;

        addToLeftParent(leftParent);
    }

    public LeftTuple(TupleImpl leftParent,
                     TupleImpl currentLeftChild,
                     NetworkNode node) {
        setNetworkNode(node);
        this.leftParent = leftParent;

        if( currentLeftChild == null ) {
            // insert at the end of the list
            addToLeftParent(leftParent);
        } else {
            // insert before current child
            leftNext = currentLeftChild;
            leftPrevious = currentLeftChild.getLeftPrevious();
            currentLeftChild.setLeftPrevious( this );
            if( leftPrevious == null ) {
                this.leftParent.setFirstChild(this);
            } else {
                leftPrevious.setLeftNext( this );
            }
        }
    }

    private void init(TupleImpl leftParent, NetworkNode node) {
        setNetworkNode(node);

    }

    @Override
    public TupleImpl getParent() {
        return leftParent.getParent();
    }

    @Override
    public TupleImpl getNextParentWithHandle() {
        return leftParent.getNextParentWithHandle();
    }

    @Override
    public void reAdd() {

    }

    private void addToLeftParent(TupleImpl leftParent) {
        if (leftParent.getLastChild() != null ) {
            leftPrevious = leftParent.getLastChild();
            leftPrevious.setLeftNext( this );
        } else {
            leftParent.setFirstChild(this);
        }
        leftParent.setLastChild(this);
    }



    public void removeFromLeftParent() {
        TupleImpl currentPrevious = getLeftPrevious();
        TupleImpl currentNext     = getLeftNext();

        if ( currentPrevious != null && currentNext != null ) {
            //remove  from middle
            leftPrevious.setLeftNext( currentNext );
            leftNext.setLeftPrevious( currentPrevious );
        } else if ( currentNext != null ) {
            //remove from first
            leftParent.setFirstChild( currentNext );
            currentNext.setLeftPrevious(null);
        } else if ( currentPrevious != null ) {
            //remove from end
            leftParent.setLastChild( currentPrevious );
            currentPrevious.setLeftNext(null);
        } else {
            // single remaining item, no previous or next
            this.leftParent.setFirstChild( null );
            this.leftParent.setLastChild( null );
        }

        this.leftPrevious = null;
        this.leftNext = null;
    }

    @Override
    public ObjectTypeNodeId getInputOtnId() {
        return null;
    }

    @Override
    public boolean isLeftTuple() {
        return false;
    }

    @Override
    public TupleImpl getLeftPrevious() {
        return leftPrevious;
    }

    @Override
    public void setLeftPrevious(TupleImpl leftPrevious) {
        this.leftPrevious = leftPrevious;
    }

    @Override
    public TupleImpl getLeftNext() {
        return leftNext;
    }

    @Override
    public void setLeftNext(TupleImpl leftNext) {
        this.leftNext = leftNext;
    }


    @Override
    public boolean hasLeftParent() {
        return true;
    }

    @Override
    public TupleImpl getLeftParent() {
        return leftParent;
    }
}

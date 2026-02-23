package org.drools.core;

import static org.drools.core.ObjectHandleTupleBlocker.extracted;

public class JoinTupleBlocker<T> extends JoinTuple<T>{
    private JoinTupleBlockable            blocked;

    public JoinTupleBlocker(TupleImpl leftParent, TupleImpl<T> rightParent, NetworkNode node, LeftTuple blocked) {
        super(leftParent, rightParent, node);
    }

    public JoinTupleBlocker(TupleImpl leftParent, TupleImpl<T> rightParent, TupleImpl currentLeftChild, TupleImpl<T> currentRightChild, NetworkNode node, LeftTuple blocked) {
        super(leftParent, rightParent, currentLeftChild, currentRightChild, node);
    }

    @Override
    public void addBlocked(JoinTupleBlockable newBlocked) {
        extracted(newBlocked, blocked);
        blocked = newBlocked;
    }

    @Override
    public void removeBlocked(JoinTupleBlockable leftTuple) {
        JoinTupleBlockable previous =  leftTuple.getBlockedPrevious();
        JoinTupleBlockable next =  leftTuple.getBlockedNext();
        if ( previous != null && next != null ) {
            //remove  from middle
            previous.setBlockedNext( next );
            next.setBlockedPrevious( previous );
        } else if ( next != null ) {
            //remove from first
            this.blocked = next;
            next.setBlockedPrevious( null );
        } else if ( previous != null ) {
            //remove from end
            previous.setBlockedNext( null );
        } else {
            this.blocked = null;
        }
        leftTuple.clearBlocker();
    }
}

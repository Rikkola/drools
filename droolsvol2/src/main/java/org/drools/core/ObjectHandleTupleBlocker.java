package org.drools.core;

public class ObjectHandleTupleBlocker<T> extends ObjectHandleTuple<T> {
    private JoinTupleBlockable blocked;

    public ObjectHandleTupleBlocker(ObjectHandleImpl<T> handle, NetworkNode node) {
        super(handle, node);
    }


    @Override
    public boolean isLeftTuple() {
        return false;
    }

    @Override
    public void addBlocked(JoinTupleBlockable newBlocked) {
        extracted(newBlocked, blocked);
        blocked = newBlocked;
    }

    public static void extracted(JoinTupleBlockable newBlocked, JoinTupleBlockable existingBlocked) {
        if ( existingBlocked != null && newBlocked != null ) {
            newBlocked.setBlockedNext(existingBlocked);
            existingBlocked.setBlockedPrevious(newBlocked);
        }
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

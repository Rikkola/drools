/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.core;

import org.drools.base.rule.Declaration;
import org.drools.core.util.DoubleLinkedNode;
import org.drools.core.util.index.TupleList;
import org.kie.api.runtime.rule.FactHandle;

import java.util.Arrays;

public abstract class RootTupleImpl<T> implements DoubleLinkedNode<RootTupleImpl<T>> { //Tuple<TupleImpl<T>> {
    private static final long          serialVersionUID = 540l;

    private            int           index;

    protected DataHandleImpl<T> handle;


    /**
     * The children linked list
     */
    private RootTupleImpl firstChild;
    private RootTupleImpl lastChild;

    /**
     * Node memory linked list
     */
    private RootTupleImpl<T> previous;
    private RootTupleImpl<T> next;

    private Object contextObject;

    private PropagationContext propagationContext;

    private Sink sink;

    private boolean expired;

    // node memory
    protected TupleList memory;


    public RootTupleImpl() {
        // constructor needed for serialisation
    }

    public RootTupleImpl(DataHandleImpl<T> handle,
                         Sink sink,
                         boolean leftTupleMemoryEnabled) {
        setSink(sink);
        this.handle = handle;
//        if ( leftTupleMemoryEnabled ) {
//            handle.addLastLeftTuple( this );
//        }
    }

    public RootTupleImpl(DataHandleImpl<T> factHandle,
                         RootTupleImpl leftTuple,
                         Sink sink) {
        setSink(sink);
        this.handle = factHandle;
        this.index = leftTuple.getIndex() + 1;
    }

    public RootTupleImpl(RootTupleImpl leftTuple,
                         Sink sink,
                         PropagationContext pctx,
                         boolean leftTupleMemoryEnabled) {
        setSink(sink);
        this.index = leftTuple.getIndex() + 1;
        this.parent = leftTuple.getNextParentWithHandle();
        this.leftParent = leftTuple;
        setPropagationContext( pctx );

        if ( leftTupleMemoryEnabled ) {
            if ( leftTuple.getLastChild() != null ) {
                this.handlePrevious = leftTuple.getLastChild();
                this.handlePrevious.setHandleNext( this );
            } else {
                leftTuple.setFirstChild( this );
            }
            leftTuple.setLastChild( this );
        }
    }

    public RootTupleImpl(RootTupleImpl leftTuple,
                         RootTupleImpl<T> rightTuple,
                         Sink sink) {
        setSink(sink);
        this.index = leftTuple.getIndex() + 1;
        this.parent = leftTuple.getNextParentWithHandle();
        this.leftParent = leftTuple;
        this.rightParent = rightTuple;

        this.handle = rightTuple.getFactHandle();
        setPropagationContext( rightTuple.getPropagationContext() );

        // insert at the end of the list
        if ( leftTuple.getLastChild() != null ) {
            this.handlePrevious = leftTuple.getLastChild();
            this.handlePrevious.setHandleNext( this );
        } else {
            leftTuple.setFirstChild( this );
        }
        leftTuple.setLastChild( this );

        // insert at the end of the list
        if ( rightTuple.getLastChild() != null ) {
            this.rightParentPrevious = rightTuple.getLastChild();
            this.rightParentPrevious.setRightParentNext( this );
        } else {
            rightTuple.setFirstChild( this );
        }
        rightTuple.setLastChild( this );
    }


    public Object getObject(Declaration declaration) {
        return getObject(declaration.getTupleIndex());
    }

    public Object getContextObject() {
        return this.contextObject;
    }

    public final void setContextObject( final Object contextObject ) {
        this.contextObject = contextObject;
    }

    public RootTupleImpl getFirstChild() {
        return firstChild;
    }

    public void setFirstChild(RootTupleImpl firstChild) {
        this.firstChild = firstChild;
    }

    public RootTupleImpl getLastChild() {
        return lastChild;
    }

    public void setLastChild(RootTupleImpl lastChild) {
        this.lastChild = lastChild;
    }

    public RootTupleImpl getRightParent() {
        throw new UnsupportedOperationException();
    }

    public void setRightParent(RootTupleImpl rightParent) {
    }

    public RootTupleImpl getRightParentPrevious() {
        throw new UnsupportedOperationException();
    }

    public void setRightParentPrevious(RootTupleImpl rightParentLeft) {
        throw new UnsupportedOperationException();
    }

    public RootTupleImpl getRightParentNext() {
        throw new UnsupportedOperationException();
    }

    public void setRightParentNext(RootTupleImpl rightParentRight) {
        throw new UnsupportedOperationException();
    }

    public T get() {
        return handle.get();
    }

    public DataHandleImpl get(int index) {
        RootTupleImpl entry =  this;
        while ( entry.getIndex() != index) {
            entry = entry.getParent();
        }
        return entry.getFactHandle();
    }

    public FactHandle[] toFactHandles() {
        // always use the count of the node that created join (not the sink target)
        FactHandle[]  handles = new FactHandle[((LeftTupleSinkNode)getSink()).getLeftTupleSource().getObjectCount()];
        RootTupleImpl entry   =  skipEmptyHandles();
        for(int i = handles.length-1; i >= 0; i--) {
            handles[i] = entry.getFactHandle();
            entry = entry.getParent();
        }
        return handles;
    }

    public Object[] toObjects(boolean reverse) {
        // always use the count of the node that created join (not the sink target)
        Object[]      objs  = new Object[((LeftTupleSinkNode)getSink()).getLeftTupleSource().getObjectCount()];
        RootTupleImpl entry =  skipEmptyHandles();

        if (!reverse) {
            for (int i = objs.length - 1; i >= 0; i--) {
                objs[i] = entry.getFactHandle().getObject();
                entry = entry.getParent();
            }
        } else {
            for (int i = 0; i < objs.length; i++) {
                objs[i] = entry.getFactHandle().getObject();
                entry = entry.getParent();
            }
        }

        return objs;
    }

    public void clearBlocker() {
        throw new UnsupportedOperationException();
    }

    public void setBlocker(RightTuple blocker) {
        throw new UnsupportedOperationException();
    }

    public RightTuple getBlocker() {
        throw new UnsupportedOperationException();
    }

    public LeftTuple getBlockedPrevious() {
        throw new UnsupportedOperationException();
    }

    public void setBlockedPrevious(LeftTuple blockerPrevious) {
        throw new UnsupportedOperationException();
    }

    public LeftTuple getBlockedNext() {
        throw new UnsupportedOperationException();
    }

    public void setBlockedNext(LeftTuple blockerNext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();

        RootTupleImpl entry = skipEmptyHandles();;
        while ( entry != null ) {
            //buffer.append( entry.handle );
            buffer.append(entry.getFactHandle());
            if ( entry.getParent() != null ) {
                buffer.append("\n");
            }
            entry = entry.getParent();
        }
        return buffer.toString();
    }

    @Override
    public int hashCode() {
        return getFactHandle() == null ? 0 : getFactHandle().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        RootTupleImpl other =  (RootTupleImpl) object;

        // A AbstractTuple is  only the same if it has the same hashCode, factId and parent
        if ( this.hashCode() != other.hashCode() || getFactHandle() != other.getFactHandle() ) {
            return false;
        }
    }

    public int size() {
        return this.index + 1;
    }

    public RootTupleImpl getSubTuple(final int elements) {
        RootTupleImpl entry = this;
        if ( elements <= this.size() ) {
            final int lastindex = elements - 1;

            while ( entry.getIndex() != lastindex ) {
                // This uses getLeftParent, instead of getParent, as the subnetwork tuple
                // parent could be any node
                entry = entry.getParent();
            }
        }
        return entry;
    }

    public RootTupleImpl getParent() {
        return null;
    }

    protected String toExternalString() {
        StringBuilder builder = new StringBuilder();
        builder.append( String.format( "%08X", System.identityHashCode( this ) ) ).append( ":" );
        long[]        ids   = new long[this.index+1];
        RootTupleImpl entry = skipEmptyHandles();;
        while( entry != null ) {
            ids[entry.getIndex()] = entry.getFactHandle().getId();
            entry = entry.getParent();
        }
        builder.append(Arrays.toString(ids))
               .append( " sink=" )
               .append( this.getSink().getClass().getSimpleName() )
               .append( "(" ).append( getSink().getId() ).append( ")" );
        return  builder.toString();
    }

    @Override
    public void clear() {
        this.memory = null;
    }

    public DataHandleImpl<T> getFactHandle() {
        return handle;
    }

    public DataHandleImpl getHandle() {
        return handle;
    }

    public void setFactHandle( DataHandleImpl handle ) {
        this.handle = handle;
    }

    public PropagationContext getPropagationContext() {
        return propagationContext;
    }

    public void setPropagationContext(PropagationContext propagationContext) {
        this.propagationContext = propagationContext;
    }

    public RootTupleImpl getPrevious() {
        return previous;
    }

    public void setPrevious(RootTupleImpl previous) {
        this.previous = previous;
    }

    public RootTupleImpl getNext() {
        return next;
    }

    public void setNext(RootTupleImpl next) {
        this.next = next;
    }

    public FactHandle get(Declaration declaration) {
        return get(declaration.getTupleIndex());
    }

    public RootTupleImpl getTuple(int index) {
        RootTupleImpl entry = this;
        while ( entry.getIndex() != index) {
            entry = entry.getParent();
        }
        return entry;
    }

    public RootTupleImpl getRootTuple() {
        return getTuple(0);
    }

    public RootTupleImpl skipEmptyHandles() {
        // because getParent now only returns a tuple that as an FH, we only need to cheeck the current tuple,
        // and not the parent chain
        return getFactHandle() == null ? getParent() : this;
    }

    public RootTupleImpl getLeftParent() {
        return null;
    }

    public void setLeftParent(RootTupleImpl leftParent) {
        throw new UnsupportedOperationException();
    }

    public RootTupleImpl getNextParentWithHandle() {
        // if parent is null, then we are LIAN
        return (handle!=null) ? this : parent != null ? parent.getNextParentWithHandle() : this;
    }

    public abstract void reAdd();

    public void reAddLeft() {
        // The parent can never be the FactHandle (root AbstractTuple) as that is handled by reAdd()
        // make sure we aren't already at the end
        if ( this.handleNext != null ) {
            if ( this.handlePrevious != null ) {
                // remove the current AbstractTuple from the middle of the chain
                this.handlePrevious.setHandleNext( this.handleNext );
                this.handleNext.setHandlePrevious( this.handlePrevious );
            } else {
                if( this.leftParent.getFirstChild() == this ) {
                    // remove the current AbstractTuple from start start of the chain
                    this.leftParent.setFirstChild( getHandleNext() );
                }
                this.handleNext.setHandlePrevious( null );
            }
            // re-add to end
            this.handlePrevious = this.leftParent.getLastChild();
            this.handlePrevious.setHandleNext( this );
            this.leftParent.setLastChild( this );
            this.handleNext = null;
        }
    }

    public void reAddRight() {
        // make sure we aren't already at the end
        if ( this.rightParentNext != null ) {
            if ( this.rightParentPrevious != null ) {
                // remove the current AbstractTuple from the middle of the chain
                this.rightParentPrevious.setRightParentNext( this.rightParentNext );
                this.rightParentNext.setRightParentPrevious( this.rightParentPrevious );
            } else {
                if( this.rightParent.getFirstChild() == this ) {
                    // remove the current AbstractTuple from the start of the chain
                    this.rightParent.setFirstChild( this.rightParentNext );
                }
                this.rightParentNext.setRightParentPrevious( null );
            }
            // re-add to end
            this.rightParentPrevious = this.rightParent.getLastChild();
            this.rightParentPrevious.setRightParentNext( this );
            this.rightParent.setLastChild( this );
            this.rightParentNext = null;
        }
    }

    public void unlinkFromLeftParent() {
        RootTupleImpl previousParent = getHandlePrevious();
        RootTupleImpl nextParent     = getHandleNext();

        if ( previousParent != null && nextParent != null ) {
            //remove  from middle
            this.handlePrevious.setHandleNext( nextParent );
            this.handleNext.setHandlePrevious( previousParent );
        } else if ( nextParent != null ) {
            //remove from first
            if ( this.leftParent != null ) {
                this.leftParent.setFirstChild( nextParent );
            } else {
                // This is relevant to the root node and only happens at rule removal time
                getFactHandle().removeLeftTuple( this );
            }
            nextParent.setHandlePrevious( null );
        } else if ( previousParent != null ) {
            //remove from end
            if ( this.leftParent != null ) {
                this.leftParent.setLastChild( previousParent );
            } else {
                // relevant to the root node, as here the parent is the FactHandle, only happens at rule removal time
                getFactHandle().removeLeftTuple( this );
            }
            previousParent.setHandleNext( null );
        } else {
            // single remaining item, no previous or next
            if( leftParent != null ) {
                this.leftParent.setFirstChild( null );
                this.leftParent.setLastChild( null );
            } else {
                // it is a root tuple - only happens during rule removal
                getFactHandle().removeLeftTuple( this );
            }
        }

        this.handlePrevious = null;
        this.handleNext = null;
    }

    public void unlinkFromRightParent() {
        doUnlinkFromRightParent();
    }

    public void doUnlinkFromRightParent() {
        if ( this.rightParent == null ) {
            // no right parent;
            return;
        }

        RootTupleImpl previousParent = this.rightParentPrevious;
        RootTupleImpl nextParent     = this.rightParentNext;

        if ( previousParent != null && nextParent != null ) {
            // remove from middle
            this.rightParentPrevious.setRightParentNext( this.rightParentNext );
            this.rightParentNext.setRightParentPrevious( this.rightParentPrevious );
        } else if ( nextParent != null ) {
            // remove from the start
            this.rightParent.setFirstChild( nextParent );
            nextParent.setRightParentPrevious( null );
        } else if ( previousParent != null ) {
            // remove from end
            this.rightParent.setLastChild( previousParent );
            previousParent.setRightParentNext( null );
        } else {
            // single remaining item, no previous or next
            this.rightParent.setFirstChild( null );
            this.rightParent.setLastChild( null );
        }

        this.rightParentPrevious = null;
        this.rightParentNext = null;
    }

    public int getIndex() {
        return this.index;
    }

    /* Had to add the set method because sink adapters must override
     * the tuple sink set when the tuple was created.
     */
    public void setLeftTupleSink( LeftTupleSink sink ) {
        setSink(sink);
    }

    public RootTupleImpl getHandlePrevious() {
        return handlePrevious;
    }

    public void setHandlePrevious(RootTupleImpl handlePrevious) {
        this.handlePrevious = handlePrevious;
    }

    public RootTupleImpl getHandleNext() {
        return handleNext;
    }

    public void setHandleNext(RootTupleImpl handleNext) {
        this.handleNext = handleNext;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired() {
        this.expired = true;
    }

    public Sink getSink() {
        return sink;
    }

    protected void setSink(Sink sink) {
        this.sink = sink;
    }

    public TupleList getMemory() {
        return this.memory;
    }

    public void setMemory(TupleList memory) {
        this.memory = memory;
    }

    public void initPeer(RootTupleImpl original, Sink sink) {
        this.index = original.index;
        this.parent = original.parent;
        this.leftParent = original.leftParent;

        setFactHandle( original.getFactHandle() );
        setPropagationContext( original.getPropagationContext() );
        setSink(sink);
    }

    public <O> O getObject(int index) {
        return (O) get(index).getObject();
    }


    public T getObject() {
        return handle.getObject();
    }

    public abstract ObjectTypeNodeId getInputOtnId();


    public InternalDataHandle getFactHandleForEvaluation() {
        throw new UnsupportedOperationException("Only RightTupleImpl implements this");
    }

    public boolean isStagedOnRight() {
        return false;
    }

//    public Collection<Object> getAccumulatedObjects() {
//        if (getFirstChild() == null) {
//            return Collections.emptyList();
//        }
//        Collection<Object> result = new ArrayList<>();
//        if ( getContextObject() instanceof AccumulateNode.AccumulateContext ) {
//            for (TupleImpl child = getFirstChild(); child != null; child = child.getHandleNext()) {
//                result.add(child.getContextObject());
//            }
//        }
//
//        if ( getFirstChild().getRightParent().isSubnetworkTuple()) {
//            TupleImpl leftParent = getFirstChild().getRightParent().getLeftParent();
//            result.addAll( leftParent.getAccumulatedObjects() );
//        }
//        return result;
//    }

    public abstract boolean isLeftTuple();

    public boolean isFullMatch() {
        return false;
    }

    public boolean isSubnetworkTuple() {
        return false;
    }
}

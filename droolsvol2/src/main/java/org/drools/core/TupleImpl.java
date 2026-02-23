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

import org.drools.api.data.ObjectHandle;
import org.drools.base.rule.Declaration;
import org.drools.core.util.AbstractDoubleLinkedNode;
import org.kie.api.runtime.rule.FactHandle;

public class TupleImpl<T> extends AbstractDoubleLinkedNode<TupleImpl<T>> { //Tuple<TupleImpl<T>> {
    private static final long          serialVersionUID = 540l;

    /**
     * The children linked list
     */
    private TupleImpl firstChild;
    private TupleImpl lastChild;

    private NetworkNode node;

    private TupleMemory<T> memory;

    public TupleImpl() {
        // constructor needed for serialisation
    }

    public TupleImpl(NetworkNode node) {
        this.node = node;
    }

    public Object getObject(Declaration declaration) {
        return getObject(declaration.getTupleIndex());
    }

    public TupleImpl getFirstChild() {
        return firstChild;
    }

    public void setFirstChild(TupleImpl firstChild) {
        this.firstChild = firstChild;
    }

    public TupleImpl getLastChild() {
        return lastChild;
    }

    public void setLastChild(TupleImpl lastChild) {
        this.lastChild = lastChild;
    }

    public TupleImpl getRightParent() {
        throw new UnsupportedOperationException();
    }

    public void setRightParent(TupleImpl rightParent){
        throw new UnsupportedOperationException();
    }

    public TupleImpl getRightParentPrevious() {
        throw new UnsupportedOperationException();
    }

    public void setRightParentPrevious(TupleImpl rightParentPrevious) {
        throw new UnsupportedOperationException();
    }

    public TupleImpl getRightParentNext() {
        throw new UnsupportedOperationException();
    }

    public void setRightParentNext(TupleImpl rightParentNext){
        throw new UnsupportedOperationException();
    }

    public T get() {
        throw new UnsupportedOperationException();
    }

    public ObjectHandle get(int index) {
        TupleImpl entry =  this;
        while ( entry.getNetworkNode().getPathIndex() != index) {
            entry = entry.getParent();
        }
        return entry.getObjectHandle();
    }

    public FactHandle[] toFactHandles() {
//        // always use the count of the node that created join (not the sink target)
//        FactHandle[] handles = new FactHandle[((LeftTupleSinkNode)getSink()).getLeftTupleSource().getObjectCount()];
//        TupleImpl    entry   =  skipEmptyHandles();
//        for(int i = handles.length-1; i >= 0; i--) {
//            handles[i] = entry.getObjectHandle();
//            entry = entry.getParent();
//        }
//        return handles;
        return null;
    }

    public Object[] toObjects(boolean reverse) {
//        // always use the count of the node that created join (not the sink target)
//        Object[]  objs  = new Object[((LeftTupleSinkNode)getSink()).getLeftTupleSource().getObjectCount()];
//        TupleImpl entry =  skipEmptyHandles();
//
//        if (!reverse) {
//            for (int i = objs.length - 1; i >= 0; i--) {
//                objs[i] = entry.getObjectHandle().getObject();
//                entry = entry.getParent();
//            }
//        } else {
//            for (int i = 0; i < objs.length; i++) {
//                objs[i] = entry.getObjectHandle().getObject();
//                entry = entry.getParent();
//            }
//        }
//
//        return objs;
        return null;
    }

    public void clearBlocker() {
        throw new UnsupportedOperationException();
    }

    public void setBlocker(TupleImpl blocker) {
        throw new UnsupportedOperationException();
    }

    public TupleImpl getBlocker() {
        throw new UnsupportedOperationException();
    }

    public JoinTupleBlockable getBlockedPrevious() {
        throw new UnsupportedOperationException();
    }

    public void setBlockedPrevious(JoinTupleBlockable blockerPrevious) {
        throw new UnsupportedOperationException();
    }

    public JoinTupleBlockable getBlockedNext() {
        throw new UnsupportedOperationException();
    }

    public void setBlockedNext(JoinTupleBlockable blockerNext) {
        throw new UnsupportedOperationException();
    }

    public TupleImpl getBlocked() {
        throw new UnsupportedOperationException();
    }

    public void setBlocked(TupleImpl leftTuple) {
        throw new UnsupportedOperationException();
    }

    public void addBlocked(JoinTupleBlockable leftTuple) {
        throw new UnsupportedOperationException();
    }

    public void removeBlocked(JoinTupleBlockable leftTuple) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(getClass().getSimpleName() + "[");

        TupleImpl entry = hasRightParent() ? this : skipEmptyHandles();
        buffer.append("index=").append(getNetworkNode().getPathIndex()).append(", ");
        buffer.append("handles=[");
        while ( entry != null ) {
            if (entry.hasRightParent()) {
                buffer.append(entry.getRightParent());
            } else {
                buffer.append(entry.getObjectHandle());
            }
            if ( entry.getParent() != null ) {
                buffer.append(", ");
            }
            entry = entry.getParent();
        }
        buffer.append("]");
        buffer.append("]");
        return buffer.toString();
    }

    @Override
    public int hashCode() {
        return getObjectHandle() == null ? 0 : getObjectHandle().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        TupleImpl<?> tuple = (TupleImpl<?>) o;
        return node.equals(tuple.node);
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public TupleImpl getSubTuple(final int elements) {
        TupleImpl entry = this;
        if ( elements <= this.size() ) {
            final int lastindex = elements - 1;

            while ( entry.getNetworkNode().getPathIndex() != lastindex ) {
                // This uses getLeftParent, instead of getParent, as the subnetwork tuple
                // parent could be any node
                entry = entry.getParent();
            }
        }
        return entry;
    }

    public TupleImpl getParent() {
        throw new UnsupportedOperationException();
    }

    protected String toExternalString() {
//        StringBuilder builder = new StringBuilder();
//        builder.append( String.format( "%08X", System.identityHashCode( this ) ) ).append( ":" );
//        long[]    ids   = new long[getIndex()+1];
//        TupleImpl entry = skipEmptyHandles();;
//        while( entry != null ) {
//            ids[entry.getIndex()] = entry.getObjectHandle().getId();
//            entry = entry.getParent();
//        }
//        builder.append(Arrays.toString(ids))
//               .append( " sink=" )
//               .append( this.getSink().getClass().getSimpleName() )
//               .append( "(" ).append( getSink().getId() ).append( ")" );
//        return  builder.toString();
        return "";
    }

    @Override
    public void clear() {
        this.memory = null;
    }

    public ObjectHandle<T> getObjectHandle() {
        return null;
    }

    public ObjectHandle getHandle() {
        return null;
    }

    public void setObjectHandle( ObjectHandle handle) {
        throw new UnsupportedOperationException();
    }

    public FactHandle get(Declaration declaration) {
        return get(declaration.getTupleIndex());
    }

    public TupleImpl getTuple(int pathIndex) {
        TupleImpl entry = this;
        while ( entry.getNetworkNode().getPathIndex() != pathIndex) {
            entry = entry.getParent();
        }
        return entry;
    }

    public TupleImpl getRootTuple() {
        return getTuple(0);
    }

    public TupleImpl skipEmptyHandles() {
        // because getParent now only returns a tuple that as an FH, we only need to cheeck the current tuple,
        // and not the parent chain
        return getObjectHandle() == null ? getParent() : this;
    }

    public TupleImpl getLeftParent() {
        throw new UnsupportedOperationException();
    }

    public boolean hasLeftParent() {
        return false;
    }

    public boolean hasRightParent() {
        return false;
    }

    public void setLeftParent(TupleImpl leftParent) {
        throw new UnsupportedOperationException();
    }

    public TupleImpl getNextParentWithHandle() {
        throw new UnsupportedOperationException();
    }

    public void reAdd() {
        throw new UnsupportedOperationException();
    }

    public void reAddLeft() {
        throw new UnsupportedOperationException();
    }

    public void reAddRight() {
        throw new UnsupportedOperationException();
    }

    public void removeFromLeftParent() {
        throw new UnsupportedOperationException();
    }

    public void removeFromRightParent() {
        throw new UnsupportedOperationException();
    }

    public TupleImpl getLeftPrevious() {
        throw new UnsupportedOperationException();
    };

    public void setLeftPrevious(TupleImpl leftPrevious){
        throw new UnsupportedOperationException();
    };

    public TupleImpl getLeftNext(){
        throw new UnsupportedOperationException();
    };

    public void setLeftNext(TupleImpl leftNext){
        throw new UnsupportedOperationException();
    };

    public NetworkNode getNetworkNode() {
        return node;
    }

    protected void setNetworkNode(NetworkNode node) {
        this.node = node;
    }

    public TupleMemory<T> getMemory() {
        return this.memory;
    }

    public void setMemory(TupleMemory<T> memory) {
        this.memory = memory;
    }

    public <O> O getObject(int index) {
        return (O) get(index).getObject();
    }


    public T getObject() {
        throw new UnsupportedOperationException();
    }

    public ObjectTypeNodeId getInputOtnId() {
        throw new UnsupportedOperationException();
    }


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

    public boolean isLeftTuple() {
        throw new UnsupportedOperationException();
    }

    public boolean isFullMatch() {
        return false;
    }

    public boolean isSubnetworkTuple() {
        return false;
    }
}

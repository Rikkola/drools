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
package org.drools.core.util.index;


import org.drools.core.InternalDataHandle;
import org.drools.core.TupleImpl;
import org.drools.core.TupleMemory;
import org.drools.core.util.FastIterator;
import org.drools.core.util.LinkedList;

public class TupleList<T> extends LinkedList<TupleImpl<T>> implements TupleMemory<T> {

    public static final long       serialVersionUID = 510l;

    private TupleList<T>           next;

    public TupleList() {
    }

    public TupleList(TupleImpl<T> first, TupleImpl<T> last, int size) {
        super(first, last, size);
    }

    @Override
    public void add(TupleImpl<T> node) {
        super.add(node);
        node.setMemory(this);
    }

    @Override
    public void remove(TupleImpl<T> node) {
        super.remove(node);
        node.clear();
    }

    @Override
    public TupleImpl<T> getFirst1(TupleImpl<T> tuple) {
        return getFirst();
    }



    public TupleImpl<T> get(final InternalDataHandle<T> handle) {
        TupleImpl<T> current = getFirst();
        while ( current != null ) {
            if ( handle == current.getObjectHandle() ) {
                return current;
            }
            current = current.getNext();
        }
        return null;
    }

    public TupleImpl<T> removeFirst() {
        TupleImpl<T> node = super.removeFirst();
        node.clear();
        return node;
    }

    public TupleImpl<T> removeLast() {
        TupleImpl<T> node = super.removeLast();
        node.clear();
        return node;
    }

    @Override
    public IndexType getIndexType() {
        return TupleMemory.IndexType.NONE;
    }

    @Override
    public FastIterator<TupleImpl<T>> fastIterator() {
        return LinkedList.fastIterator; // contains no state, so ok to be static
    }

    @Override
    public FastIterator<TupleImpl<T>> fullFastIterator() {
        return LinkedList.fastIterator; // contains no state, so ok to be static
    }

    @Override
    public FastIterator<TupleImpl<T>> fullFastIterator(TupleImpl tuple) {
        return LinkedList.fastIterator; // contains no state, so ok to be static
    }

    @Override
    public boolean isIndexed() {
        return false;
    }

    public TupleList<T> getNext() {
        return this.next;
    }

    public void setNext(final TupleList<T> next) {
        this.next = next;
    }

    public String toString() {
        StringBuilder              builder = new StringBuilder();
        FastIterator<TupleImpl<T>> it      = super.fastIterator();
        for (TupleImpl<T> tuple = getFirst(); tuple != null; tuple = it.next(tuple) ) {
            builder.append(tuple).append("\n");
        }

        return builder.toString();
    }

    protected void copyStateInto(TupleList<T> other) {
        super.copyStateInto(other);

        for (TupleImpl<T> current = getFirst(); current != null; current = current.getNext() ) {
            current.setMemory(other);
        }
    }
}

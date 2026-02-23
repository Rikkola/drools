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
package org.drools.core.util;

import org.drools.base.util.IndexedValueReader;
import org.drools.core.TupleImpl;
import org.drools.core.util.index.TupleList;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public abstract class AbstractHashTable<T>
    implements
    Externalizable {
    static final int           MAX_CAPACITY = 1 << 30;

    public static final int    PRIME            = 31;

    protected int              size;
    protected int              threshold;
    protected float            loadFactor;

    protected TupleList<T>[]      table;

    private HashTableIterator<T>  iterator;

    public AbstractHashTable() {
        this( 16,
              0.75f );
    }

    public AbstractHashTable(final int capacity,
                             final float loadFactor) {
        this.loadFactor = loadFactor;
        this.threshold = (int) (capacity * loadFactor);
        this.table = new TupleList[capacity];
    }

    public AbstractHashTable(final TupleList[] table) {
        this( 0.75f,
              table );
    }

    public AbstractHashTable(final float loadFactor,
                             final TupleList[] table) {
        this.loadFactor = loadFactor;
        this.threshold = (int) (table.length * loadFactor);
        this.table = table;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        size = in.readInt();
        threshold = in.readInt();
        loadFactor = in.readFloat();
        table = (TupleList<T>[]) in.readObject();
        iterator = (HashTableIterator<T>) in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt( size );
        out.writeInt( threshold );
        out.writeFloat( loadFactor );
        out.writeObject( table );
        out.writeObject( iterator );
    }

    public Iterator<T> iterator() {
        if ( this.iterator == null ) {
            this.iterator = new HashTableIterator<>( this );
        } else {
            this.iterator.reset();
        }
        return this.iterator;
    }

    public Iterator<T> newIterator() {
        return new HashTableIterator<>( this );
    }

    public void ensureCapacity(int itemsToBeAdded) {
        int newCapacity = this.size + itemsToBeAdded;
        if (newCapacity > this.threshold) {
            int newSize = this.table.length * 2;
            while (newSize < newCapacity) {
                newSize *= 2;
            }
            resize(newSize);
        }
    }

    protected void resize(final int newCapacity) {
        final TupleList[] oldTable = this.table;
        final int oldCapacity = oldTable.length;
        if ( oldCapacity == AbstractHashTable.MAX_CAPACITY ) {
            this.threshold = Integer.MAX_VALUE;
            return;
        }

        final TupleList<T>[] newTable = new TupleList[newCapacity];

        for ( int i = 0; i < this.table.length; i++ ) {
            TupleList<T> entry = this.table[i];
            if ( entry == null ) {
                continue;
            }
            this.table[i] = null;
            while ( entry != null ) {
                TupleList<T> next = entry.getNext();
                                
                // we must use getResizeHashcode as some sub classes cache the hashcode and some don't
                // otherwise we end up rehashing a cached hashcode that has already been rehashed.
                final int index = indexOf(  getResizeHashcode( entry ),
                                            newTable.length );
                
                entry.setNext( newTable[index] );
                newTable[index] = entry;

                entry = next;
            }
        }

        this.table = newTable;
        this.threshold = (int) (newCapacity * this.loadFactor);
    }
    
    public abstract int getResizeHashcode(TupleList<T> entry);

    public TupleList<T>[] getTable() {
        return this.table;
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }
    
    private static int rehash(int hash) {
        hash ^= (hash >>> 20) ^ (hash >>> 12);
        return hash ^ (hash >>> 7) ^ (hash >>> 4);
    }     

    protected static int indexOf(final int hashCode,
                          final int dataSize) {
        return hashCode & (dataSize - 1);
    }
    
    @Override
    public String toString() {
        StringBuilder sbuilder = new StringBuilder();
        Iterator it = newIterator();
        boolean isFirst = true;
        for (TupleList entry = ( TupleList ) it.next(); entry != null; entry = ( TupleList ) it.next() ) {
            sbuilder.append( entry );
            if ( !isFirst ) {
                sbuilder.append( ", " );
            }
            isFirst = false;
        }
        
        return sbuilder.toString();
    }

    public interface Index extends Externalizable {
        IndexedValueReader getFieldIndex(int index);
        HashEntry hashCodeOf(TupleImpl tuple, boolean left);
    }

    public static class IndexTupleList<T> extends TupleList<T> implements HashEntry {
        private HashEntry hashEntry;
        private Index index;
        private int hashCode;

        public IndexTupleList( Index index, HashEntry hashEntry ) {
            this.index = index;
            this.hashEntry = hashEntry;
            this.hashCode = hashEntry.hashCode();
        }

        @Override
        public boolean equals(final Object object) {
            if (!(object instanceof IndexTupleList)) {
                return false;
            }
            final IndexTupleList other = (IndexTupleList) object;
            return this.hashCode == other.hashCode && this.index == other.index;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }

        @Override
        protected void copyStateInto(TupleList other) {
            super.copyStateInto(other);
            ( (IndexTupleList) other ).hashEntry = hashEntry;
            ( (IndexTupleList) other ).index = index;
            ( (IndexTupleList) other ).hashCode = hashCode;
        }

        public HashEntry getHashEntry() {
            return hashEntry;
        }

        @Override
        public HashEntry clone() {
            throw new UnsupportedOperationException();
        }
    }

    public void clear() {
        this.table = new TupleList[Math.min( this.table.length,
                                         16 )];
        this.threshold = (int) (this.table.length * this.loadFactor);
        this.size = 0;
        this.iterator = null;
    }

    public interface HashEntry {
        HashEntry clone();
    }

}

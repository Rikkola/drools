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

import org.drools.core.util.AbstractHashTable.Index;
import org.drools.core.util.FastIterator;

public interface TupleMemory<T> {

    default Index getIndex() {
        return null;
    }

    enum IndexType {
        NONE, EQUAL, COMPARISON, RANGE;

        public boolean isComparison() {
            return this == COMPARISON || this == RANGE;
        }
    }

    /**
     * The FactHandle is always the context fact and is necessary when the object being modified is in the both left and right
     * node memories. This is because the memory on the opposite side would not have yet memory.removeAdd the fact, so it
     * could potentially be in the wrong bucket. So the bucket matches check always checks to ignore the first facthandle if it's
     * the same as the context fact.
     */

    TupleImpl<T> getFirst();

    default TupleImpl<T> getFirstN(TupleImpl<T> tuple) {
        return getFirst();
    }

    default TupleImpl<T> getFirst1(TupleImpl<T> tuple1) {
        return getFirst();
    }

    default TupleImpl<T> getFirst2(TupleImpl<T> tuple1, TupleImpl<T> tuple2) {
        return getFirst();
    }

    default TupleImpl<T> getFirst3(TupleImpl<T> tuple1, TupleImpl<T> tuple2, TupleImpl<T> tuple3) {
        return getFirst();
    }

    default TupleImpl<T> getFirst4(TupleImpl<T> tuple1, TupleImpl<T> tuple2, TupleImpl<T> tuple3, TupleImpl<T> tuple4) {
        return getFirst();
    }
    
    void removeAdd(TupleImpl<T> tuple);

    void add(TupleImpl<T> tuple);

    void remove(TupleImpl<T> tuple);

    boolean isIndexed();

    int size();

//    Iterator<TupleImpl<T>> iterator();

    FastIterator<TupleImpl<T>> fastIterator();

    /**
     * Iterates the entire data structure, regardless of whether TupleMemory is hashed or not.
     * @return
     */
    FastIterator<TupleImpl<T>> fullFastIterator();

    /**
     * Iterator that resumes from the current RightTuple, regardless of whether the TupleMemory is hashed or not 
     * @param tuple
     * @return
     */
    FastIterator<TupleImpl<T>> fullFastIterator(TupleImpl<T> tuple);

    IndexType getIndexType();

    void clear();
}

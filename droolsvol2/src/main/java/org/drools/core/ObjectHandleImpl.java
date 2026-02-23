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

public class ObjectHandleImpl<T> extends TupleImpl<T> implements ObjectHandle<T> {

    private final long id;
    private T object;
    private int hashCode;

    public ObjectHandleImpl(long id, T object, int hashCode, NetworkNode node) {
        super(node);
        this.id = id;
        this.object = object;
    }

    public ObjectHandleImpl(long id, T object, NetworkNode node) {
        super(node);
        this.id = id;
        this.object = object;
        this.hashCode = object.hashCode();
    }

    @Override
    public TupleImpl getParent() {
        return null; // ObjectHandle's are root and have no parent
    }

    @Override
    public TupleImpl getNextParentWithHandle() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ObjectHandleImpl that = (ObjectHandleImpl) o;
        return id == that.id;
    }

    @Override
    public T getObject() {
        return object;
    }

    public T get() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    public long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "ObjectHandleImpl[" +
               "id=" + id +
               ", object=" + object +
               ", hashCode=" + hashCode +
               ']';
    }

    @Override
    public ObjectHandle<T> getObjectHandle() {
        return this;
    }

    @Override
    public ObjectHandle getHandle() {
        return this;
    }

    @Override
    public boolean isNegated() {
        return false;
    }

    @Override
    public boolean isEvent() {
        return false;
    }

    @Override
    public long getRecency() {
        return 0;
    }

    @Override
    public Object as(Class klass) throws ClassCastException {
        return null;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public String toExternalForm() {
        return "";
    }

    public void removeLeftTuple(TupleImpl<T> t) {

    }

    public void addLeftTuple(TupleImpl<T> t) {
        if (getLastChild() != null ) {
            TupleImpl<T> lastChild = getLastChild();
            lastChild.setLeftNext( t );
        } else {
            setFirstChild(t);
        }
        setLastChild(t);
    }

    @Override
    public boolean hasLeftParent() {
        return false;
    }

    @Override
    public boolean hasRightParent() {
        return false;
    }
}

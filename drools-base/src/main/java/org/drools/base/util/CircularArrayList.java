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
package org.drools.base.util;

import java.lang.reflect.Array;
import java.util.Arrays;

public class CircularArrayList<E> {
    private E[] array;
    private int head = 0;
    private int capacity;
    private Class cls;

    public CircularArrayList(int capacity) {
        this(Object.class, capacity);
    }

    public CircularArrayList(Class cls, int capacity) {
        this.capacity = capacity;
        this.array = (E[]) new Object[capacity];
        this.cls = cls;
    }

    public void resetHeadByOffset(int offset) {
        head = head-offset;
    }

    public boolean set(int index, E e) {
        array[index % capacity] = e;
        return true;
    }

    public boolean add(E e) {
            array[head++ % capacity] = e;
            return true;
    }

    public void addEmpty(int size) {
        Arrays.fill(array, head, head+size, null);
        head = head + size;
    }

    public E getHead() {
        return array[(head-1) % capacity];
    }

    public E getHeadMinus(int i) {
        return array[(head-1-i) % capacity];
    }

    public E fastGet(int index) {
        return array[index % capacity];
    }

    public E get(int index) {
        if (index < head - capacity ) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + capacity);
        }

        return array[index % capacity];
    }

    public int size() {
        return head;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public E[] toArray() {
        int headIndex = head % capacity;
        E[] trg;
        if (head < array.length) {
            trg = (E[]) Array.newInstance(cls, head);
            System.arraycopy(array, 0, trg, 0, headIndex);
        } else {
            trg = (E[]) Array.newInstance(cls, array.length);
            System.arraycopy(array, headIndex, trg, 0, array.length-headIndex);
            System.arraycopy(array, 0, trg, array.length-headIndex, headIndex);
        }

        return trg;
    }

    @Override
    public String toString() {
        return "CircularArrayList{" +
               "array=" + Arrays.toString(array) +
               ", head=" + head +
               ", capacity=" + capacity +
               '}';
    }
}

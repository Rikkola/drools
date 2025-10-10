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
package org.drools.core.reteoo;

import org.drools.base.rule.Declaration;
import org.drools.core.common.InternalFactHandle;
import org.kie.api.runtime.rule.FactHandle;

/**
 * BiLinearTuple represents a tuple that combines facts from two separate left input networks.
 * This specialized tuple enables cross-network variable resolution for BiLinearJoinNode,
 * allowing constraints to reference variables from both input networks.
 * 
 * The tuple maintains references to both source network tuples and provides unified
 * variable access across networks through enhanced Declaration resolution.
 */
public class BiLinearTuple extends TupleImpl {
    
    private static final long serialVersionUID = 540l;
    
    /** First network tuple (primary left input) */
    private final TupleImpl firstNetworkTuple;
    
    /** Second network tuple (secondary left input) */ 
    private final TupleImpl secondNetworkTuple;
    
    /** Offset for second network tuple indices to avoid conflicts */
    private final int secondNetworkOffset;
    
    /**
     * Creates a BiLinearTuple combining tuples from two networks
     * 
     * @param firstNetworkTuple Tuple from the first left input network
     * @param secondNetworkTuple Tuple from the second left input network  
     * @param rightFactHandle Right input fact handle (may be null for some scenarios)
     * @param sink The sink node for this tuple
     */
    public BiLinearTuple(TupleImpl firstNetworkTuple,
                        TupleImpl secondNetworkTuple,
                        InternalFactHandle rightFactHandle,
                        Sink sink) {
        super(rightFactHandle, sink, false);
        
        
        this.firstNetworkTuple = firstNetworkTuple;
        this.secondNetworkTuple = secondNetworkTuple;
        
        // Calculate offset to avoid tuple index conflicts
        // Second network indices start after first network's highest index
        this.secondNetworkOffset = firstNetworkTuple != null ? firstNetworkTuple.size() : 0;
        
        // Set the combined size
        int firstSize = firstNetworkTuple != null ? firstNetworkTuple.size() : 0;
        int secondSize = secondNetworkTuple != null ? secondNetworkTuple.size() : 0;
        int rightSize = rightFactHandle != null ? 1 : 0;
        
        // Note: Cannot set index directly as it's private in parent class
        // Will override getIndex() method to return correct value
    }
    
    /**
     * Enhanced get method that resolves declarations across both networks
     */
    @Override
    public FactHandle get(Declaration declaration) {
        return get(declaration.getTupleIndex());
    }
    
    /**
     * Enhanced get method that resolves indices across both networks
     * 
     * Index mapping:
     * - 0 to firstNetworkSize-1: First network facts
     * - firstNetworkSize to firstNetworkSize+secondNetworkSize-1: Second network facts  
     * - firstNetworkSize+secondNetworkSize: Right fact (if present)
     */
    @Override
    public FactHandle get(int index) {
        int firstSize = firstNetworkTuple != null ? firstNetworkTuple.size() : 0;
        int secondSize = secondNetworkTuple != null ? secondNetworkTuple.size() : 0;
        
        // First network range
        if (index < firstSize) {
            return firstNetworkTuple.get(index);
        }
        
        // Second network range  
        if (index < firstSize + secondSize) {
            int secondNetworkIndex = index - firstSize;
            return secondNetworkTuple.get(secondNetworkIndex);
        }
        
        // Right fact
        if (index == firstSize + secondSize && this.handle != null) {
            return this.handle;
        }
        
        throw new IndexOutOfBoundsException("Tuple index " + index + " is out of bounds. " +
            "First network size: " + firstSize + ", Second network size: " + secondSize + 
            ", Has right fact: " + (this.handle != null));
    }
    
    /**
     * Enhanced getObject method for cross-network object access
     */
    @Override
    public Object getObject(Declaration declaration) {
        return getObject(declaration.getTupleIndex());
    }
    
    /**
     * Enhanced getObject method for cross-network object access
     */
    @Override
    public Object getObject(int index) {
        FactHandle handle = get(index);
        return handle != null ? handle.getObject() : null;
    }
    
    /**
     * Returns the total size across both networks plus right fact
     */
    @Override
    public int size() {
        int firstSize = firstNetworkTuple != null ? firstNetworkTuple.size() : 0;
        int secondSize = secondNetworkTuple != null ? secondNetworkTuple.size() : 0;
        int rightSize = this.handle != null ? 1 : 0;
        return firstSize + secondSize + rightSize;
    }
    
    /**
     * Override getIndex to return the correct index for BiLinearTuple
     */
    @Override
    public int getIndex() {
        return size() - 1;
    }
    
    /**
     * Enhanced toObjects that combines objects from both networks
     */
    @Override
    public Object[] toObjects() {
        return toObjects(false);
    }
    
    /**
     * Enhanced toObjects with reverse option
     */
    @Override
    public Object[] toObjects(boolean reverse) {
        int totalSize = size();
        Object[] objects = new Object[totalSize];
        
        int pos = 0;
        
        // Add first network objects
        if (firstNetworkTuple != null) {
            Object[] firstObjects = firstNetworkTuple.toObjects(reverse);
            System.arraycopy(firstObjects, 0, objects, pos, firstObjects.length);
            pos += firstObjects.length;
        }
        
        // Add second network objects
        if (secondNetworkTuple != null) {
            Object[] secondObjects = secondNetworkTuple.toObjects(reverse);
            System.arraycopy(secondObjects, 0, objects, pos, secondObjects.length);
            pos += secondObjects.length;
        }
        
        // Add right object if present
        if (this.handle != null) {
            objects[pos] = this.handle.getObject();
        }
        
        return reverse ? reverseArray(objects) : objects;
    }
    
    /**
     * Enhanced toFactHandles that combines handles from both networks
     */
    @Override
    public FactHandle[] toFactHandles() {
        int totalSize = size();
        FactHandle[] handles = new FactHandle[totalSize];
        
        int pos = 0;
        
        // Add first network handles
        if (firstNetworkTuple != null) {
            FactHandle[] firstHandles = firstNetworkTuple.toFactHandles();
            System.arraycopy(firstHandles, 0, handles, pos, firstHandles.length);
            pos += firstHandles.length;
        }
        
        // Add second network handles
        if (secondNetworkTuple != null) {
            FactHandle[] secondHandles = secondNetworkTuple.toFactHandles();
            System.arraycopy(secondHandles, 0, handles, pos, secondHandles.length);
            pos += secondHandles.length;
        }
        
        // Add right handle if present
        if (this.handle != null) {
            handles[pos] = this.handle;
        }
        
        return handles;
    }
    
    /**
     * Network-aware tuple traversal 
     */
    @Override
    public TupleImpl getTuple(int index) {
        // For BiLinearTuple, we need to map to the appropriate source tuple
        int firstSize = firstNetworkTuple != null ? firstNetworkTuple.size() : 0;
        
        if (index < firstSize && firstNetworkTuple != null) {
            return firstNetworkTuple.getTuple(index);
        } else if (index < firstSize + (secondNetworkTuple != null ? secondNetworkTuple.size() : 0) 
                   && secondNetworkTuple != null) {
            return secondNetworkTuple.getTuple(index - firstSize);
        }
        
        // Return this tuple for right fact index
        return this;
    }
    
    // Getters for network access
    public TupleImpl getFirstNetworkTuple() {
        return firstNetworkTuple;
    }
    
    public TupleImpl getSecondNetworkTuple() {
        return secondNetworkTuple;
    }
    
    public int getSecondNetworkOffset() {
        return secondNetworkOffset;
    }
    
    /**
     * Determines which network a given tuple index belongs to
     * 
     * @param index The tuple index
     * @return 1 for first network, 2 for second network, 3 for right fact
     */
    public int getNetworkForIndex(int index) {
        int firstSize = firstNetworkTuple != null ? firstNetworkTuple.size() : 0;
        int secondSize = secondNetworkTuple != null ? secondNetworkTuple.size() : 0;
        
        if (index < firstSize) {
            return 1; // First network
        } else if (index < firstSize + secondSize) {
            return 2; // Second network
        } else {
            return 3; // Right fact
        }
    }
    
    /**
     * Helper method to reverse an array
     */
    private Object[] reverseArray(Object[] array) {
        Object[] reversed = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            reversed[i] = array[array.length - 1 - i];
        }
        return reversed;
    }
    
    @Override
    public String toString() {
        return "BiLinearTuple{" +
                "firstNetwork=" + (firstNetworkTuple != null ? firstNetworkTuple.size() : 0) + " facts, " +
                "secondNetwork=" + (secondNetworkTuple != null ? secondNetworkTuple.size() : 0) + " facts, " +
                "rightFact=" + (handle != null ? "present" : "absent") +
                '}';
    }
    
    // Required abstract method implementations 
    @Override
    public ObjectTypeNodeId getInputOtnId() {
        // Fallback to first network's OTN ID
        if (firstNetworkTuple != null) {
            return firstNetworkTuple.getInputOtnId();
        }
        return null;
    }
    
    @Override
    public boolean isLeftTuple() {
        return true; // BiLinearTuple is always a left tuple
    }
    
    @Override
    public void reAdd() {
        // For BiLinearTuple, delegate to first network tuple's reAdd if it exists
        if (firstNetworkTuple != null) {
            firstNetworkTuple.reAdd();
        }
    }
}
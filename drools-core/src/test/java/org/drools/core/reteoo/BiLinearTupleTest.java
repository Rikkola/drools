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

import java.util.Collections;

import org.drools.core.common.DefaultFactHandle;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.impl.InternalRuleBase;
import org.drools.core.impl.RuleBaseFactory;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.test.model.Cheese;
import org.drools.core.test.model.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.kie.api.runtime.rule.FactHandle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BiLinearTuple cross-network functionality
 */
public class BiLinearTupleTest {
    
    private BuildContext buildContext;
    
    @BeforeEach
    public void setUp() {
        InternalRuleBase kBase = RuleBaseFactory.newRuleBase();
        buildContext = new BuildContext(kBase, Collections.emptyList());
    }
    
    @Test
    public void testBiLinearTupleConstruction() {
        // Create mock tuples representing results from two networks
        TupleImpl firstNetworkTuple = createMockTuple(new Person("John", 30), new Cheese("Cheddar", 10));
        TupleImpl secondNetworkTuple = createMockTuple(new Person("Jane", 25), new Cheese("Swiss", 15));
        
        MockLeftTupleSink sink = new MockLeftTupleSink(1, buildContext);
        
        // Create BiLinearTuple
        BiLinearTuple biLinearTuple = new BiLinearTuple(
            firstNetworkTuple,
            secondNetworkTuple,
            null, // No right fact handle
            sink
        );
        
        // Test basic properties
        assertThat(biLinearTuple.getFirstNetworkTuple()).isEqualTo(firstNetworkTuple);
        assertThat(biLinearTuple.getSecondNetworkTuple()).isEqualTo(secondNetworkTuple);
        assertThat(biLinearTuple.isLeftTuple()).isTrue();
        assertThat(biLinearTuple.size()).isEqualTo(4); // 2 from each network
        assertThat(biLinearTuple.getIndex()).isEqualTo(3); // size - 1
    }
    
    @Test
    public void testBiLinearTupleWithRightFact() {
        TupleImpl firstNetworkTuple = createMockTuple(new Person("John", 30));
        TupleImpl secondNetworkTuple = createMockTuple(new Cheese("Cheddar", 10));
        InternalFactHandle rightFactHandle = new DefaultFactHandle(123, new Person("Right", 40));
        
        MockLeftTupleSink sink = new MockLeftTupleSink(1, buildContext);
        
        BiLinearTuple biLinearTuple = new BiLinearTuple(
            firstNetworkTuple,
            secondNetworkTuple,
            rightFactHandle,
            sink
        );
        
        // Test size includes right fact
        assertThat(biLinearTuple.size()).isEqualTo(3); // 1 + 1 + 1 (right)
        assertThat(biLinearTuple.getIndex()).isEqualTo(2);
    }
    
    @Test
    public void testBiLinearTupleNetworkIndexMapping() {
        TupleImpl firstNetworkTuple = createMockTuple(new Person("John", 30), new Cheese("Cheddar", 10));
        TupleImpl secondNetworkTuple = createMockTuple(new Person("Jane", 25), new Cheese("Swiss", 15));
        
        MockLeftTupleSink sink = new MockLeftTupleSink(1, buildContext);
        
        BiLinearTuple biLinearTuple = new BiLinearTuple(
            firstNetworkTuple,
            secondNetworkTuple,
            null,
            sink
        );
        
        // Test network identification for indices
        assertThat(biLinearTuple.getNetworkForIndex(0)).isEqualTo(1); // First network
        assertThat(biLinearTuple.getNetworkForIndex(1)).isEqualTo(1); // First network
        assertThat(biLinearTuple.getNetworkForIndex(2)).isEqualTo(2); // Second network
        assertThat(biLinearTuple.getNetworkForIndex(3)).isEqualTo(2); // Second network
    }
    
    @Test
    public void testBiLinearTupleObjectAccess() {
        Person person1 = new Person("John", 30);
        Cheese cheese1 = new Cheese("Cheddar", 10);
        Person person2 = new Person("Jane", 25);
        Cheese cheese2 = new Cheese("Swiss", 15);
        
        TupleImpl firstNetworkTuple = createMockTuple(person1, cheese1);
        TupleImpl secondNetworkTuple = createMockTuple(person2, cheese2);
        
        MockLeftTupleSink sink = new MockLeftTupleSink(1, buildContext);
        
        BiLinearTuple biLinearTuple = new BiLinearTuple(
            firstNetworkTuple,
            secondNetworkTuple,
            null,
            sink
        );
        
        // Test object access across networks
        assertThat(biLinearTuple.getObject(0)).isEqualTo(person1);
        assertThat(biLinearTuple.getObject(1)).isEqualTo(cheese1);
        assertThat(biLinearTuple.getObject(2)).isEqualTo(person2);
        assertThat(biLinearTuple.getObject(3)).isEqualTo(cheese2);
    }
    
    @Test
    public void testBiLinearTupleFactHandleAccess() {
        Person person1 = new Person("John", 30);
        Cheese cheese1 = new Cheese("Cheddar", 10);
        
        TupleImpl firstNetworkTuple = createMockTuple(person1, cheese1);
        TupleImpl secondNetworkTuple = createMockTuple(new Person("Jane", 25));
        
        MockLeftTupleSink sink = new MockLeftTupleSink(1, buildContext);
        
        BiLinearTuple biLinearTuple = new BiLinearTuple(
            firstNetworkTuple,
            secondNetworkTuple,
            null,
            sink
        );
        
        // Test fact handle access
        FactHandle handle0 = biLinearTuple.get(0);
        FactHandle handle1 = biLinearTuple.get(1);
        
        assertThat(handle0).isNotNull();
        assertThat(handle1).isNotNull();
        assertThat(handle0.getObject()).isEqualTo(person1);
        assertThat(handle1.getObject()).isEqualTo(cheese1);
    }
    
    @Test
    public void testBiLinearTupleToObjectsArray() {
        Person person1 = new Person("John", 30);
        Cheese cheese1 = new Cheese("Cheddar", 10);
        Person person2 = new Person("Jane", 25);
        
        TupleImpl firstNetworkTuple = createMockTuple(person1, cheese1);
        TupleImpl secondNetworkTuple = createMockTuple(person2);
        
        MockLeftTupleSink sink = new MockLeftTupleSink(1, buildContext);
        
        BiLinearTuple biLinearTuple = new BiLinearTuple(
            firstNetworkTuple,
            secondNetworkTuple,
            null,
            sink
        );
        
        // Test toObjects array construction
        Object[] objects = biLinearTuple.toObjects();
        
        assertThat(objects).hasSize(3);
        assertThat(objects[0]).isEqualTo(person1);
        assertThat(objects[1]).isEqualTo(cheese1);
        assertThat(objects[2]).isEqualTo(person2);
    }
    
    @Test
    public void testBiLinearTupleToFactHandlesArray() {
        TupleImpl firstNetworkTuple = createMockTuple(new Person("John", 30));
        TupleImpl secondNetworkTuple = createMockTuple(new Cheese("Cheddar", 10));
        
        MockLeftTupleSink sink = new MockLeftTupleSink(1, buildContext);
        
        BiLinearTuple biLinearTuple = new BiLinearTuple(
            firstNetworkTuple,
            secondNetworkTuple,
            null,
            sink
        );
        
        // Test toFactHandles array construction
        FactHandle[] handles = biLinearTuple.toFactHandles();
        
        assertThat(handles).hasSize(2);
        assertThat(handles[0]).isNotNull();
        assertThat(handles[1]).isNotNull();
        assertThat(handles[0].getObject()).isInstanceOf(Person.class);
        assertThat(handles[1].getObject()).isInstanceOf(Cheese.class);
    }
    
    @Test
    public void testBiLinearTupleWithNullNetworks() {
        MockLeftTupleSink sink = new MockLeftTupleSink(1, buildContext);
        
        // Test with null first network
        BiLinearTuple biLinearTuple1 = new BiLinearTuple(
            null,
            createMockTuple(new Person("Jane", 25)),
            null,
            sink
        );
        
        assertThat(biLinearTuple1.size()).isEqualTo(1);
        assertThat(biLinearTuple1.getNetworkForIndex(0)).isEqualTo(2);
        
        // Test with null second network
        BiLinearTuple biLinearTuple2 = new BiLinearTuple(
            createMockTuple(new Person("John", 30)),
            null,
            null,
            sink
        );
        
        assertThat(biLinearTuple2.size()).isEqualTo(1);
        assertThat(biLinearTuple2.getNetworkForIndex(0)).isEqualTo(1);
    }
    
    @Test
    public void testBiLinearTupleTupleTraversal() {
        TupleImpl firstNetworkTuple = createMockTuple(new Person("John", 30), new Cheese("Cheddar", 10));
        TupleImpl secondNetworkTuple = createMockTuple(new Person("Jane", 25));
        
        MockLeftTupleSink sink = new MockLeftTupleSink(1, buildContext);
        
        BiLinearTuple biLinearTuple = new BiLinearTuple(
            firstNetworkTuple,
            secondNetworkTuple,
            null,
            sink
        );
        
        // Test tuple traversal for specific indices
        TupleImpl tupleAt0 = biLinearTuple.getTuple(0);
        TupleImpl tupleAt2 = biLinearTuple.getTuple(2);
        
        // Index 0 should come from first network
        assertThat(tupleAt0).isNotNull();
        // Index 2 should come from second network  
        assertThat(tupleAt2).isNotNull();
    }
    
    @Test
    public void testBiLinearTupleReAdd() {
        TupleImpl firstNetworkTuple = createMockTuple(new Person("John", 30));
        TupleImpl secondNetworkTuple = createMockTuple(new Cheese("Cheddar", 10));
        
        MockLeftTupleSink sink = new MockLeftTupleSink(1, buildContext);
        
        BiLinearTuple biLinearTuple = new BiLinearTuple(
            firstNetworkTuple,
            secondNetworkTuple,
            null,
            sink
        );
        
        // Test reAdd method (should delegate to first network tuple)
        // This should not throw an exception
        biLinearTuple.reAdd();
    }
    
    /**
     * Creates a mock tuple containing the specified objects
     */
    private TupleImpl createMockTuple(Object... objects) {
        return new MockTuple(objects);
    }
    
    /**
     * Simple mock tuple implementation for testing
     */
    private static class MockTuple extends TupleImpl {
        private final Object[] objects;
        private final FactHandle[] handles;
        
        public MockTuple(Object... objects) {
            super();
            this.objects = objects;
            this.handles = new FactHandle[objects.length];
            
            // Create fact handles for each object
            for (int i = 0; i < objects.length; i++) {
                this.handles[i] = new DefaultFactHandle(i + 1, objects[i]);
            }
        }
        
        @Override
        public int size() {
            return objects.length;
        }
        
        @Override
        public Object getObject(int index) {
            return index < objects.length ? objects[index] : null;
        }
        
        @Override
        public FactHandle get(int index) {
            return index < handles.length ? handles[index] : null;
        }
        
        @Override
        public Object[] toObjects() {
            return objects.clone();
        }
        
        @Override
        public Object[] toObjects(boolean reverse) {
            Object[] result = objects.clone();
            if (reverse) {
                for (int i = 0; i < result.length / 2; i++) {
                    Object temp = result[i];
                    result[i] = result[result.length - 1 - i];
                    result[result.length - 1 - i] = temp;
                }
            }
            return result;
        }
        
        @Override
        public FactHandle[] toFactHandles() {
            return handles.clone();
        }
        
        @Override
        public boolean isLeftTuple() {
            return true;
        }
        
        @Override
        public void reAdd() {
            // Mock implementation
        }
        
        @Override
        public ObjectTypeNodeId getInputOtnId() {
            return null;
        }
        
        @Override
        public int getIndex() {
            return size() - 1;
        }
        
        @Override
        public TupleImpl getTuple(int index) {
            // Simple implementation - just return this for valid indices
            if (index >= 0 && index < size()) {
                return this;
            }
            return null;
        }
    }
}
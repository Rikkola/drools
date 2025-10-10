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

import org.drools.base.base.ValueResolver;
import org.drools.base.rule.Declaration;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.impl.InternalRuleBase;
import org.drools.core.impl.RuleBaseFactory;
import org.drools.core.reteoo.builder.BuildContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for BiLinearContextEntry functionality
 */
public class BiLinearContextEntryTest {

    private InternalRuleBase ruleBase;
    private BuildContext buildContext;
    
    @Mock
    private ValueResolver valueResolver;
    
    @Mock
    private InternalFactHandle factHandle;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ruleBase = RuleBaseFactory.newRuleBase();
        buildContext = new BuildContext(ruleBase, Collections.emptyList());
    }

    @Test
    public void testBasicContextCreation() {
        // Create declaration context with empty declarations for testing
        Map<String, Declaration> firstDeclarations = new HashMap<>();
        Map<String, Declaration> secondDeclarations = new HashMap<>();
        BiLinearDeclarationContext declarationContext = new BiLinearDeclarationContext(
            firstDeclarations, secondDeclarations, 2
        );
        
        BiLinearContextEntry context = new BiLinearContextEntry(declarationContext);
        
        assertThat(context.getDeclarationContext()).isEqualTo(declarationContext);
        assertThat(context.getNext()).isNull();
        assertThat(context.getFirstNetworkTuple()).isNull();
        assertThat(context.getSecondNetworkTuple()).isNull();
    }

    @Test
    public void testTupleUpdates() {
        BiLinearContextEntry context = new BiLinearContextEntry();
        
        // Create mock tuples
        TupleImpl firstTuple = new MockTupleImpl(1, factHandle);
        TupleImpl secondTuple = new MockTupleImpl(2, factHandle);
        
        // Test BiLinear tuple update
        context.updateFromBiLinearTuples(valueResolver, firstTuple, secondTuple);
        
        assertThat(context.getFirstNetworkTuple()).isEqualTo(firstTuple);
        assertThat(context.getSecondNetworkTuple()).isEqualTo(secondTuple);
        assertThat(context.getValueResolver()).isEqualTo(valueResolver);
    }

    @Test
    public void testFactHandleUpdate() {
        BiLinearContextEntry context = new BiLinearContextEntry();
        
        context.updateFromFactHandle(valueResolver, factHandle);
        
        assertThat(context.getRightHandle()).isEqualTo(factHandle);
        assertThat(context.getValueResolver()).isEqualTo(valueResolver);
    }

    @Test
    public void testContextReset() {
        BiLinearContextEntry context = new BiLinearContextEntry();
        
        // Set up some state
        TupleImpl tuple = new MockTupleImpl(1, factHandle);
        context.updateFromBiLinearTuples(valueResolver, tuple, tuple);
        context.updateFromFactHandle(valueResolver, factHandle);
        
        // Reset tuple context
        context.resetTuple();
        
        assertThat(context.getFirstNetworkTuple()).isNull();
        assertThat(context.getSecondNetworkTuple()).isNull();
        assertThat(context.getCombinedTuple()).isNull();
        
        // Reset fact handle context
        context.resetFactHandle();
        
        assertThat(context.getValueResolver()).isNull();
        assertThat(context.getRightHandle()).isNull();
    }

    @Test
    public void testBiLinearTupleHandling() {
        BiLinearContextEntry context = new BiLinearContextEntry();
        
        // Create BiLinearTuple
        TupleImpl firstTuple = new MockTupleImpl(1, factHandle);
        TupleImpl secondTuple = new MockTupleImpl(2, factHandle);
        BiLinearTuple biLinearTuple = new BiLinearTuple(firstTuple, secondTuple, null, null);
        
        // Update from BiLinearTuple
        context.updateFromTuple(valueResolver, biLinearTuple);
        
        assertThat(context.getFirstNetworkTuple()).isEqualTo(firstTuple);
        assertThat(context.getSecondNetworkTuple()).isEqualTo(secondTuple);
        assertThat(context.getCombinedTuple()).isEqualTo(biLinearTuple);
    }

    @Test
    public void testRegularTupleFallback() {
        BiLinearContextEntry context = new BiLinearContextEntry();
        
        // Update with regular tuple (not BiLinearTuple)
        TupleImpl regularTuple = new MockTupleImpl(1, factHandle);
        context.updateFromTuple(valueResolver, regularTuple);
        
        // Should treat as first network tuple
        assertThat(context.getFirstNetworkTuple()).isEqualTo(regularTuple);
        assertThat(context.getSecondNetworkTuple()).isNull();
        assertThat(context.getCombinedTuple()).isNull();
    }

    /**
     * Simple mock implementation of TupleImpl for testing
     */
    private static class MockTupleImpl extends TupleImpl {
        private final int id;
        
        public MockTupleImpl(int id, InternalFactHandle handle) {
            super(handle, null, false);
            this.id = id;
        }
        
        @Override
        public int size() {
            return 1;
        }
        
        @Override
        public ObjectTypeNodeId getInputOtnId() {
            return null;
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
        public String toString() {
            return "MockTuple-" + id;
        }
    }
}
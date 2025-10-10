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

import org.drools.base.reteoo.NodeTypeEnums;
import org.drools.core.common.BiLinearBetaConstraints;
import org.drools.core.common.EmptyBetaConstraints;
import org.drools.core.impl.InternalRuleBase;
import org.drools.core.impl.RuleBaseFactory;
import org.drools.core.reteoo.builder.BuildContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BiLinearJoinNode functionality
 */
public class BiLinearJoinNodeTest {
    
    @Test
    public void testBiLinearJoinNodeConstruction() {
        InternalRuleBase kBase = RuleBaseFactory.newRuleBase();
        BuildContext buildContext = new BuildContext(kBase, Collections.emptyList());
        
        // Create mock sources for the two left inputs
        final MockTupleSource firstLeftSource = new MockTupleSource(1, buildContext);
        final MockTupleSource secondLeftSource = new MockTupleSource(2, buildContext);
        final MockObjectSource objectSource = new MockObjectSource(3);
        RightInputAdapterNode rightInput = new JoinRightAdapterNode(4, objectSource, buildContext);
        
        // Create BiLinearJoinNode
        BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
            5,
            firstLeftSource,
            secondLeftSource,
            rightInput,
            EmptyBetaConstraints.getInstance(),
            buildContext
        );
        
        // Test basic properties
        assertThat(biLinearJoin.getId()).isEqualTo(5);
        assertThat(biLinearJoin.getType()).isEqualTo(NodeTypeEnums.BiLinearJoinNode);
        assertThat(biLinearJoin.getLeftTupleSource()).isEqualTo(firstLeftSource);
        assertThat(biLinearJoin.getSecondLeftInput()).isEqualTo(secondLeftSource);
        assertThat(biLinearJoin.getRightInput()).isEqualTo(rightInput);
    }
    
    @Test
    public void testBiLinearJoinNodeObjectCount() {
        InternalRuleBase kBase = RuleBaseFactory.newRuleBase();
        BuildContext buildContext = new BuildContext(kBase, Collections.emptyList());
        
        // Create mock sources with known object counts
        final MockTupleSource firstLeftSource = new MockTupleSource(1, buildContext);
        firstLeftSource.setObjectCount(2); // Simulates 2 objects in first network
        
        final MockTupleSource secondLeftSource = new MockTupleSource(2, buildContext);
        secondLeftSource.setObjectCount(3); // Simulates 3 objects in second network
        
        final MockObjectSource objectSource = new MockObjectSource(3);
        RightInputAdapterNode rightInput = new JoinRightAdapterNode(4, objectSource, buildContext);
        
        // Create BiLinearJoinNode
        BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
            5,
            firstLeftSource,
            secondLeftSource,
            rightInput,
            EmptyBetaConstraints.getInstance(),
            buildContext
        );
        
        // Test that object count is sum of both left inputs
        assertThat(biLinearJoin.getObjectCount()).isEqualTo(5); // 2 + 3 = 5
    }
    
    @Test
    public void testBiLinearJoinNodeWithDeclarationContext() {
        InternalRuleBase kBase = RuleBaseFactory.newRuleBase();
        BuildContext buildContext = new BuildContext(kBase, Collections.emptyList());
        
        final MockTupleSource firstLeftSource = new MockTupleSource(1, buildContext);
        final MockTupleSource secondLeftSource = new MockTupleSource(2, buildContext);
        final MockObjectSource objectSource = new MockObjectSource(3);
        RightInputAdapterNode rightInput = new JoinRightAdapterNode(4, objectSource, buildContext);
        
        // Create declaration context for cross-network variables
        BiLinearDeclarationContext declarationContext = new BiLinearDeclarationContext(
            firstLeftSource,
            secondLeftSource,
            2 // Second network offset
        );
        
        // Create BiLinearJoinNode with explicit declaration context
        BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
            5,
            firstLeftSource,
            secondLeftSource,
            rightInput,
            EmptyBetaConstraints.getInstance(),
            buildContext,
            declarationContext
        );
        
        // Test cross-network variable resolution capabilities
        assertThat(biLinearJoin.hasCrossNetworkVariableResolution()).isTrue();
        assertThat(biLinearJoin.getDeclarationContext()).isEqualTo(declarationContext);
        assertThat(biLinearJoin.getBiLinearConstraints()).isNotNull();
    }
    
    @Test
    public void testBiLinearJoinNodeConstraintWrapping() {
        InternalRuleBase kBase = RuleBaseFactory.newRuleBase();
        BuildContext buildContext = new BuildContext(kBase, Collections.emptyList());
        
        final MockTupleSource firstLeftSource = new MockTupleSource(1, buildContext);
        final MockTupleSource secondLeftSource = new MockTupleSource(2, buildContext);
        final MockObjectSource objectSource = new MockObjectSource(3);
        RightInputAdapterNode rightInput = new JoinRightAdapterNode(4, objectSource, buildContext);
        
        BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
            5,
            firstLeftSource,
            secondLeftSource,
            rightInput,
            EmptyBetaConstraints.getInstance(),
            buildContext
        );
        
        // Test that constraints are wrapped in BiLinearBetaConstraints
        assertThat(biLinearJoin.getRawConstraints()).isInstanceOf(BiLinearBetaConstraints.class);
        
        BiLinearBetaConstraints biLinearConstraints = biLinearJoin.getBiLinearConstraints();
        assertThat(biLinearConstraints).isNotNull();
        assertThat(biLinearConstraints.getWrappedConstraints()).isNotNull();
    }
    
    @Test
    public void testBiLinearJoinNodeSecondInputUpdate() {
        InternalRuleBase kBase = RuleBaseFactory.newRuleBase();
        BuildContext buildContext = new BuildContext(kBase, Collections.emptyList());
        
        final MockTupleSource firstLeftSource = new MockTupleSource(1, buildContext);
        final MockTupleSource secondLeftSource = new MockTupleSource(2, buildContext);
        final MockTupleSource newSecondLeftSource = new MockTupleSource(6, buildContext);
        final MockObjectSource objectSource = new MockObjectSource(3);
        RightInputAdapterNode rightInput = new JoinRightAdapterNode(4, objectSource, buildContext);
        
        BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
            5,
            firstLeftSource,
            secondLeftSource,
            rightInput,
            EmptyBetaConstraints.getInstance(),
            buildContext
        );
        
        // Test updating second left input
        biLinearJoin.setSecondLeftInput(newSecondLeftSource);
        
        assertThat(biLinearJoin.getSecondLeftInput()).isEqualTo(newSecondLeftSource);
        // Declaration context should be recreated when second input changes
        assertThat(biLinearJoin.getDeclarationContext()).isNotNull();
    }
    
    @Test
    public void testBiLinearTupleCreation() {
        InternalRuleBase kBase = RuleBaseFactory.newRuleBase();
        BuildContext buildContext = new BuildContext(kBase, Collections.emptyList());
        
        final MockTupleSource firstLeftSource = new MockTupleSource(1, buildContext);
        final MockTupleSource secondLeftSource = new MockTupleSource(2, buildContext);
        final MockObjectSource objectSource = new MockObjectSource(3);
        RightInputAdapterNode rightInput = new JoinRightAdapterNode(4, objectSource, buildContext);
        
        BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
            5,
            firstLeftSource,
            secondLeftSource,
            rightInput,
            EmptyBetaConstraints.getInstance(),
            buildContext
        );
        
        // Create mock tuples for testing
        TupleImpl firstNetworkTuple = new MockLeftTuple();
        TupleImpl secondNetworkTuple = new MockLeftTuple();
        LeftTupleSink sink = biLinearJoin; // Use the biLinearJoin itself as sink
        
        // Test tuple creation - createBiLinearTuple now returns LeftTuple for compatibility
        TupleImpl resultTuple = biLinearJoin.createBiLinearTuple(
            firstNetworkTuple,
            secondNetworkTuple,
            sink
        );
        
        // Verify that we get a standard LeftTuple for downstream compatibility
        assertThat(resultTuple).isNotNull();
        assertThat(resultTuple).isInstanceOf(TupleImpl.class);
        assertThat(resultTuple.isLeftTuple()).isTrue();
        // Note: BiLinearJoinNode.createBiLinearTuple now creates LeftTuple via TupleFactory
        // to prevent ClassCastException at terminal nodes
    }
    
    @Test
    public void testBiLinearJoinNodeToString() {
        InternalRuleBase kBase = RuleBaseFactory.newRuleBase();
        BuildContext buildContext = new BuildContext(kBase, Collections.emptyList());
        
        final MockTupleSource firstLeftSource = new MockTupleSource(1, buildContext);
        final MockTupleSource secondLeftSource = new MockTupleSource(2, buildContext);
        final MockObjectSource objectSource = new MockObjectSource(3);
        RightInputAdapterNode rightInput = new JoinRightAdapterNode(4, objectSource, buildContext);
        
        BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
            5,
            firstLeftSource,
            secondLeftSource,
            rightInput,
            EmptyBetaConstraints.getInstance(),
            buildContext
        );
        
        String toString = biLinearJoin.toString();
        
        // Test that toString contains expected information
        assertThat(toString).contains("BiLinearJoinNode");
        assertThat(toString).contains("5"); // Node ID
        assertThat(toString).contains("FirstInput: 1"); // First left input ID
        assertThat(toString).contains("SecondInput: 2"); // Second left input ID
        assertThat(toString).contains("HasCrossNetworkVars: true");
    }
    
    /**
     * Simple mock LeftTuple for testing
     */
    private static class MockLeftTuple extends TupleImpl {
        
        public MockLeftTuple() {
            super();
        }
        
        @Override
        public boolean isLeftTuple() {
            return true;
        }
        
        @Override
        public void reAdd() {
            // Mock implementation - no actual reAdd needed for test
        }
        
        @Override
        public ObjectTypeNodeId getInputOtnId() {
            return null;
        }
    }
    
}
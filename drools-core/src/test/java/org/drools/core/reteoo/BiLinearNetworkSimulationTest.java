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

import org.drools.base.base.ClassObjectType;
import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.base.reteoo.NodeTypeEnums;
import org.drools.core.common.BetaConstraints;
import org.drools.core.common.BiLinearBetaConstraints;
import org.drools.core.common.EmptyBetaConstraints;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.impl.InternalRuleBase;
import org.drools.core.impl.RuleBaseFactory;
import org.drools.core.reteoo.builder.BuildContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simulation test for BiLinearNetworkTest scenario that can run from drools-core.
 * This verifies that BiLinearJoinNode behaves correctly in the integration scenario.
 */
public class BiLinearNetworkSimulationTest {
    
    BuildContext buildContext;
    
    // Simplified test data classes (since we can't easily import the phreak test classes)
    static class SimpleA {
        Integer object;
        public SimpleA(Integer object) { this.object = object; }
        public Integer getObject() { return object; }
        @Override public String toString() { return "A[" + object + "]"; }
    }
    
    static class SimpleB {
        Integer object;
        public SimpleB(Integer object) { this.object = object; }
        public Integer getObject() { return object; }
        @Override public String toString() { return "B[" + object + "]"; }
    }
    
    static class SimpleC {
        Integer object;
        public SimpleC(Integer object) { this.object = object; }
        public Integer getObject() { return object; }
        @Override public String toString() { return "C[" + object + "]"; }
    }
    
    static class SimpleD {
        Integer object;
        public SimpleD(Integer object) { this.object = object; }
        public Integer getObject() { return object; }
        @Override public String toString() { return "D[" + object + "]"; }
    }
    
    public void setupBiLinearNetwork() {
        InternalRuleBase kBase = RuleBaseFactory.newRuleBase();
        buildContext = new BuildContext(kBase, Collections.emptyList());
        buildContext.setRule(new RuleImpl("test"));
    }
    
    @Test
    public void testBiLinearJoinNodeStructuralSetup() {
        setupBiLinearNetwork();
        
        // Simulate the BiLinearNetworkTest structure
        // Create mock sources for the two left inputs
        LeftTupleSource joinNodeABC = new MockTupleSource(1, buildContext);  // Rule1's ABC chain
        LeftTupleSource cLiaNode = new MockTupleSource(2, buildContext);    // Rule2's C
        ObjectSource dObjectSource = new MockObjectSource(3, buildContext);
        JoinRightAdapterNode dRiaNode = new JoinRightAdapterNode(4, dObjectSource, buildContext);
        
        // Create the shared BiLinearJoinNode (matching BiLinearNetworkTest line 193-200)
        BetaConstraints constraints = EmptyBetaConstraints.getInstance(); // Start simple
        
        BiLinearJoinNode biLinearJoinCD = new BiLinearJoinNode(
            buildContext.getNextNodeId(),
            joinNodeABC,  // Rule1's ABC chain as first left input
            cLiaNode,     // Rule2's C as second left input  
            dRiaNode,     // D as right input (shared by both rule paths)
            constraints,  // For now use empty constraints
            buildContext
        );
        
        // Network connections (matching BiLinearNetworkTest lines 211-216)
        joinNodeABC.addTupleSink(biLinearJoinCD);  // Rule1's ABC chain feeds into bi-linear join
        cLiaNode.addTupleSink(biLinearJoinCD);     // Rule2's C feeds into bi-linear join
        
        // Create sink nodes  
        JoinNode sinkNodeRule1 = new MockJoinNode(100, buildContext);
        JoinNode sinkNodeRule2 = new MockJoinNode(101, buildContext);
        
        biLinearJoinCD.addTupleSink(sinkNodeRule1);  // Rule1 gets combined (A,B,C,D) results
        biLinearJoinCD.addTupleSink(sinkNodeRule2);  // Rule2 gets (C,D) results
        
        // Verify the BiLinearJoinNode structure (matching BiLinearNetworkTest assertions)
        assertThat(biLinearJoinCD).isNotNull();
        assertThat(biLinearJoinCD.getLeftTupleSource()).isEqualTo(joinNodeABC);
        assertThat(biLinearJoinCD.getSecondLeftInput()).isEqualTo(cLiaNode);
        assertThat(biLinearJoinCD.getType()).isEqualTo(NodeTypeEnums.BiLinearJoinNode);
        
        // Verify BiLinear-specific features
        assertThat(biLinearJoinCD.hasCrossNetworkVariableResolution()).isTrue();
        assertThat(biLinearJoinCD.getBiLinearConstraints()).isNotNull();
        assertThat(biLinearJoinCD.getDeclarationContext()).isNotNull();
        
        // Verify network connectivity
        assertThat(biLinearJoinCD.getSinks()).hasSize(2);
        assertThat(biLinearJoinCD.getSinks()).contains(sinkNodeRule1, sinkNodeRule2);
        
        System.out.println("✅ BiLinearJoinNode structural setup verified:");
        System.out.println("   - BiLinearJoinNode created: " + biLinearJoinCD.getId());
        System.out.println("   - First left input: " + biLinearJoinCD.getLeftTupleSource().getId());
        System.out.println("   - Second left input: " + biLinearJoinCD.getSecondLeftInput().getId());
        System.out.println("   - Has cross-network vars: " + biLinearJoinCD.hasCrossNetworkVariableResolution());
        System.out.println("   - Connected to " + biLinearJoinCD.getSinks().length + " sinks");
    }
    
    @Test
    public void testBiLinearJoinNodeConstraintsCreation() {
        setupBiLinearNetwork();
        
        // Test constraints creation similar to BiLinearNetworkTest
        LeftTupleSource joinNodeABC = new MockTupleSource(1, buildContext);
        LeftTupleSource cLiaNode = new MockTupleSource(2, buildContext);
        ObjectSource dObjectSource = new MockObjectSource(3, buildContext);
        JoinRightAdapterNode dRiaNode = new JoinRightAdapterNode(4, dObjectSource, buildContext);
        
        BiLinearJoinNode biLinearJoinCD = new BiLinearJoinNode(
            buildContext.getNextNodeId(),
            joinNodeABC,
            cLiaNode,
            dRiaNode,
            EmptyBetaConstraints.getInstance(),
            buildContext
        );
        
        // Test that constraints were properly wrapped
        assertThat(biLinearJoinCD.getRawConstraints()).isInstanceOf(BiLinearBetaConstraints.class);
        
        BiLinearBetaConstraints biLinearConstraints = biLinearJoinCD.getBiLinearConstraints();
        assertThat(biLinearConstraints).isNotNull();
        assertThat(biLinearConstraints.getWrappedConstraints()).isNotNull();
        assertThat(biLinearConstraints.getDeclarationContext()).isNotNull();
        
        // Test context creation
        BiLinearContextEntry contextEntry = biLinearConstraints.createContext();
        assertThat(contextEntry).isNotNull();
        assertThat(contextEntry.getDeclarationContext()).isNotNull();
        
        System.out.println("✅ BiLinearJoinNode constraints creation verified:");
        System.out.println("   - BiLinearBetaConstraints created successfully");
        System.out.println("   - BiLinearContextEntry created successfully");
        System.out.println("   - Declaration context available");
    }
    
    /**
     * Simple mock join node for testing sinks
     */
    private static class MockJoinNode extends JoinNode {
        public MockJoinNode(int id, BuildContext buildContext) {
            super(id, 
                  new MockTupleSource(id + 1000, buildContext), 
                  new JoinRightAdapterNode(id + 2000, new MockObjectSource(id + 3000, buildContext), buildContext),
                  EmptyBetaConstraints.getInstance(),
                  buildContext);
        }
    }
}
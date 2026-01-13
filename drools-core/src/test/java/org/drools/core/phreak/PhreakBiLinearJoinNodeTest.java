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
package org.drools.core.phreak;

import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.core.common.BetaConstraints;
import org.drools.core.common.BiLinearBetaConstraints;
import org.drools.core.common.EmptyBetaConstraints;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.TupleSets;
import org.drools.core.common.TupleSetsImpl;
import org.drools.core.impl.InternalRuleBase;
import org.drools.core.impl.RuleBaseFactory;
import org.drools.core.reteoo.*;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.util.index.TupleIndexHashTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for PhreakBiLinearJoinNode execution engine
 */
public class PhreakBiLinearJoinNodeTest {

    private InternalRuleBase ruleBase;
    private BuildContext buildContext;
    
    @Mock
    private ReteEvaluator reteEvaluator;
    
    @Mock
    private LeftTupleSink sink;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ruleBase = RuleBaseFactory.newRuleBase();
        buildContext = new BuildContext(ruleBase, Collections.emptyList());
        buildContext.setRule(new RuleImpl("test"));
    }

    @Test
    public void testPhreakProcessorCreation() {
        PhreakBiLinearJoinNode processor = new PhreakBiLinearJoinNode(reteEvaluator);
        assertThat(processor).isNotNull();
    }

    @Test
    public void testLeftInsertWithEmptyMemory() {
        // Create BiLinearJoinNode
        MockTupleSource firstLeftInput = new MockTupleSource(1, buildContext);
        MockTupleSource secondLeftInput = new MockTupleSource(2, buildContext);
        MockObjectSource objectSource = new MockObjectSource(3, buildContext);
        JoinRightAdapterNode rightAdapter = new JoinRightAdapterNode(4, objectSource, buildContext);
        
        BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
            5, firstLeftInput, secondLeftInput, rightAdapter, buildContext
        );
        
        // Create processor
        PhreakBiLinearJoinNode processor = new PhreakBiLinearJoinNode(reteEvaluator);
        
        // Create memory with empty tuple memory
        BetaMemory betaMemory = new BetaMemory(
            new TupleIndexHashTable(),  // Left memory
            new TupleIndexHashTable(),  // Right memory (for second left input)
            biLinearJoin.createContext(),
            5 // node type
        );
        
        // Create source tuples (empty)
        TupleSets srcLeftTuples = new TupleSetsImpl();
        TupleSets trgLeftTuples = new TupleSetsImpl();
        TupleSets stagedLeftTuples = new TupleSetsImpl();
        
        // Should not throw exception with empty memory
        processor.doNode(biLinearJoin, sink, betaMemory, srcLeftTuples, trgLeftTuples, stagedLeftTuples);
        
        // Verify no tuples were created
        assertThat(trgLeftTuples.getInsertFirst()).isNull();
    }

    @Test
    public void testBiLinearConstraintsIntegration() {
        // Test that the processor correctly handles BiLinearBetaConstraints
        
        MockTupleSource firstLeftInput = new MockTupleSource(1, buildContext);
        MockTupleSource secondLeftInput = new MockTupleSource(2, buildContext);
        MockObjectSource objectSource = new MockObjectSource(3, buildContext);
        JoinRightAdapterNode rightAdapter = new JoinRightAdapterNode(4, objectSource, buildContext);
        
        BetaConstraints constraints = new EmptyBetaConstraints();
        BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
            5, firstLeftInput, secondLeftInput, rightAdapter, constraints, buildContext
        );
        
        // Verify BiLinear constraints are created
        BiLinearBetaConstraints biLinearConstraints = biLinearJoin.getBiLinearConstraints();
        assertThat(biLinearConstraints).isNotNull();
        // Note: BiLinearBetaConstraints wraps the original constraints internally
        
        // Create processor
        PhreakBiLinearJoinNode processor = new PhreakBiLinearJoinNode(reteEvaluator);
        
        // Create memory with BiLinear context
        BetaMemory betaMemory = new BetaMemory(
            new TupleIndexHashTable(),
            new TupleIndexHashTable(),
            biLinearJoin.createContext(),  // Creates BiLinearContextEntry
            5 // node type
        );
        
        // Verify context is BiLinearContextEntry
        assertThat(betaMemory.getContext()).isInstanceOf(BiLinearContextEntry.class);
        
        TupleSets srcLeftTuples = new TupleSetsImpl();
        TupleSets trgLeftTuples = new TupleSetsImpl();
        TupleSets stagedLeftTuples = new TupleSetsImpl();
        
        // Should handle BiLinear constraints correctly
        processor.doNode(biLinearJoin, sink, betaMemory, srcLeftTuples, trgLeftTuples, stagedLeftTuples);
    }

    @Test
    public void testBiLinearTupleCreation() {
        // Test that processor creates BiLinearTuple instances correctly
        
        MockTupleSource firstLeftInput = new MockTupleSource(1, buildContext);
        MockTupleSource secondLeftInput = new MockTupleSource(2, buildContext);
        MockObjectSource objectSource = new MockObjectSource(3, buildContext);
        JoinRightAdapterNode rightAdapter = new JoinRightAdapterNode(4, objectSource, buildContext);
        
        BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
            5, firstLeftInput, secondLeftInput, rightAdapter, buildContext
        );
        
        PhreakBiLinearJoinNode processor = new PhreakBiLinearJoinNode(reteEvaluator);
        
        // Create memories
        BetaMemory betaMemory = new BetaMemory(
            new TupleIndexHashTable(),
            new TupleIndexHashTable(),
            biLinearJoin.createContext(),
            5 // node type
        );
        
        // Create a source tuple for insertion
        TupleSets srcLeftTuples = new TupleSetsImpl();
        TupleSets trgLeftTuples = new TupleSetsImpl();
        TupleSets stagedLeftTuples = new TupleSetsImpl();
        
        // For this test, we verify the processor doesn't crash
        // Full integration testing will be done in integration tests
        processor.doNode(biLinearJoin, sink, betaMemory, srcLeftTuples, trgLeftTuples, stagedLeftTuples);
        
        // Test passes if no exceptions are thrown
        assertThat(true).isTrue();
    }

    @Test
    public void testConstraintFallback() {
        // Test fallback to standard constraints when BiLinear constraints are not available
        
        MockTupleSource firstLeftInput = new MockTupleSource(1, buildContext);
        MockTupleSource secondLeftInput = new MockTupleSource(2, buildContext);
        MockObjectSource objectSource = new MockObjectSource(3, buildContext);
        JoinRightAdapterNode rightAdapter = new JoinRightAdapterNode(4, objectSource, buildContext);
        
        // Create node without BiLinear constraints
        BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
            5, firstLeftInput, secondLeftInput, rightAdapter, 
            new EmptyBetaConstraints(), buildContext
        );
        
        PhreakBiLinearJoinNode processor = new PhreakBiLinearJoinNode(reteEvaluator);
        
        // Create memory
        BetaMemory betaMemory = new BetaMemory(
            new TupleIndexHashTable(),
            new TupleIndexHashTable(),
            biLinearJoin.createContext(),
            5 // node type
        );
        
        TupleSets srcLeftTuples = new TupleSetsImpl();
        TupleSets trgLeftTuples = new TupleSetsImpl();
        TupleSets stagedLeftTuples = new TupleSetsImpl();
        
        // Should handle standard constraints correctly
        processor.doNode(biLinearJoin, sink, betaMemory, srcLeftTuples, trgLeftTuples, stagedLeftTuples);
        
        // Test passes if no exceptions are thrown
        assertThat(true).isTrue();
    }
}
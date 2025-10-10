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

import org.drools.core.common.BetaConstraints;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.TupleSets;
import org.drools.core.reteoo.BetaMemory;
import org.drools.core.common.BiLinearBetaConstraints;
import org.drools.core.reteoo.BiLinearContextEntry;
import org.drools.core.reteoo.BiLinearDeclarationContext;
import org.drools.core.reteoo.BiLinearJoinNode;
import org.drools.core.reteoo.BiLinearTuple;
import org.drools.core.reteoo.LeftTuple;
import org.drools.core.reteoo.LeftTupleSink;
import org.drools.core.reteoo.TupleFactory;
import org.drools.core.reteoo.TupleImpl;
import org.drools.core.reteoo.TupleMemory;
import org.drools.core.util.AbstractHashTable;
import org.drools.core.util.FastIterator;

/**
 * Phreak processor for BiLinearJoinNode that handles joining two left input networks.
 * Unlike traditional joins that have one left and one right input, BiLinearJoinNode
 * has two left inputs that get joined together.
 */
public class PhreakBiLinearJoinNode {

    private final ReteEvaluator reteEvaluator;
    
    // Simple execution counter for rule isolation
    private static int executionCounter = 0;

    public PhreakBiLinearJoinNode(ReteEvaluator reteEvaluator) {
        this.reteEvaluator = reteEvaluator;
    }

    public void doNode(BiLinearJoinNode biLinearJoinNode,
                       LeftTupleSink sink,
                       BetaMemory bm,
                       TupleSets srcLeftTuples,
                       TupleSets stagedLeftTuples,
                       TupleSets trgLeftTuples) {

        // CRITICAL: Use execution counter for rule isolation
        String currentRuleContext = "rule_" + (++executionCounter);

        // Get tuples from second left input (stored in "right" memory for BiLinear)
        TupleSets srcRightTuples = bm.getStagedRightTuples().takeAll();

        // Process deletes from both inputs first
        if (srcRightTuples.getDeleteFirst() != null) {
            doRightDeletes(bm, srcRightTuples, trgLeftTuples, stagedLeftTuples);
        }
        if (srcLeftTuples.getDeleteFirst() != null) {
            doLeftDeletes(bm, srcLeftTuples, trgLeftTuples, stagedLeftTuples);
        }

        // Process updates from both inputs
        if (srcRightTuples.getUpdateFirst() != null) {
            PhreakNodeOperations.doUpdatesReorderRightMemory(bm, srcRightTuples);
        }
        if (srcLeftTuples.getUpdateFirst() != null) {
            PhreakNodeOperations.doUpdatesReorderLeftMemory(bm, srcLeftTuples);
        }
        
        if (srcRightTuples.getUpdateFirst() != null) {
            doRightUpdates(biLinearJoinNode, sink, bm, srcRightTuples, trgLeftTuples, stagedLeftTuples);
        }
        if (srcLeftTuples.getUpdateFirst() != null) {
            doLeftUpdates(biLinearJoinNode, sink, bm, srcLeftTuples, trgLeftTuples, stagedLeftTuples);
        }

        // Process inserts from both inputs
        if (srcRightTuples.getInsertFirst() != null) {
            doRightInserts(biLinearJoinNode, sink, bm, srcRightTuples, trgLeftTuples, currentRuleContext);
        }
        if (srcLeftTuples.getInsertFirst() != null) {
            doLeftInserts(biLinearJoinNode, sink, bm, srcLeftTuples, trgLeftTuples, currentRuleContext);
        }

        srcRightTuples.resetAll();
        srcLeftTuples.resetAll();
    }

    /**
     * Process left tuple inserts for BiLinearJoinNode.
     * This method joins tuples from the primary left input with tuples from the second left input.
     */
    public void doLeftInserts(BiLinearJoinNode biLinearJoinNode,
                              LeftTupleSink sink,
                              BetaMemory<?> bm,
                              TupleSets srcLeftTuples,
                              TupleSets trgLeftTuples,
                              String currentRuleContext) {
        
        TupleMemory ltm = bm.getLeftTupleMemory();   // Memory for first left input
        TupleMemory rtm = bm.getRightTupleMemory();  // Memory for second left input (treated as "right" for memory purposes)
        Object contextEntry = bm.getContext();
        BetaConstraints constraints = biLinearJoinNode.getRawConstraints();

        // Get BiLinear constraints wrapper (all BiLinearJoinNode constraints are wrapped)
        BiLinearBetaConstraints biLinearConstraints = biLinearJoinNode.getBiLinearConstraints();
        
        // Check for BiLinear constraints and context
        if (biLinearConstraints == null) {
            // Fall back to standard constraint processing
            for (TupleImpl leftTuple = srcLeftTuples.getInsertFirst(); leftTuple != null; ) {
                TupleImpl next = leftTuple.getStagedNext();
                leftTuple.clearStaged();
                leftTuple = next;
            }
            return;
        }
        
        BiLinearContextEntry biLinearContext = contextEntry instanceof BiLinearContextEntry ?
            (BiLinearContextEntry) contextEntry : null;
        
        if (biLinearContext == null) {
            // Fall back to standard constraint processing  
            for (TupleImpl leftTuple = srcLeftTuples.getInsertFirst(); leftTuple != null; ) {
                TupleImpl next = leftTuple.getStagedNext();
                leftTuple.clearStaged();
                leftTuple = next;
            }
            return;
        }

        for (TupleImpl leftTuple = srcLeftTuples.getInsertFirst(); leftTuple != null; ) {
            TupleImpl next = leftTuple.getStagedNext();

            boolean useLeftMemory = PhreakNodeOperations.useLeftMemory(biLinearJoinNode, leftTuple);

            // SIMPLIFIED: Use default memory for now to ensure basic functionality
            if (useLeftMemory) {
                ltm.add(leftTuple);
            }

            // SIMPLIFIED: Use default memory access for now
            if (rtm != null && rtm.size() > 0) {
                FastIterator<TupleImpl> it = biLinearJoinNode.getSecondNetworkIterator(rtm);
                TupleImpl firstSecondTuple = biLinearJoinNode.getFirstSecondNetworkTuple(leftTuple, rtm, it);

                // Update constraint context with BiLinear context
                biLinearConstraints.updateFromTuple(biLinearContext, reteEvaluator, leftTuple);

                for (TupleImpl secondLeftTuple = firstSecondTuple; 
                     secondLeftTuple != null; 
                     secondLeftTuple = it != null ? it.next(secondLeftTuple) : null) {
                    
                    biLinearConstraints.updateFromBiLinearTuples(biLinearContext, reteEvaluator, 
                                                               leftTuple, secondLeftTuple);
                    boolean isAllowed = biLinearConstraints.isAllowedCachedLeft(biLinearContext, 
                                                                      secondLeftTuple.getFactHandle());
                    
                    if (isAllowed) {
                        // Create a bi-linear join result combining both left inputs
                        insertBiLinearChildTuple(trgLeftTuples,
                                               leftTuple,
                                               secondLeftTuple,
                                               sink,
                                               useLeftMemory);
                    }
                }
            }
            
            leftTuple.clearStaged();
            leftTuple = next;
        }
        
        // Reset constraint context
        biLinearConstraints.resetTupleContext(biLinearContext);
    }

    /**
     * Process left tuple updates for BiLinearJoinNode.
     */
    public void doLeftUpdates(BiLinearJoinNode biLinearJoinNode,
                              LeftTupleSink sink,
                              BetaMemory<?> bm,
                              TupleSets srcLeftTuples,
                              TupleSets trgLeftTuples,
                              TupleSets stagedLeftTuples) {
        
        TupleMemory rtm = bm.getRightTupleMemory();
        Object contextEntry = bm.getContext();
        BetaConstraints constraints = biLinearJoinNode.getRawConstraints();

        // Get BiLinear constraints wrapper (all BiLinearJoinNode constraints are wrapped)
        BiLinearBetaConstraints biLinearConstraints = biLinearJoinNode.getBiLinearConstraints();
        
        // BiLinearJoinNode should ALWAYS have BiLinearBetaConstraints
        if (biLinearConstraints == null) {
            throw new IllegalStateException("BiLinearJoinNode must have BiLinearBetaConstraints but got null");
        }
        
        BiLinearContextEntry biLinearContext = contextEntry instanceof BiLinearContextEntry ?
            (BiLinearContextEntry) contextEntry : null;
        
        // BiLinearJoinNode should ALWAYS have BiLinearContextEntry
        if (biLinearContext == null) {
            throw new IllegalStateException("BiLinearJoinNode must have BiLinearContextEntry but got " + 
                                          (contextEntry != null ? contextEntry.getClass().getSimpleName() : "null"));
        }

        for (TupleImpl leftTuple = srcLeftTuples.getUpdateFirst(); leftTuple != null; ) {
            TupleImpl next = leftTuple.getStagedNext();

            // Update constraint context with BiLinear context
            biLinearConstraints.updateFromTuple(biLinearContext, reteEvaluator, leftTuple);

            FastIterator<TupleImpl> it = biLinearJoinNode.getSecondNetworkIterator(rtm);
            
            TupleImpl childLeftTuple = leftTuple.getFirstChild();
            if (childLeftTuple != null) {
                PhreakNodeOperations.unlinkAndDeleteChildLeftTuple(trgLeftTuples, stagedLeftTuples, childLeftTuple);
            }

            for (TupleImpl secondLeftTuple = biLinearJoinNode.getFirstSecondNetworkTuple(leftTuple, rtm, it); 
                 secondLeftTuple != null; 
                 secondLeftTuple = it.next(secondLeftTuple)) {
                
                boolean isAllowed = false;
                biLinearConstraints.updateFromBiLinearTuples(biLinearContext, reteEvaluator, 
                                                           leftTuple, secondLeftTuple);
                isAllowed = biLinearConstraints.isAllowedCachedLeft(biLinearContext, 
                                                                  secondLeftTuple.getFactHandle());
                
                if (isAllowed) {
                    insertBiLinearChildTuple(trgLeftTuples,
                                           leftTuple,
                                           secondLeftTuple,
                                           sink,
                                           true);
                }
            }

            leftTuple.clearStaged();
            leftTuple = next;
        }
        
        // Reset constraint context
        biLinearConstraints.resetTupleContext(biLinearContext);
    }

    /**
     * Process left tuple deletes for BiLinearJoinNode.
     */
    public void doLeftDeletes(BetaMemory<?> bm,
                              TupleSets srcLeftTuples,
                              TupleSets trgLeftTuples,
                              TupleSets stagedLeftTuples) {
        
        TupleMemory ltm = bm.getLeftTupleMemory();

        for (TupleImpl leftTuple = srcLeftTuples.getDeleteFirst(); leftTuple != null; ) {
            TupleImpl next = leftTuple.getStagedNext();

            if (leftTuple.getMemory() != null) {
                ltm.remove(leftTuple);
            }

            TupleImpl childLeftTuple = leftTuple.getFirstChild();
            if (childLeftTuple != null) {
                PhreakNodeOperations.unlinkAndDeleteChildLeftTuple(trgLeftTuples, stagedLeftTuples, childLeftTuple);
            }

            leftTuple.clearStaged();
            leftTuple = next;
        }
    }

    /**
     * Process second left input (right memory) inserts for BiLinearJoinNode.
     * This method joins tuples from the second left input with tuples from the primary left input.
     */
    public void doRightInserts(BiLinearJoinNode biLinearJoinNode,
                               LeftTupleSink sink,
                               BetaMemory<?> bm,
                               TupleSets srcRightTuples,
                               TupleSets trgLeftTuples,
                               String currentRuleContext) {
        
        TupleMemory ltm = bm.getLeftTupleMemory();   // Memory for first left input
        TupleMemory rtm = bm.getRightTupleMemory();  // Memory for second left input
        Object contextEntry = bm.getContext();

        BiLinearBetaConstraints biLinearConstraints = biLinearJoinNode.getBiLinearConstraints();
        
        if (biLinearConstraints == null) {
            for (TupleImpl rightTuple = srcRightTuples.getInsertFirst(); rightTuple != null; ) {
                TupleImpl next = rightTuple.getStagedNext();
                rightTuple.clearStaged();
                rightTuple = next;
            }
            return;
        }
        
        BiLinearContextEntry biLinearContext = contextEntry instanceof BiLinearContextEntry ?
            (BiLinearContextEntry) contextEntry : null;
        
        if (biLinearContext == null) {
            for (TupleImpl rightTuple = srcRightTuples.getInsertFirst(); rightTuple != null; ) {
                TupleImpl next = rightTuple.getStagedNext();
                rightTuple.clearStaged();
                rightTuple = next;
            }
            return;
        }

        for (TupleImpl rightTuple = srcRightTuples.getInsertFirst(); rightTuple != null; ) {
            TupleImpl next = rightTuple.getStagedNext();

            boolean useRightMemory = biLinearJoinNode.isLeftTupleMemoryEnabled();

            // SIMPLIFIED: Use default memory for now
            if (useRightMemory) {
                rtm.add(rightTuple);
            }

            if (ltm != null && ltm.size() > 0) {
                FastIterator<TupleImpl> it = biLinearJoinNode.getLeftIterator(ltm);
                TupleImpl firstLeftTuple = biLinearJoinNode.getFirstLeftTuple(rightTuple, ltm, it);

                for (TupleImpl leftTuple = firstLeftTuple; 
                     leftTuple != null; 
                     leftTuple = it.next(leftTuple)) {
                    
                    biLinearConstraints.updateFromTuple(biLinearContext, reteEvaluator, leftTuple);
                    biLinearConstraints.updateFromBiLinearTuples(biLinearContext, reteEvaluator, 
                                                               leftTuple, rightTuple);
                    boolean isAllowed = biLinearConstraints.isAllowedCachedLeft(biLinearContext, 
                                                                      rightTuple.getFactHandle());
                    
                    if (isAllowed) {
                        insertBiLinearChildTuple(trgLeftTuples,
                                               leftTuple,
                                               rightTuple,
                                               sink,
                                               PhreakNodeOperations.useLeftMemory(biLinearJoinNode, leftTuple));
                    }
                }
            }
            
            rightTuple.clearStaged();
            rightTuple = next;
        }
        
        biLinearConstraints.resetTupleContext(biLinearContext);
    }

    /**
     * Process second left input (right memory) updates for BiLinearJoinNode.
     */
    public void doRightUpdates(BiLinearJoinNode biLinearJoinNode,
                               LeftTupleSink sink,
                               BetaMemory<?> bm,
                               TupleSets srcRightTuples,
                               TupleSets trgLeftTuples,
                               TupleSets stagedLeftTuples) {
        
        TupleMemory ltm = bm.getLeftTupleMemory();
        TupleMemory rtm = bm.getRightTupleMemory();
        Object contextEntry = bm.getContext();

        BiLinearBetaConstraints biLinearConstraints = biLinearJoinNode.getBiLinearConstraints();
        
        if (biLinearConstraints == null) {
            throw new IllegalStateException("BiLinearJoinNode must have BiLinearBetaConstraints but got null");
        }
        
        BiLinearContextEntry biLinearContext = contextEntry instanceof BiLinearContextEntry ?
            (BiLinearContextEntry) contextEntry : null;
        
        if (biLinearContext == null) {
            throw new IllegalStateException("BiLinearJoinNode must have BiLinearContextEntry but got " + 
                                          (contextEntry != null ? contextEntry.getClass().getSimpleName() : "null"));
        }

        for (TupleImpl rightTuple = srcRightTuples.getUpdateFirst(); rightTuple != null; ) {
            TupleImpl next = rightTuple.getStagedNext();

            FastIterator<TupleImpl> it = biLinearJoinNode.getLeftIterator(ltm);
            
            TupleImpl childLeftTuple = rightTuple.getFirstChild();
            if (childLeftTuple != null) {
                PhreakNodeOperations.unlinkAndDeleteChildLeftTuple(trgLeftTuples, stagedLeftTuples, childLeftTuple);
            }

            for (TupleImpl leftTuple = biLinearJoinNode.getFirstLeftTuple(rightTuple, ltm, it); 
                 leftTuple != null; 
                 leftTuple = it.next(leftTuple)) {
                
                biLinearConstraints.updateFromTuple(biLinearContext, reteEvaluator, leftTuple);
                biLinearConstraints.updateFromBiLinearTuples(biLinearContext, reteEvaluator, 
                                                           leftTuple, rightTuple);
                boolean isAllowed = biLinearConstraints.isAllowedCachedLeft(biLinearContext, 
                                                                  rightTuple.getFactHandle());
                
                if (isAllowed) {
                    insertBiLinearChildTuple(trgLeftTuples,
                                           leftTuple,
                                           rightTuple,
                                           sink,
                                           true);
                }
            }

            rightTuple.clearStaged();
            rightTuple = next;
        }
        
        biLinearConstraints.resetTupleContext(biLinearContext);
    }

    /**
     * Process second left input (right memory) deletes for BiLinearJoinNode.
     */
    public void doRightDeletes(BetaMemory<?> bm,
                               TupleSets srcRightTuples,
                               TupleSets trgLeftTuples,
                               TupleSets stagedLeftTuples) {
        
        TupleMemory rtm = bm.getRightTupleMemory();

        for (TupleImpl rightTuple = srcRightTuples.getDeleteFirst(); rightTuple != null; ) {
            TupleImpl next = rightTuple.getStagedNext();

            if (rightTuple.getMemory() != null) {
                rtm.remove(rightTuple);
            }

            TupleImpl childLeftTuple = rightTuple.getFirstChild();
            if (childLeftTuple != null) {
                PhreakNodeOperations.unlinkAndDeleteChildLeftTuple(trgLeftTuples, stagedLeftTuples, childLeftTuple);
            }

            rightTuple.clearStaged();
            rightTuple = next;
        }
    }

    /**
     * Creates a new child tuple that represents the join result of two left inputs.
     * This is the key difference from traditional joins - we're combining two left tuples.
     * Uses TupleFactory to create a proper LeftTuple that can be processed by downstream nodes.
     */
    private void insertBiLinearChildTuple(TupleSets trgLeftTuples,
                                        TupleImpl firstLeftTuple,
                                        TupleImpl secondLeftTuple,
                                        LeftTupleSink sink,
                                        boolean useLeftMemory) {
        
        // For BiLinear joins, we create a standard LeftTuple using TupleFactory
        // The second left tuple is treated as the "right" tuple in the factory method
        // This ensures compatibility with downstream processing (especially terminal nodes)
        TupleImpl childTuple = TupleFactory.createLeftTuple(
            firstLeftTuple,   // Primary left parent
            secondLeftTuple,  // Secondary left parent (treated as "right" in factory)
            sink              // Sink for this tuple
        );
        

        // Set propagation context from the first left tuple
        childTuple.setPropagationContext(firstLeftTuple.getPropagationContext());
        
        // Propagate the properly created LeftTuple
        trgLeftTuples.addInsert(childTuple);
    }
    
    /**
     * Extracts rule context from execution to enable rule-specific memory isolation
     * CRITICAL: Uses sink (RuleTerminalNode) as the primary source of rule identity
     */
    private String extractRuleContext(TupleSets srcLeftTuples, LeftTupleSink sink) {
        // PRIMARY: Try to extract from sink first (most reliable for BiLinear)
        if (sink != null) {

            // For RuleTerminalNode, we can get the rule directly
            if (sink instanceof org.drools.core.reteoo.RuleTerminalNode) {
                org.drools.core.reteoo.RuleTerminalNode rtn = 
                    (org.drools.core.reteoo.RuleTerminalNode) sink;
                if (rtn.getRule() != null) {
                    String ruleName = rtn.getRule().getName();
                    return ruleName;
                }
            }
            
            // For other sink types, try to get associated rule information
            try {
                // Check if sink has direct rule associations
                if (sink instanceof org.drools.core.common.BaseNode) {
                    org.drools.core.common.BaseNode baseNode = (org.drools.core.common.BaseNode) sink;
                    org.kie.api.definition.rule.Rule[] rules = baseNode.getAssociatedRules();
                    if (rules != null && rules.length > 0) {
                        String ruleName = rules[0].getName();
                        return ruleName;
                    }
                }
            } catch (Exception e) {
            }
        }
        
        // SECONDARY: Try to extract from tuples if available
        if (srcLeftTuples != null && srcLeftTuples.getInsertFirst() != null) {
            TupleImpl tuple = srcLeftTuples.getInsertFirst();
            if (tuple.getPropagationContext() != null) {
                // Note: PropagationContext may not have direct rule access
                String ruleName = "fromTuple";
                return ruleName;
            }
        }
        
        return "unknown";
    }
}
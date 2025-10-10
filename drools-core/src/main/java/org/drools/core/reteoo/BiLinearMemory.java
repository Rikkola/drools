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

import org.drools.base.reteoo.NodeTypeEnums;
import org.drools.core.common.TupleSets;
import org.drools.core.common.TupleSetsImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * BiLinearMemory extends BetaMemory to handle BiLinear join operations
 * with dual left inputs and cross-network coordination.
 * 
 * CRITICAL: Implements rule-specific memory isolation to prevent cross-rule contamination
 * 
 * This specialized memory type:
 * 1. Extends BetaMemory for BetaNode interface compatibility
 * 2. Handles dual left inputs (leftInput and secondLeftInput)
 * 3. Provides cross-network coordination for shared nodes
 * 4. Maintains rule-specific memory contexts to prevent tuple contamination
 * 5. Prevents ClassCastException by staying within BetaMemory hierarchy
 */
public class BiLinearMemory<C> extends BetaMemory<C> {

    // Rule-specific memory isolation to prevent cross-rule contamination
    private Map<String, TupleMemory> ruleSpecificLeftMemory;
    private Map<String, TupleMemory> ruleSpecificRightMemory;
    private Map<String, TupleSets> ruleSpecificStagedTuples;
    
    // Second network staging areas (for secondLeftInput)
    private TupleSets stagedSecondLeftTuples;
    
    // Cross-network coordination - tracks which rule networks use this shared node
    private Set<Integer> activeRuleNetworks;
    
    // Network segment mapping for cross-network operations
    private Map<Integer, SegmentMemory> networkSegments;
    
    // BiLinear-specific constraint indexing for performance
    private Map<String, Object> constraintIndex;

    /**
     * Default constructor for BiLinearMemory
     */
    public BiLinearMemory() {
        super();
        initializeBiLinearFields();
    }

    /**
     * Full constructor for BiLinearMemory
     */
    public BiLinearMemory(final TupleMemory leftTupleMemory,
                         final TupleMemory rightTupleMemory, 
                         final C context,
                         final int nodeType) {
        super(leftTupleMemory, rightTupleMemory, context, nodeType);
        initializeBiLinearFields();
    }

    /**
     * BiLinear-specific constructor with dual left memories
     */
    public BiLinearMemory(final TupleMemory leftTupleMemory,
                         final TupleMemory secondLeftTupleMemory,
                         final TupleMemory rightTupleMemory,
                         final C context) {
        super(leftTupleMemory, rightTupleMemory, context, NodeTypeEnums.BiLinearJoinNode);
        initializeBiLinearFields();
        
        // In BiLinear, rightTupleMemory actually stores second left input tuples
        // This maintains compatibility while providing dual left input semantics
    }

    /**
     * Initialize BiLinear-specific fields including rule isolation
     */
    private void initializeBiLinearFields() {
        this.ruleSpecificLeftMemory = new HashMap<>();
        this.ruleSpecificRightMemory = new HashMap<>();
        this.ruleSpecificStagedTuples = new HashMap<>();
        this.stagedSecondLeftTuples = new TupleSetsImpl();
        this.activeRuleNetworks = new HashSet<>();
        this.networkSegments = new HashMap<>(); 
        this.constraintIndex = new HashMap<>();
    }

    /**
     * Gets staged tuples for second left input (cross-network tuples)
     */
    public TupleSets getStagedSecondLeftTuples() {
        return stagedSecondLeftTuples;
    }

    /**
     * Sets staged tuples for second left input
     */
    public void setStagedSecondLeftTuples(TupleSets stagedSecondLeftTuples) {
        this.stagedSecondLeftTuples = stagedSecondLeftTuples;
    }

    /**
     * Gets the second left tuple memory (stored in rightTupleMemory for compatibility)
     */
    public TupleMemory getSecondLeftTupleMemory() {
        // In BiLinear architecture, rightTupleMemory contains second left input tuples
        return getRightTupleMemory();
    }
    
    /**
     * Gets rule-specific left tuple memory to prevent cross-rule contamination
     */
    public TupleMemory getRuleSpecificLeftMemory(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return getLeftTupleMemory(); // Fallback to default
        }
        return ruleSpecificLeftMemory.computeIfAbsent(ruleName, 
            k -> new org.drools.core.util.index.TupleIndexHashTable());
    }
    
    /**
     * Gets rule-specific right tuple memory to prevent cross-rule contamination
     */
    public TupleMemory getRuleSpecificRightMemory(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return getRightTupleMemory(); // Fallback to default
        }
        return ruleSpecificRightMemory.computeIfAbsent(ruleName, 
            k -> new org.drools.core.util.index.TupleIndexHashTable());
    }
    
    /**
     * Gets rule-specific staged tuples to prevent cross-rule contamination
     */
    public TupleSets getRuleSpecificStagedTuples(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return getStagedRightTuples(); // Fallback to default (BetaMemory only has stagedRightTuples)
        }
        return ruleSpecificStagedTuples.computeIfAbsent(ruleName, 
            k -> new TupleSetsImpl());
    }
    
    /**
     * Adds a tuple to rule-specific memory to prevent cross-rule contamination
     */
    public void addTupleToRuleMemory(String ruleName, TupleImpl tuple, boolean isLeftTuple) {
        if (ruleName == null || ruleName.isEmpty()) {
            // Fallback to default memory
            if (isLeftTuple) {
                getLeftTupleMemory().add(tuple);
            } else {
                getRightTupleMemory().add(tuple);
            }
            return;
        }
        
        TupleMemory ruleMemory = isLeftTuple ? 
            getRuleSpecificLeftMemory(ruleName) : 
            getRuleSpecificRightMemory(ruleName);
        ruleMemory.add(tuple);
    }
    
    /**
     * Gets tuples from rule-specific memory to prevent cross-rule contamination
     */
    public org.drools.core.util.FastIterator<TupleImpl> getRuleSpecificIterator(String ruleName, boolean isLeftTuple) {
        if (ruleName == null || ruleName.isEmpty()) {
            // Fallback to default memory
            TupleMemory memory = isLeftTuple ? getLeftTupleMemory() : getRightTupleMemory();
            return memory != null ? memory.fastIterator() : null;
        }
        
        TupleMemory ruleMemory = isLeftTuple ? 
            getRuleSpecificLeftMemory(ruleName) : 
            getRuleSpecificRightMemory(ruleName);
        return ruleMemory.fastIterator();
    }
    
    /**
     * Gets first tuple from rule-specific memory to prevent cross-rule contamination
     */
    public TupleImpl getRuleSpecificFirstTuple(String ruleName, TupleImpl leftTuple, boolean isLeftTuple) {
        if (ruleName == null || ruleName.isEmpty()) {
            // Fallback to default memory
            TupleMemory memory = isLeftTuple ? getLeftTupleMemory() : getRightTupleMemory();
            return memory != null ? memory.getFirst(leftTuple) : null;
        }
        
        TupleMemory ruleMemory = isLeftTuple ? 
            getRuleSpecificLeftMemory(ruleName) : 
            getRuleSpecificRightMemory(ruleName);
        return ruleMemory.getFirst(leftTuple);
    }
    
    /**
     * Checks if rule-specific memory has tuples to prevent unnecessary processing
     */
    public boolean hasRuleSpecificTuples(String ruleName, boolean isLeftTuple) {
        if (ruleName == null || ruleName.isEmpty()) {
            TupleMemory memory = isLeftTuple ? getLeftTupleMemory() : getRightTupleMemory();
            return memory != null && memory.size() > 0;
        }
        
        TupleMemory ruleMemory = isLeftTuple ? 
            ruleSpecificLeftMemory.get(ruleName) : 
            ruleSpecificRightMemory.get(ruleName);
        return ruleMemory != null && ruleMemory.size() > 0;
    }

    /**
     * Registers a rule network as active on this shared BiLinear node
     */
    public void addActiveRuleNetwork(int ruleNetworkId) {
        activeRuleNetworks.add(ruleNetworkId);
    }

    /**
     * Unregisters a rule network from this shared BiLinear node
     */
    public void removeActiveRuleNetwork(int ruleNetworkId) {
        activeRuleNetworks.remove(ruleNetworkId);
        networkSegments.remove(ruleNetworkId);
    }

    /**
     * Gets all active rule networks using this BiLinear node
     */
    public Set<Integer> getActiveRuleNetworks() {
        return new HashSet<>(activeRuleNetworks);
    }

    /**
     * Checks if a specific rule network is active
     */
    public boolean isRuleNetworkActive(int ruleNetworkId) {
        return activeRuleNetworks.contains(ruleNetworkId);
    }

    /**
     * Associates a segment memory with a rule network
     */
    public void setNetworkSegment(int ruleNetworkId, SegmentMemory segmentMemory) {
        networkSegments.put(ruleNetworkId, segmentMemory);
    }

    /**
     * Gets segment memory for a specific rule network
     */
    public SegmentMemory getNetworkSegment(int ruleNetworkId) {
        return networkSegments.get(ruleNetworkId);
    }

    /**
     * Gets all network segments
     */
    public Map<Integer, SegmentMemory> getNetworkSegments() {
        return new HashMap<>(networkSegments);
    }

    /**
     * Sets a constraint index value for BiLinear optimization
     */
    public void setConstraintIndex(String key, Object value) {
        constraintIndex.put(key, value);
    }

    /**
     * Gets a constraint index value
     */
    public Object getConstraintIndex(String key) {
        return constraintIndex.get(key);
    }

    /**
     * Checks if constraint index contains a key
     */
    public boolean hasConstraintIndex(String key) {
        return constraintIndex.containsKey(key);
    }

    /**
     * Clears constraint index
     */
    public void clearConstraintIndex() {
        constraintIndex.clear();
    }

    /**
     * Resets BiLinearMemory, clearing all tuple memories and BiLinear-specific state
     */
    @Override
    public void reset() {
        // Reset standard BetaMemory state
        super.reset();
        
        // Reset BiLinear-specific state
        stagedSecondLeftTuples.resetAll();
        activeRuleNetworks.clear();
        networkSegments.clear();
        constraintIndex.clear();
    }

    /**
     * Gets total number of active rule networks
     */
    public int getActiveRuleNetworkCount() {
        return activeRuleNetworks.size();
    }

    /**
     * Checks if this BiLinear node is shared across multiple rules
     */
    public boolean isSharedNode() {
        return activeRuleNetworks.size() > 1;
    }

    @Override
    public String toString() {
        return "BiLinearMemory{" +
               "nodeType=" + getNodeType() +
               ", activeNetworks=" + activeRuleNetworks.size() +
               ", isShared=" + isSharedNode() +
               ", leftTuples=" + (getLeftTupleMemory() != null ? getLeftTupleMemory().size() : 0) +
               ", secondLeftTuples=" + (getSecondLeftTupleMemory() != null ? getSecondLeftTupleMemory().size() : 0) +
               '}';
    }
}
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
package org.drools.core.reteoo.builder;

import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.core.common.BetaConstraints;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.ObjectSource;

/**
 * Metadata storage for deferred BiLinear node creation.
 * 
 * Instead of creating mock nodes or placeholder nodes, this class stores
 * all information needed to create a BiLinear node later when its dependencies
 * (nodes from other rules) become available.
 * 
 * This approach eliminates mock nodes entirely by deferring creation until
 * all real nodes are available.
 */
public class PendingBiLinearInfo {
    
    /** Rule name that this BiLinear request is waiting for */
    private final String waitingForRuleName;
    
    /** Rule name that wants to create the BiLinear node */
    private final String currentRuleName;
    
    /** Pattern signature to match when finding the second input */
    private final String patternSignature;
    
    /** First left input source (available now from current rule) */
    private final LeftTupleSource firstInput;
    
    /** Prepared constraints for the BiLinear node */
    private final BetaConstraints constraints;
    
    /** Snapshot of build context needed for deferred creation */
    private final BuildContextSnapshot contextSnapshot;
    
    /**
     * Creates a pending BiLinear info for deferred node creation.
     * 
     * @param waitingForRuleName Rule we're waiting for to complete
     * @param currentRuleName Current rule that wants BiLinear optimization
     * @param patternSignature Pattern hash to match in target rule
     * @param firstInput First left input source (ready now)
     * @param constraints Prepared beta constraints
     * @param contextSnapshot Build context snapshot for later use
     */
    public PendingBiLinearInfo(String waitingForRuleName,
                              String currentRuleName,
                              String patternSignature,
                              LeftTupleSource firstInput,
                              BetaConstraints constraints,
                              BuildContextSnapshot contextSnapshot) {
        this.waitingForRuleName = waitingForRuleName;
        this.currentRuleName = currentRuleName;
        this.patternSignature = patternSignature;
        this.firstInput = firstInput;
        this.constraints = constraints;
        this.contextSnapshot = contextSnapshot;
    }
    
    /**
     * Gets the rule name this BiLinear creation is waiting for.
     */
    public String getWaitingForRuleName() {
        return waitingForRuleName;
    }
    
    /**
     * Gets the rule name that wants to create the BiLinear node.
     */
    public String getCurrentRuleName() {
        return currentRuleName;
    }
    
    /**
     * Gets the pattern signature to match in the target rule.
     */
    public String getPatternSignature() {
        return patternSignature;
    }
    
    /**
     * Gets the first left input source (available from current rule).
     */
    public LeftTupleSource getFirstInput() {
        return firstInput;
    }
    
    /**
     * Gets the prepared beta constraints for the BiLinear node.
     */
    public BetaConstraints getConstraints() {
        return constraints;
    }
    
    /**
     * Gets the build context snapshot for deferred creation.
     */
    public BuildContextSnapshot getContextSnapshot() {
        return contextSnapshot;
    }
    
    @Override
    public String toString() {
        return "PendingBiLinearInfo{" +
               "current=" + currentRuleName +
               ", waitingFor=" + waitingForRuleName +
               ", pattern=" + patternSignature +
               ", firstInput=" + (firstInput != null ? firstInput.getClass().getSimpleName() + "#" + firstInput.getId() : "null") +
               '}';
    }
    
    /**
     * Snapshot of essential BuildContext state needed for deferred BiLinear node creation.
     * 
     * This captures the necessary context information at the time of deferral
     * so that BiLinear node creation can happen later with the correct context.
     */
    public static class BuildContextSnapshot {
        private final int nodeId;
        private final ObjectSource objectSource;
        private final RuleImpl rule;
        private final boolean tupleMemoryEnabled;
        private final String kieBaseId;
        
        /**
         * Creates a snapshot of essential BuildContext state.
         */
        public BuildContextSnapshot(BuildContext context) {
            this.nodeId = context.getNextNodeId();
            this.objectSource = context.getObjectSource();
            this.rule = context.getRule();
            this.tupleMemoryEnabled = context.isTupleMemoryEnabled();
            // Capture KieBase ID for proper scoping
            this.kieBaseId = context.getRuleBase() != null ? 
                           context.getRuleBase().getId() : "defaultKieBase";
        }
        
        /**
         * Gets the node ID that was reserved for this BiLinear node.
         */
        public int getNodeId() {
            return nodeId;
        }
        
        /**
         * Gets the object source from the original context.
         */
        public ObjectSource getObjectSource() {
            return objectSource;
        }
        
        /**
         * Gets the rule from the original context.
         */
        public RuleImpl getRule() {
            return rule;
        }
        
        /**
         * Gets the tuple memory enabled flag.
         */
        public boolean isTupleMemoryEnabled() {
            return tupleMemoryEnabled;
        }
        
        /**
         * Gets the KieBase ID for proper scoping.
         */
        public String getKieBaseId() {
            return kieBaseId;
        }
        
        /**
         * Creates a minimal BuildContext for BiLinear node creation.
         * 
         * Note: This creates a simplified context with only essential information.
         * For full BuildContext reconstruction, additional state may be needed.
         */
        public BuildContext toMinimalBuildContext(BuildContext currentContext) {
            // Create a minimal BuildContext using current infrastructure
            // and captured essential state
            BuildContext minimalContext = new BuildContext(
                currentContext.getRuleBase(),
                currentContext.getWorkingMemories()
            );
            
            // Set essential state from snapshot
            minimalContext.setRule(rule);
            minimalContext.setTupleMemoryEnabled(tupleMemoryEnabled);
            
            return minimalContext;
        }
        
        @Override
        public String toString() {
            return "BuildContextSnapshot{" +
                   "nodeId=" + nodeId +
                   ", rule=" + (rule != null ? rule.getName() : "null") +
                   ", objectSource=" + (objectSource != null ? objectSource.getClass().getSimpleName() + "#" + objectSource.getId() : "null") +
                   ", kieBase=" + kieBaseId +
                   '}';
        }
    }
}
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.core.common.BetaConstraints;
import org.drools.core.reteoo.BiLinearJoinNode;
import org.drools.core.reteoo.CoreComponentFactory;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.ObjectSource;

/**
 * Registry for managing shared BiLinearJoinNode instances across multiple rules.
 * 
 * This class implements Phase 6 network sharing by maintaining a registry of
 * shared BiLinearJoinNodes that can be reused across multiple rules with
 * identical join patterns, eliminating duplicate network structures and
 * enabling shared computation.
 */
public class SharedNetworkRegistry {
    
    // Map from pattern signature to shared BiLinearJoinNode
    private final Map<String, BiLinearJoinNode> sharedNodes = new ConcurrentHashMap<>();
    
    // Map from shared node to the set of rules using it
    private final Map<BiLinearJoinNode, Set<RuleImpl>> nodeUsers = new ConcurrentHashMap<>();
    
    // Map from shared node to its usage statistics
    private final Map<BiLinearJoinNode, SharedNodeStats> nodeStats = new ConcurrentHashMap<>();
    
    /**
     * Gets an existing shared BiLinearJoinNode for the given signature, or creates a new one
     * if none exists. This is the core method for network sharing.
     */
    public BiLinearJoinNode getOrCreateSharedNode(String signature,
                                                  LeftTupleSource leftInput,
                                                  ObjectSource rightInput,
                                                  BetaConstraints betaConstraints,
                                                  BuildContext context) {
        
        BiLinearJoinNode sharedNode = sharedNodes.get(signature);
        
        if (sharedNode == null) {
            // Create new shared BiLinearJoinNode using standard interface signature
            // The NodeFactory interface expects (leftInput, rightInput) and BiLinearJoinNode
            // constructor handles the dual left input scenario internally
            sharedNode = CoreComponentFactory.get()
                                           .getNodeFactoryService()
                                           .buildBiLinearJoinNode(
                                                                 betaConstraints,
                                                                 context);
            
            // Register the new shared node
            sharedNodes.put(signature, sharedNode);
            nodeUsers.put(sharedNode, new HashSet<>());
            nodeStats.put(sharedNode, new SharedNodeStats(signature));
            
        }
        
        return sharedNode;
    }
    
    /**
     * Registers a rule as a user of the specified shared node.
     */
    public void registerNodeUser(BiLinearJoinNode node, RuleImpl rule) {
        Set<RuleImpl> users = nodeUsers.get(node);
        if (users != null) {
            users.add(rule);
            
            SharedNodeStats stats = nodeStats.get(node);
            if (stats != null) {
                stats.incrementUserCount();
            }
        }
    }
    
    /**
     * Unregisters a rule as a user of the specified shared node.
     * If no rules are using the node anymore, it can be cleaned up.
     */
    public void unregisterNodeUser(BiLinearJoinNode node, RuleImpl rule) {
        Set<RuleImpl> users = nodeUsers.get(node);
        if (users != null) {
            users.remove(rule);
            
            SharedNodeStats stats = nodeStats.get(node);
            if (stats != null) {
                stats.decrementUserCount();
                
                // Clean up if no users remain
                if (users.isEmpty()) {
                    cleanupSharedNode(node);
                }
            }
        }
    }
    
    /**
     * Gets the shared node for a given signature, or null if none exists.
     */
    public BiLinearJoinNode getSharedNode(String signature) {
        return sharedNodes.get(signature);
    }
    
    /**
     * Checks if a shared node exists for the given signature.
     */
    public boolean hasSharedNode(String signature) {
        return sharedNodes.containsKey(signature);
    }
    
    /**
     * Gets the set of rules using the specified shared node.
     */
    public Set<RuleImpl> getNodeUsers(BiLinearJoinNode node) {
        Set<RuleImpl> users = nodeUsers.get(node);
        return users != null ? new HashSet<>(users) : new HashSet<>();
    }
    
    /**
     * Gets statistics for the specified shared node.
     */
    public SharedNodeStats getNodeStats(BiLinearJoinNode node) {
        return nodeStats.get(node);
    }
    
    /**
     * Gets all shared nodes in the registry.
     */
    public Set<BiLinearJoinNode> getAllSharedNodes() {
        return new HashSet<>(sharedNodes.values());
    }
    
    /**
     * Gets sharing statistics for the entire registry.
     */
    public RegistryStats getRegistryStats() {
        int totalSharedNodes = sharedNodes.size();
        int totalRuleConnections = 0;
        int totalMemorySavings = 0;
        
        for (SharedNodeStats stats : nodeStats.values()) {
            totalRuleConnections += stats.getUserCount();
            if (stats.getUserCount() > 1) {
                totalMemorySavings += stats.getUserCount() - 1; // Saved nodes
            }
        }
        
        return new RegistryStats(totalSharedNodes, totalRuleConnections, totalMemorySavings);
    }
    
    /**
     * Cleans up a shared node that is no longer used by any rules.
     */
    private void cleanupSharedNode(BiLinearJoinNode node) {
        SharedNodeStats stats = nodeStats.get(node);
        String signature = stats != null ? stats.getSignature() : "unknown";
        
        // Remove from all maps
        sharedNodes.entrySet().removeIf(entry -> entry.getValue() == node);
        nodeUsers.remove(node);
        nodeStats.remove(node);
        
    }
    
    /**
     * Clears all shared nodes from the registry. Used for testing and cleanup.
     */
    public void clear() {
        sharedNodes.clear();
        nodeUsers.clear();
        nodeStats.clear();
    }
    
    /**
     * Statistics for a shared node.
     */
    public static class SharedNodeStats {
        private final String signature;
        private int userCount = 0;
        private long creationTime = System.currentTimeMillis();
        
        public SharedNodeStats(String signature) {
            this.signature = signature;
        }
        
        public void incrementUserCount() { userCount++; }
        public void decrementUserCount() { userCount = Math.max(0, userCount - 1); }
        
        public String getSignature() { return signature; }
        public int getUserCount() { return userCount; }
        public long getCreationTime() { return creationTime; }
    }
    
    /**
     * Overall registry statistics.
     */
    public static class RegistryStats {
        private final int totalSharedNodes;
        private final int totalRuleConnections;
        private final int memorySavings;
        
        public RegistryStats(int totalSharedNodes, int totalRuleConnections, int memorySavings) {
            this.totalSharedNodes = totalSharedNodes;
            this.totalRuleConnections = totalRuleConnections;
            this.memorySavings = memorySavings;
        }
        
        public int getTotalSharedNodes() { return totalSharedNodes; }
        public int getTotalRuleConnections() { return totalRuleConnections; }
        public int getMemorySavings() { return memorySavings; }
        
        public double getSharingEfficiency() {
            return totalRuleConnections > 0 ? (double) memorySavings / totalRuleConnections : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("RegistryStats{nodes=%d, connections=%d, savings=%d, efficiency=%.2f%%}", 
                               totalSharedNodes, totalRuleConnections, memorySavings, getSharingEfficiency() * 100);
        }
    }
}
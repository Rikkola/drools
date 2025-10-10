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
import org.drools.base.rule.Pattern;

import java.util.*;

/**
 * Represents a pattern chain shared between multiple rules that can benefit
 * from BiLinear optimization. Pattern chains are sequences of patterns that
 * appear in the same order with the same constraints across different rules.
 */
public class SharedPatternChain {
    
    private final List<Pattern> patterns;
    private final String chainHash;
    private final int startIndex;
    private final int length;
    private final List<RuleChainInfo> participatingRules;
    private final String chainSignature;
    
    // Hash context storage for optimization
    private final Map<String, String> patternToHashMap;
    private final Map<String, String> constraintHashes;
    
    /**
     * Creates a new SharedPatternChain for BiLinear optimization.
     * 
     * @param patterns The ordered list of patterns in the chain
     * @param chainHash The normalized hash representing this chain
     * @param startIndex The starting index of this chain in the original rule
     * @param length The length of the pattern chain
     */
    public SharedPatternChain(List<Pattern> patterns, String chainHash, int startIndex, int length) {
        this.patterns = Collections.unmodifiableList(new ArrayList<>(patterns));
        this.chainHash = chainHash;
        this.startIndex = startIndex;
        this.length = length;
        this.participatingRules = new ArrayList<>();
        this.chainSignature = createChainSignature();
        this.patternToHashMap = new HashMap<>();
        this.constraintHashes = new HashMap<>();
        
        // Pre-compute hash context for reuse during network construction
        initializeHashContext();
    }
    
    /**
     * Enhanced constructor that accepts pre-computed hash context.
     * 
     * @param patterns The ordered list of patterns in the chain
     * @param chainHash The normalized hash representing this chain
     * @param startIndex The starting index of this chain in the original rule
     * @param length The length of the pattern chain
     * @param patternToHashMap Pre-computed pattern to hash mappings
     * @param constraintHashes Pre-computed constraint hash mappings
     */
    public SharedPatternChain(List<Pattern> patterns, String chainHash, int startIndex, int length,
                            Map<String, String> patternToHashMap, Map<String, String> constraintHashes) {
        this.patterns = Collections.unmodifiableList(new ArrayList<>(patterns));
        this.chainHash = chainHash;
        this.startIndex = startIndex;
        this.length = length;
        this.participatingRules = new ArrayList<>();
        this.chainSignature = createChainSignature();
        this.patternToHashMap = new HashMap<>(patternToHashMap != null ? patternToHashMap : Collections.emptyMap());
        this.constraintHashes = new HashMap<>(constraintHashes != null ? constraintHashes : Collections.emptyMap());
    }
    
    /**
     * Initializes hash context for pattern reuse during network construction.
     */
    private void initializeHashContext() {
        // Pre-compute individual pattern hashes for quick lookup during construction
        for (int i = 0; i < patterns.size(); i++) {
            Pattern pattern = patterns.get(i);
            String objectType = pattern.getObjectType().getClassName();
            String patternKey = createPatternKey(objectType, i);
            
            // Store pattern-specific hash for reuse
            String patternHash = PatternHashComparator.generateNormalizedHash(pattern);
            patternToHashMap.put(patternKey, patternHash);
            
            // Store constraint hash if patterns have constraints
            if (!pattern.getConstraints().isEmpty()) {
                String constraintKey = patternKey + "_constraints";
                String constraintHash = createConstraintHash(pattern);
                constraintHashes.put(constraintKey, constraintHash);
            }
        }
        
        // Store chain combinations for common join scenarios
        if (patterns.size() >= 2) {
            // Store adjacent pattern pair hashes for join node construction
            for (int i = 0; i < patterns.size() - 1; i++) {
                String leftType = patterns.get(i).getObjectType().getClassName();
                String rightType = patterns.get(i + 1).getObjectType().getClassName();
                String pairKey = createSourcePairKey(leftType, rightType);
                String pairHash = createPairHash(patterns.get(i), patterns.get(i + 1));
                patternToHashMap.put(pairKey, pairHash);
            }
        }
    }
    
    /**
     * Creates a constraint hash for a pattern.
     */
    private String createConstraintHash(Pattern pattern) {
        // Use existing constraint normalization logic if available
        StringBuilder constraintHash = new StringBuilder();
        pattern.getConstraints().forEach(constraint -> 
            constraintHash.append(constraint.toString()).append("|"));
        return constraintHash.toString();
    }
    
    /**
     * Creates a hash for a pair of patterns (common join scenario).
     */
    private String createPairHash(Pattern left, Pattern right) {
        return PatternHashComparator.generateNormalizedHash(left) + "|" + 
               PatternHashComparator.generateNormalizedHash(right);
    }
    
    /**
     * Creates a unique key for a pattern at a specific index.
     */
    private String createPatternKey(String objectType, int index) {
        return objectType + "_" + index;
    }
    
    /**
     * Creates a key for source pair lookup (used in join node construction).
     */
    private String createSourcePairKey(String leftType, String rightType) {
        return leftType + "->" + rightType;
    }
    
    /**
     * Creates a unique signature for this pattern chain based on object types and structure.
     */
    private String createChainSignature() {
        StringBuilder sig = new StringBuilder();
        sig.append("CHAIN[").append(startIndex).append("->").append(length).append("]:");
        
        for (int i = 0; i < patterns.size(); i++) {
            if (i > 0) sig.append("->");
            Pattern pattern = patterns.get(i);
            sig.append(pattern.getObjectType().getClassName());
            
            // Add constraint count for structural information
            if (!pattern.getConstraints().isEmpty()) {
                sig.append("(").append(pattern.getConstraints().size()).append(")");
            }
        }
        
        return sig.toString();
    }
    
    /**
     * Adds a rule that participates in this shared pattern chain.
     * 
     * @param rule The rule that contains this pattern chain
     * @param ruleStartIndex The starting index of this chain within the rule
     * @param ruleName The name of the rule for identification
     */
    public void addParticipatingRule(RuleImpl rule, int ruleStartIndex, String ruleName) {
        RuleChainInfo ruleInfo = new RuleChainInfo(rule, ruleStartIndex, ruleName);
        if (!participatingRules.contains(ruleInfo)) {
            participatingRules.add(ruleInfo);
        }
    }
    
    /**
     * Checks if this pattern chain can be optimized with BiLinear nodes.
     * 
     * @return true if this chain is shared by multiple rules
     */
    public boolean canOptimizeWithBiLinear() {
        return participatingRules.size() >= 2;
    }
    
    /**
     * Gets the list of patterns in this chain.
     */
    public List<Pattern> getPatterns() {
        return patterns;
    }
    
    /**
     * Gets the normalized hash for this pattern chain.
     */
    public String getChainHash() {
        return chainHash;
    }
    
    /**
     * Gets the starting index of this chain in the original rule.
     */
    public int getStartIndex() {
        return startIndex;
    }
    
    /**
     * Gets the length of this pattern chain.
     */
    public int getLength() {
        return length;
    }
    
    /**
     * Gets the rules that participate in this shared pattern chain.
     */
    public List<RuleChainInfo> getParticipatingRules() {
        return Collections.unmodifiableList(participatingRules);
    }
    
    /**
     * Gets the chain signature for identification and debugging.
     */
    public String getChainSignature() {
        return chainSignature;
    }
    
    /**
     * Gets the object types involved in this pattern chain.
     */
    public List<String> getObjectTypes() {
        List<String> types = new ArrayList<>();
        for (Pattern pattern : patterns) {
            types.add(pattern.getObjectType().getClassName());
        }
        return types;
    }
    
    /**
     * Gets the first pattern in the chain.
     */
    public Pattern getFirstPattern() {
        return patterns.isEmpty() ? null : patterns.get(0);
    }
    
    /**
     * Gets the last pattern in the chain.
     */
    public Pattern getLastPattern() {
        return patterns.isEmpty() ? null : patterns.get(patterns.size() - 1);
    }
    
    /**
     * Calculates the optimization potential score based on chain length and rule participation.
     */
    public int getOptimizationScore() {
        // Longer chains and more participating rules provide better optimization potential
        return length * participatingRules.size();
    }
    
    // Hash context access methods for network construction optimization
    
    /**
     * Gets the pre-computed hash for a source pair (used in join node construction).
     * This allows reusing hashes calculated during detection phase.
     * 
     * @param leftType The left source object type
     * @param rightType The right source object type
     * @return The pre-computed hash, or null if not found
     */
    public String getSourcePairHash(String leftType, String rightType) {
        String pairKey = createSourcePairKey(leftType, rightType);
        return patternToHashMap.get(pairKey);
    }
    
    /**
     * Gets the pre-computed hash for a specific pattern by object type and index.
     * 
     * @param objectType The object type of the pattern
     * @param index The index of the pattern in the chain
     * @return The pre-computed pattern hash, or null if not found
     */
    public String getPatternHash(String objectType, int index) {
        String patternKey = createPatternKey(objectType, index);
        return patternToHashMap.get(patternKey);
    }
    
    /**
     * Gets the pre-computed constraint hash for a pattern.
     * 
     * @param objectType The object type of the pattern
     * @param index The index of the pattern in the chain
     * @return The pre-computed constraint hash, or null if not found
     */
    public String getConstraintHash(String objectType, int index) {
        String constraintKey = createPatternKey(objectType, index) + "_constraints";
        return constraintHashes.get(constraintKey);
    }
    
    /**
     * Checks if this pattern chain contains a specific source pair combination.
     * 
     * @param leftType The left source object type
     * @param rightType The right source object type
     * @return true if this pattern chain contains the source pair
     */
    public boolean containsSourcePair(String leftType, String rightType) {
        // Check if any adjacent patterns match the source pair
        for (int i = 0; i < patterns.size() - 1; i++) {
            String leftPattern = patterns.get(i).getObjectType().getClassName();
            String rightPattern = patterns.get(i + 1).getObjectType().getClassName();
            if (leftPattern.equals(leftType) && rightPattern.equals(rightType)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets all pre-computed pattern-to-hash mappings for debugging and advanced usage.
     * 
     * @return Unmodifiable map of pattern keys to hashes
     */
    public Map<String, String> getPatternToHashMap() {
        return Collections.unmodifiableMap(patternToHashMap);
    }
    
    /**
     * Gets all pre-computed constraint hashes for debugging and advanced usage.
     * 
     * @return Unmodifiable map of constraint keys to hashes
     */
    public Map<String, String> getConstraintHashes() {
        return Collections.unmodifiableMap(constraintHashes);
    }
    
    @Override
    public String toString() {
        return "SharedPatternChain{" +
                "length=" + length +
                ", startIndex=" + startIndex +
                ", participatingRules=" + participatingRules.size() +
                ", signature='" + chainSignature + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SharedPatternChain that = (SharedPatternChain) obj;
        return Objects.equals(chainHash, that.chainHash);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(chainHash);
    }
    
    /**
     * Information about a rule that participates in a shared pattern chain.
     */
    public static class RuleChainInfo {
        private final RuleImpl rule;
        private final int startIndex;
        private final String ruleName;
        
        public RuleChainInfo(RuleImpl rule, int startIndex, String ruleName) {
            this.rule = rule;
            this.startIndex = startIndex;
            this.ruleName = ruleName;
        }
        
        public RuleImpl getRule() {
            return rule;
        }
        
        public int getStartIndex() {
            return startIndex;
        }
        
        public String getRuleName() {
            return ruleName;
        }
        
        @Override
        public String toString() {
            return "RuleChainInfo{" +
                    "ruleName='" + ruleName + '\'' +
                    ", startIndex=" + startIndex +
                    '}';
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RuleChainInfo that = (RuleChainInfo) obj;
            return startIndex == that.startIndex &&
                    Objects.equals(rule, that.rule) &&
                    Objects.equals(ruleName, that.ruleName);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(rule, startIndex, ruleName);
        }
    }
}
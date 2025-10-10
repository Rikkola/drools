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
import org.drools.base.rule.GroupElement;
import org.drools.base.rule.Pattern;
import org.drools.base.rule.RuleConditionElement;

import java.util.*;

/**
 * Generates combined hashes for pattern chains in rules, creating hashes for 
 * progressively shorter "tails" of the pattern sequence.
 * 
 * For example, if a rule has patterns [A, B, C]:
 * - Creates hash for [A, B, C] (full chain)
 * - Creates hash for [B, C] (tail starting at position 1)
 * - Creates hash for [C] (tail starting at position 2)
 * 
 * This enables efficient matching of partial pattern sequences across rules.
 */
public class PatternChainHasher {
    
    /**
     * Generates chain hashes for all pattern tails in a rule.
     * 
     * @param rule The rule to analyze
     * @param kieBaseId The KieBase identifier for hash scoping
     * @return ChainHashResult containing all tail hashes
     */
    public static ChainHashResult generateChainHashes(RuleImpl rule, String kieBaseId) {
        if (rule == null) {
            return new ChainHashResult(Collections.emptyList(), Collections.emptyMap());
        }
        
        List<Pattern> patterns = extractPatternsFromRule(rule);
        return generateChainHashes(kieBaseId, patterns);
    }
    
    /**
     * Generates chain hashes for all pattern tails in a rule using package name (for backward compatibility).
     * 
     * @param rule The rule to analyze
     * @return ChainHashResult containing all tail hashes
     * @deprecated Use generateChainHashes(RuleImpl rule, String kieBaseId) for proper KieBase scoping
     */
    @Deprecated
    public static ChainHashResult generateChainHashes(RuleImpl rule) {
        if (rule == null) {
            return new ChainHashResult(Collections.emptyList(), Collections.emptyMap());
        }
        
        List<Pattern> patterns = extractPatternsFromRule(rule);
        return generateChainHashes(rule.getPackageName(), patterns);
    }
    
    /**
     * Generates chain hashes for all pattern tails from a list of patterns.
     *
     * @param identifier  The scope identifier (KieBase ID or package name) for hash scoping
     * @param patterns    The ordered list of patterns
     * @return ChainHashResult containing all tail hashes
     */
    public static ChainHashResult generateChainHashes(String identifier, List<Pattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return new ChainHashResult(Collections.emptyList(), Collections.emptyMap());
        }
        
        Map<Integer, String> tailHashes = new LinkedHashMap<>();
        List<TailHash> tailHashList = new ArrayList<>();
        
        // Generate hash for each tail starting from position i
        for (int startIndex = 0; startIndex < patterns.size(); startIndex++) {
            List<Pattern> tailPatterns = patterns.subList(startIndex, patterns.size());
            String combinedHash = generateCombinedHash(tailPatterns, startIndex);
            
            tailHashes.put(startIndex, combinedHash);
            tailHashList.add(new TailHash(startIndex, tailPatterns.size(), combinedHash, new ArrayList<>(tailPatterns)));

            // Use "::" separator for proper scoping (KieBase::hash format)
            String scopedHash = identifier.contains("::") ? identifier + combinedHash : identifier + "::" + combinedHash;
            tailPatterns.get(0).setTailHash(scopedHash);
        }
        
        return new ChainHashResult(tailHashList, tailHashes);
    }
    
    /**
     * Generates a combined hash for a sequence of patterns.
     * 
     * @param patterns The patterns to hash
     * @param startIndex The starting index in the original chain (for context)
     * @return Combined hash string
     */
    public static String generateCombinedHash(List<Pattern> patterns, int startIndex) {
        if (patterns == null || patterns.isEmpty()) {
            return "EMPTY_CHAIN";
        }
        
        StringBuilder chainHash = new StringBuilder();
        // For BiLinear detection, we only care about the pattern sequence, not the position
        chainHash.append("CHAIN[").append(patterns.size()).append("]:");
        
        // Generate individual pattern hashes and combine them
        List<String> patternHashes = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) {
            Pattern pattern = patterns.get(i);
            String patternHash = PatternHashComparator.generateNormalizedHash(pattern);
            
            // Include position information for ordering sensitivity
            String positionedHash = "P" + i + ":" + patternHash;
            patternHashes.add(positionedHash);
        }
        
        // Join all pattern hashes with a separator
        chainHash.append(String.join("|", patternHashes));
        
        return chainHash.toString();
    }
    
    /**
     * Extracts all patterns from a rule's condition elements.
     * 
     * @param rule The rule to analyze
     * @return Ordered list of patterns
     */
    public static List<Pattern> extractPatternsFromRule(RuleImpl rule) {
        List<Pattern> patterns = new ArrayList<>();
        
        if (rule != null && rule.getLhs() != null) {
            collectPatternsFromRCE(rule.getLhs(), patterns);
        }
        
        return patterns;
    }
    
    /**
     * Recursively collects all Pattern objects from rule condition elements.
     * 
     * @param rce The rule condition element to traverse
     * @param patterns The list to collect patterns into
     */
    private static void collectPatternsFromRCE(RuleConditionElement rce, List<Pattern> patterns) {
        if (rce instanceof Pattern) {
            patterns.add((Pattern) rce);
        } else if (rce instanceof GroupElement) {
            GroupElement ge = (GroupElement) rce;
            for (RuleConditionElement child : ge.getChildren()) {
                collectPatternsFromRCE(child, patterns);
            }
        }
        // Add support for other RCE types as needed
    }

    /**
     * Result class containing all chain hash information for a rule.
     */
    public static class ChainHashResult {
        private final List<TailHash> tailHashes;
        private final Map<Integer, String> hashByStartIndex;
        
        public ChainHashResult(List<TailHash> tailHashes, Map<Integer, String> hashByStartIndex) {
            this.tailHashes = Collections.unmodifiableList(tailHashes);
            this.hashByStartIndex = Collections.unmodifiableMap(hashByStartIndex);
        }
        
        public List<TailHash> getTailHashes() {
            return tailHashes;
        }
        
        public Map<Integer, String> getHashByStartIndex() {
            return hashByStartIndex;
        }
        
        public String getFullChainHash() {
            return hashByStartIndex.get(0);
        }
        
        public String getTailHash(int startIndex) {
            return hashByStartIndex.get(startIndex);
        }
        
        @Override
        public String toString() {
            return "ChainHashResult{" +
                    "tailCount=" + tailHashes.size() +
                    ", fullChainHash='" + getFullChainHash() + '\'' +
                    '}';
        }
    }
    
    /**
     * Information about a specific tail hash in a pattern chain.
     */
    public static class TailHash {
        private final int startIndex;
        private final int length;
        private final String hash;
        private final List<Pattern> patterns;
        
        public TailHash(int startIndex, int length, String hash, List<Pattern> patterns) {
            this.startIndex = startIndex;
            this.length = length;
            this.hash = hash;
            this.patterns = Collections.unmodifiableList(patterns);
        }
        
        public int getStartIndex() {
            return startIndex;
        }
        
        public int getLength() {
            return length;
        }
        
        public String getHash() {
            return hash;
        }
        
        public List<Pattern> getPatterns() {
            return patterns;
        }
        
        @Override
        public String toString() {
            return "TailHash{" +
                    "startIndex=" + startIndex +
                    ", length=" + length +
                    ", hash='" + hash.substring(0, Math.min(50, hash.length())) + "...'" +
                    '}';
        }
    }

}
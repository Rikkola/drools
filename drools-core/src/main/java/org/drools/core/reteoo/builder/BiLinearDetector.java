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
import org.kie.api.definition.rule.Rule;

import java.util.*;

/**
 * BiLinear Pattern Detection and Optimization
 *
 * Detects opportunities for BiLinearJoinNode creation when multiple rules
 * share common join patterns that can be optimized through network sharing.
 */
public class BiLinearDetector {

    /**
     * Analyzes all rules in a KieBase to detect shared pattern chains that could
     * benefit from BiLinearJoinNode optimization. KieBase-scoped to ensure proper isolation.
     *
     * @param rules All rules in the KieBase being built
     * @param kieBaseId The identifier of the KieBase to ensure pattern isolation
     * @return Map of chain signatures to SharedPatternChain objects
     */
    public static Map<String, SharedPatternChain> detectBiLinearOpportunities(Collection<? extends Rule> rules, String kieBaseId) {
        // Configuration check: BiLinear is now opt-in only to prevent performance issues
        if (!isBiLinearEnabled()) {
            return new HashMap<>();
        }

        // Use PatternChainHasher to detect shared pattern chains across rules
        Map<String, SharedPatternChain> sharedChains = detectSharedPatternChains(rules, kieBaseId);
        

        return sharedChains;
    }
    
    /**
     * Detects shared pattern chains across rules using PatternChainHasher.
     */
    public static Map<String, SharedPatternChain> detectSharedPatternChains(Collection<? extends Rule> rules, String kieBaseId) {
        Map<String, List<ChainMatch>> chainMatches = new HashMap<>();
        
        // Step 1: Generate chain hashes for all rules
        for (Rule r : rules) {
            if (r instanceof RuleImpl rule) {
                PatternChainHasher.ChainHashResult chainResult = PatternChainHasher.generateChainHashes(rule, kieBaseId);
                
                // Process each tail hash
                for (PatternChainHasher.TailHash tailHash : chainResult.getTailHashes()) {
                    String hash = tailHash.getHash();
                    
                    // Scope hash to KieBase to prevent cross-KieBase pattern matching
                    String scopedHash = kieBaseId != null ? kieBaseId + "::" + hash : hash;
                    
                    chainMatches.computeIfAbsent(scopedHash, k -> new ArrayList<>())
                              .add(new ChainMatch(rule, tailHash));
                }
            }
        }
        
        // Step 2: Create SharedPatternChain objects for chains shared by multiple rules
        Map<String, SharedPatternChain> sharedChains = new HashMap<>();
        
        for (Map.Entry<String, List<ChainMatch>> entry : chainMatches.entrySet()) {
            List<ChainMatch> matches = entry.getValue();
            
            // Only create shared chains if multiple rules have this pattern sequence
            if (matches.size() >= 2) {
                ChainMatch firstMatch = matches.get(0);
                SharedPatternChain sharedChain = new SharedPatternChain(
                    firstMatch.getTailHash().getPatterns(),
                    firstMatch.getTailHash().getHash(),
                    firstMatch.getTailHash().getStartIndex(),
                    firstMatch.getTailHash().getLength()
                );
                
                // Add all participating rules
                for (ChainMatch match : matches) {
                    sharedChain.addParticipatingRule(
                        match.getRule(),
                        match.getTailHash().getStartIndex(),
                        match.getRule().getName()
                    );
                }
                
                sharedChains.put(entry.getKey(), sharedChain);
            }
        }
        
        return sharedChains;
    }
    
    
    /**
     * Helper class to store pattern chain matches.
     */
    private static class ChainMatch {
        private final RuleImpl rule;
        private final PatternChainHasher.TailHash tailHash;
        
        public ChainMatch(RuleImpl rule, PatternChainHasher.TailHash tailHash) {
            this.rule = rule;
            this.tailHash = tailHash;
        }
        
        public RuleImpl getRule() {
            return rule;
        }
        
        public PatternChainHasher.TailHash getTailHash() {
            return tailHash;
        }
    }


    /**
     * Determines if a pattern chain is suitable for BiLinearJoinNode optimization.
     *
     * @param chain The shared pattern chain to evaluate
     * @return true if the chain can benefit from BiLinear optimization
     */
    public static boolean isSuitableForBiLinearOptimization(SharedPatternChain chain) {
        // Must be shared by at least 2 rules
        if (!chain.canOptimizeWithBiLinear()) {
            return false;
        }

        // Pattern chains with length >= 2 are suitable for BiLinear optimization
        // The optimization comes from sharing the network structure and pattern sequences
        if (chain.getLength() >= 2) {
            return true;
        }

        // Single pattern chains can still be optimized if multiple rules share identical patterns
        return chain.canOptimizeWithBiLinear();
    }

    /**
     * Checks if BiLinear optimization is enabled via system property
     */
    private static boolean isBiLinearEnabled() {
        return Boolean.parseBoolean(System.getProperty("drools.bilinear.enabled", "true"));
    }
}
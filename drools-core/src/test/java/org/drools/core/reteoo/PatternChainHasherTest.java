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

import org.drools.base.base.ClassObjectType;
import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.base.rule.GroupElement;
import org.drools.base.rule.Pattern;
import org.drools.core.reteoo.builder.PatternChainHasher;
import org.drools.core.test.model.Cheese;
import org.drools.core.test.model.Person;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for PatternChainHasher functionality.
 */
public class PatternChainHasherTest {

    @Test
    public void testEmptyPatternList() {
        PatternChainHasher.ChainHashResult result = PatternChainHasher.generateChainHashes("default", Arrays.asList());
        
        assertThat(result.getTailHashes())
            .as("Empty pattern list should produce empty tail hashes")
            .isEmpty();
            
        assertThat(result.getHashByStartIndex())
            .as("Empty pattern list should produce empty hash map")
            .isEmpty();
    }
    
    @Test
    public void testSinglePattern() {
        Pattern pattern = new Pattern(0, new ClassObjectType(Person.class), "$person");
        List<Pattern> patterns = Arrays.asList(pattern);
        
        PatternChainHasher.ChainHashResult result = PatternChainHasher.generateChainHashes("default", patterns);
        
        assertThat(result.getTailHashes())
            .as("Single pattern should produce one tail hash")
            .hasSize(1);
            
        PatternChainHasher.TailHash tailHash = result.getTailHashes().get(0);
        assertThat(tailHash.getStartIndex()).isEqualTo(0);
        assertThat(tailHash.getLength()).isEqualTo(1);
        assertThat(tailHash.getPatterns()).hasSize(1);
        assertThat(tailHash.getHash()).contains("CHAIN[1]");
    }
    
    @Test
    public void testThreePatternChain() {
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person");
        Pattern pattern2 = new Pattern(1, new ClassObjectType(Cheese.class), "$cheese");
        Pattern pattern3 = new Pattern(2, new ClassObjectType(Person.class), "$person2");
        
        List<Pattern> patterns = Arrays.asList(pattern1, pattern2, pattern3);
        
        PatternChainHasher.ChainHashResult result = PatternChainHasher.generateChainHashes("default", patterns);
        
        assertThat(result.getTailHashes())
            .as("Three patterns should produce three tail hashes")
            .hasSize(3);
            
        // Check full chain (starting at index 0)
        PatternChainHasher.TailHash fullChain = result.getTailHashes().get(0);
        assertThat(fullChain.getStartIndex()).isEqualTo(0);
        assertThat(fullChain.getLength()).isEqualTo(3);
        assertThat(fullChain.getHash()).contains("CHAIN[3]");
        
        // Check tail starting at index 1
        PatternChainHasher.TailHash tail1 = result.getTailHashes().get(1);
        assertThat(tail1.getStartIndex()).isEqualTo(1);
        assertThat(tail1.getLength()).isEqualTo(2);
        assertThat(tail1.getHash()).contains("CHAIN[2]");
        
        // Check tail starting at index 2 (last pattern only)
        PatternChainHasher.TailHash tail2 = result.getTailHashes().get(2);
        assertThat(tail2.getStartIndex()).isEqualTo(2);
        assertThat(tail2.getLength()).isEqualTo(1);
        assertThat(tail2.getHash()).contains("CHAIN[1]");
    }
    
    @Test
    public void testChainHashContainsPatternOrder() {
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person");
        Pattern pattern2 = new Pattern(1, new ClassObjectType(Cheese.class), "$cheese");
        
        List<Pattern> patterns = Arrays.asList(pattern1, pattern2);
        
        PatternChainHasher.ChainHashResult result = PatternChainHasher.generateChainHashes("default", patterns);
        
        String fullChainHash = result.getFullChainHash();
        
        assertThat(fullChainHash)
            .as("Chain hash should contain position information")
            .contains("P0:")  // Pattern at position 0
            .contains("P1:"); // Pattern at position 1
    }
    
    @Test
    public void testChainHashWithConstraints() {
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person");
        pattern1.addConstraint(new BiLinearDetectionTest.MockAlphaConstraint("age", 42));
        
        Pattern pattern2 = new Pattern(1, new ClassObjectType(Cheese.class), "$cheese");
        pattern2.addConstraint(new BiLinearDetectionTest.MockAlphaConstraint("type", "stilton"));
        
        Pattern pattern3 = new Pattern(2, new ClassObjectType(Person.class), "$person2");
        pattern3.addConstraint(new BiLinearDetectionTest.MockAlphaConstraint("name", "John"));
        
        List<Pattern> patterns = Arrays.asList(pattern1, pattern2, pattern3);
        
        PatternChainHasher.ChainHashResult result = PatternChainHasher.generateChainHashes("default", patterns);
        
        assertThat(result.getTailHashes())
            .as("Should generate tail hashes for patterns with constraints")
            .hasSize(3);
            
        String fullChainHash = result.getFullChainHash();
        assertThat(fullChainHash)
            .as("Chain hash should contain constraint information")
            .contains("CONSTRAINTS:");
    }
    
    @Test
    public void testConstraintAwareChainComparison() {
        // Create two chains with similar structure but different constraint values
        Pattern chain1_pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person1");
        chain1_pattern1.addConstraint(new BiLinearDetectionTest.MockAlphaConstraint("age", 42));
        
        Pattern chain1_pattern2 = new Pattern(1, new ClassObjectType(Cheese.class), "$cheese1");
        chain1_pattern2.addConstraint(new BiLinearDetectionTest.MockAlphaConstraint("type", "stilton"));
        
        Pattern chain2_pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person2");
        chain2_pattern1.addConstraint(new BiLinearDetectionTest.MockAlphaConstraint("age", 35)); // Different age
        
        Pattern chain2_pattern2 = new Pattern(1, new ClassObjectType(Cheese.class), "$cheese2");
        chain2_pattern2.addConstraint(new BiLinearDetectionTest.MockAlphaConstraint("type", "stilton")); // Same type
        
        List<Pattern> chain1 = Arrays.asList(chain1_pattern1, chain1_pattern2);
        List<Pattern> chain2 = Arrays.asList(chain2_pattern1, chain2_pattern2);
        
        String hash1 = PatternChainHasher.generateCombinedHash(chain1, 0);
        String hash2 = PatternChainHasher.generateCombinedHash(chain2, 0);
        
        assertThat(hash1)
            .as("Chains with different constraint values should have different hashes")
            .isNotEqualTo(hash2);
    }
    
    @Test
    public void testIdenticalConstraintsProduceSameHash() {
        // Create two chains with identical constraints but different variable names
        Pattern chain1_pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person1");
        chain1_pattern1.addConstraint(new BiLinearDetectionTest.MockAlphaConstraint("age", 42));
        
        Pattern chain1_pattern2 = new Pattern(1, new ClassObjectType(Cheese.class), "$cheese1");
        chain1_pattern2.addConstraint(new BiLinearDetectionTest.MockAlphaConstraint("type", "stilton"));
        
        Pattern chain2_pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$p");  // Different variable name
        chain2_pattern1.addConstraint(new BiLinearDetectionTest.MockAlphaConstraint("age", 42)); // Same constraint
        
        Pattern chain2_pattern2 = new Pattern(1, new ClassObjectType(Cheese.class), "$c"); // Different variable name
        chain2_pattern2.addConstraint(new BiLinearDetectionTest.MockAlphaConstraint("type", "stilton")); // Same constraint
        
        List<Pattern> chain1 = Arrays.asList(chain1_pattern1, chain1_pattern2);
        List<Pattern> chain2 = Arrays.asList(chain2_pattern1, chain2_pattern2);
        
        String hash1 = PatternChainHasher.generateCombinedHash(chain1, 0);
        String hash2 = PatternChainHasher.generateCombinedHash(chain2, 0);
        
        assertThat(hash1)
            .as("Chains with identical constraints but different variable names should have same hash")
            .isEqualTo(hash2);
    }
    
    @Test
    public void testDifferentOrderProducesDifferentHashes() {
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person");
        Pattern pattern2 = new Pattern(1, new ClassObjectType(Cheese.class), "$cheese");
        
        List<Pattern> order1 = Arrays.asList(pattern1, pattern2);
        List<Pattern> order2 = Arrays.asList(pattern2, pattern1);
        
        String hash1 = PatternChainHasher.generateCombinedHash(order1, 0);
        String hash2 = PatternChainHasher.generateCombinedHash(order2, 0);
        
        assertThat(hash1)
            .as("Different pattern orders should produce different hashes")
            .isNotEqualTo(hash2);
    }
    
    @Test
    public void testHashByStartIndexAccess() {
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person");
        Pattern pattern2 = new Pattern(1, new ClassObjectType(Cheese.class), "$cheese");
        Pattern pattern3 = new Pattern(2, new ClassObjectType(Person.class), "$person2");
        
        List<Pattern> patterns = Arrays.asList(pattern1, pattern2, pattern3);
        
        PatternChainHasher.ChainHashResult result = PatternChainHasher.generateChainHashes("default", patterns);
        
        assertThat(result.getTailHash(0))
            .as("Should be able to access hash by start index 0")
            .isNotNull()
            .contains("CHAIN[3]");
            
        assertThat(result.getTailHash(1))
            .as("Should be able to access hash by start index 1")
            .isNotNull()
            .contains("CHAIN[2]");
            
        assertThat(result.getTailHash(2))
            .as("Should be able to access hash by start index 2")
            .isNotNull()
            .contains("CHAIN[1]");
    }
    
    @Test
    public void testExtractPatternsFromRule() {
        RuleImpl rule = createMockRule();
        
        List<Pattern> patterns = PatternChainHasher.extractPatternsFromRule(rule);
        
        assertThat(patterns)
            .as("Should extract patterns from rule")
            .isNotEmpty();
    }
    
    @Test
    public void testGenerateChainHashesFromRule() {
        RuleImpl rule = createMockRule();
        
        PatternChainHasher.ChainHashResult result = PatternChainHasher.generateChainHashes(rule);
        
        assertThat(result.getTailHashes())
            .as("Should generate chain hashes from rule")
            .isNotEmpty();
    }

    @Test
    public void testNullRuleHandling() {
        PatternChainHasher.ChainHashResult result = PatternChainHasher.generateChainHashes((RuleImpl) null);
        
        assertThat(result.getTailHashes())
            .as("Null rule should produce empty result")
            .isEmpty();
            
        assertThat(result.getHashByStartIndex())
            .as("Null rule should produce empty hash map")
            .isEmpty();
    }
    
    @Test
    public void testTailHashToString() {
        Pattern pattern = new Pattern(0, new ClassObjectType(Person.class), "$person");
        String hash = PatternChainHasher.generateCombinedHash(Arrays.asList(pattern), 0);
        
        PatternChainHasher.TailHash tailHash = new PatternChainHasher.TailHash(0, 1, hash, Arrays.asList(pattern));
        
        String toString = tailHash.toString();
        
        assertThat(toString)
            .as("TailHash toString should contain key information")
            .contains("startIndex=0")
            .contains("length=1");
    }
    
    @Test
    public void testChainHashResultToString() {
        Pattern pattern = new Pattern(0, new ClassObjectType(Person.class), "$person");
        List<Pattern> patterns = Arrays.asList(pattern);
        
        PatternChainHasher.ChainHashResult result = PatternChainHasher.generateChainHashes("default", patterns);
        
        String toString = result.toString();
        
        assertThat(toString)
            .as("ChainHashResult toString should contain key information")
            .contains("tailCount=1")
            .contains("fullChainHash=");
    }
    
    // Helper methods
    
    private RuleImpl createMockRule() {
        RuleImpl rule = new RuleImpl("testRule");
        rule.setPackage("org.drools.test"); // Set package name to avoid null identifier
        
        // Create a simple group element with patterns
        GroupElement lhs = new GroupElement(GroupElement.Type.AND);
        lhs.addChild(new Pattern(0, new ClassObjectType(Person.class), "$person"));
        lhs.addChild(new Pattern(1, new ClassObjectType(Cheese.class), "$cheese"));
        
        rule.setLhs(lhs);
        
        return rule;
    }
    
    private RuleImpl createMockRuleWithPatterns(String ruleName, Pattern... patterns) {
        RuleImpl rule = new RuleImpl(ruleName);
        rule.setPackage("org.drools.test"); // Set package name to avoid null identifier
        
        GroupElement lhs = new GroupElement(GroupElement.Type.AND);
        for (Pattern pattern : patterns) {
            lhs.addChild(pattern);
        }
        
        rule.setLhs(lhs);
        
        return rule;
    }
}
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
import org.drools.base.rule.Declaration;
import org.drools.base.rule.Pattern;
import org.drools.base.rule.constraint.AlphaNodeFieldConstraint;
import org.drools.base.rule.constraint.BetaConstraint;
import org.drools.base.rule.constraint.Constraint;
import org.drools.base.base.ValueResolver;
import org.kie.api.runtime.rule.FactHandle;
import org.drools.core.reteoo.builder.PatternHashComparator;
import org.drools.core.test.model.Cheese;
import org.drools.core.test.model.Person;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for PatternHashComparator functionality.
 */
public class PatternHashComparatorTest {

    @Test
    public void testIdenticalPatternsWithDifferentVariableNames() {
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person");
        Pattern pattern2 = new Pattern(0, new ClassObjectType(Person.class), "$p");
        
        boolean result = PatternHashComparator.arePatternStructuresEqual(pattern1, pattern2);
        
        assertThat(result)
            .as("Patterns with same structure but different variable names should be equal")
            .isTrue();
    }
    
    @Test
    public void testDifferentObjectTypes() {
        Pattern personPattern = new Pattern(0, new ClassObjectType(Person.class), "$person");
        Pattern cheesePattern = new Pattern(0, new ClassObjectType(Cheese.class), "$cheese");
        
        boolean result = PatternHashComparator.arePatternStructuresEqual(personPattern, cheesePattern);
        
        assertThat(result)
            .as("Patterns with different object types should not be equal")
            .isFalse();
    }
    
    @Test
    public void testIdenticalPatternStructure() {
        // For variable name normalization testing, patterns with same structure but different variable names should be equal
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person");
        Pattern pattern2 = new Pattern(0, new ClassObjectType(Person.class), "$p");
        
        boolean result = PatternHashComparator.arePatternStructuresEqual(pattern1, pattern2);
        
        assertThat(result)
            .as("Patterns with same structure but different variable names should be equal")
            .isTrue();
    }
    
    @Test
    public void testPatternsWithIdenticalAlphaConstraints() {
        Pattern pattern1 = createPatternWithAlphaConstraint(Person.class, "age", 42, "$person");
        Pattern pattern2 = createPatternWithAlphaConstraint(Person.class, "age", 42, "$p");
        
        boolean result = PatternHashComparator.arePatternStructuresEqual(pattern1, pattern2);
        
        assertThat(result)
            .as("Patterns with identical alpha constraints should be equal despite different variable names")
            .isTrue();
    }
    
    @Test
    public void testPatternsWithDifferentAlphaConstraints() {
        Pattern pattern1 = createPatternWithAlphaConstraint(Person.class, "age", 42, "$person");
        Pattern pattern2 = createPatternWithAlphaConstraint(Person.class, "age", 30, "$person");
        
        boolean result = PatternHashComparator.arePatternStructuresEqual(pattern1, pattern2);
        
        assertThat(result)
            .as("Patterns with different alpha constraint values should not be equal")
            .isFalse();
    }
    
    @Test
    public void testPatternsWithDifferentFieldConstraints() {
        Pattern pattern1 = createPatternWithAlphaConstraint(Person.class, "age", 42, "$person");
        Pattern pattern2 = createPatternWithAlphaConstraint(Person.class, "name", "John", "$person");
        
        boolean result = PatternHashComparator.arePatternStructuresEqual(pattern1, pattern2);
        
        assertThat(result)
            .as("Patterns with constraints on different fields should not be equal")
            .isFalse();
    }
    
    
    @Test
    public void testNullPatterns() {
        Pattern pattern = new Pattern(0, new ClassObjectType(Person.class), "$person");
        
        assertThat(PatternHashComparator.arePatternStructuresEqual(null, null))
            .as("Two null patterns should be equal")
            .isTrue();
            
        assertThat(PatternHashComparator.arePatternStructuresEqual(pattern, null))
            .as("Pattern and null should not be equal")
            .isFalse();
            
        assertThat(PatternHashComparator.arePatternStructuresEqual(null, pattern))
            .as("Null and pattern should not be equal")
            .isFalse();
    }
    
    @Test
    public void testGenerateNormalizedHash() {
        Pattern pattern = createPatternWithAlphaConstraint(Person.class, "age", 42, "$person");
        
        String hash1 = PatternHashComparator.generateNormalizedHash(pattern);
        String hash2 = PatternHashComparator.generateNormalizedHash(pattern);
        
        assertThat(hash1)
            .as("Hash generation should be deterministic")
            .isEqualTo(hash2);
            
        assertThat(hash1)
            .as("Hash should contain object type")
            .contains("TYPE:org.drools.core.test.model.Person");
            
        assertThat(hash1)
            .as("Hash should contain index")
            .contains("IDX:0");
            
        assertThat(hash1)
            .as("Hash should contain constraints")
            .contains("CONSTRAINTS:");
    }
    
    @Test
    public void testComparePatterns() {
        Pattern pattern1 = createPatternWithAlphaConstraint(Person.class, "age", 42, "$person");
        Pattern pattern2 = createPatternWithAlphaConstraint(Person.class, "age", 42, "$p");
        
        PatternHashComparator.ComparisonResult result = 
            PatternHashComparator.comparePatterns(pattern1, pattern2);
        
        assertThat(result.isEqual())
            .as("Comparison result should indicate equality")
            .isTrue();
            
        assertThat(result.getDetails())
            .as("Details should contain hash information")
            .contains("Pattern 1 hash:")
            .contains("Pattern 2 hash:");
    }
    
    
    @Test
    public void testComplexPatternComparison() {
        // Create patterns with multiple constraints
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person1");
        pattern1.addConstraint(new MockAlphaConstraint("age", 42));
        pattern1.addConstraint(new MockAlphaConstraint("name", "John"));
        
        Pattern pattern2 = new Pattern(0, new ClassObjectType(Person.class), "$person2");
        pattern2.addConstraint(new MockAlphaConstraint("age", 42));
        pattern2.addConstraint(new MockAlphaConstraint("name", "John"));
        
        Pattern pattern3 = new Pattern(0, new ClassObjectType(Person.class), "$person3");
        pattern3.addConstraint(new MockAlphaConstraint("age", 30)); // Different value
        pattern3.addConstraint(new MockAlphaConstraint("name", "John"));
        
        assertThat(PatternHashComparator.arePatternStructuresEqual(pattern1, pattern2))
            .as("Patterns with identical multiple constraints should be equal")
            .isTrue();
            
        assertThat(PatternHashComparator.arePatternStructuresEqual(pattern1, pattern3))
            .as("Patterns with different constraint values should not be equal")
            .isFalse();
    }
    
    @Test
    public void testConstraintFieldNormalization() {
        // Test that field names are properly extracted and normalized
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person1");
        pattern1.addConstraint(new MockAlphaConstraint("age", 42));
        
        Pattern pattern2 = new Pattern(0, new ClassObjectType(Person.class), "$person2");
        pattern2.addConstraint(new MockAlphaConstraint("age", 42));
        
        Pattern pattern3 = new Pattern(0, new ClassObjectType(Person.class), "$person3");
        pattern3.addConstraint(new MockAlphaConstraint("salary", 42)); // Different field
        
        String hash1 = PatternHashComparator.generateNormalizedHash(pattern1);
        String hash2 = PatternHashComparator.generateNormalizedHash(pattern2);
        String hash3 = PatternHashComparator.generateNormalizedHash(pattern3);
        
        assertThat(hash1)
            .as("Same field constraints should produce identical hashes")
            .isEqualTo(hash2);
            
        assertThat(hash1)
            .as("Different field constraints should produce different hashes")
            .isNotEqualTo(hash3);
    }
    
    @Test
    public void testConstraintValueNormalization() {
        // Test that constraint values are properly captured
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person");
        pattern1.addConstraint(new MockAlphaConstraint("age", 42));
        
        Pattern pattern2 = new Pattern(0, new ClassObjectType(Person.class), "$person");
        pattern2.addConstraint(new MockAlphaConstraint("age", 35));
        
        String hash1 = PatternHashComparator.generateNormalizedHash(pattern1);
        String hash2 = PatternHashComparator.generateNormalizedHash(pattern2);
        
        assertThat(hash1)
            .as("Different constraint values should produce different hashes")
            .isNotEqualTo(hash2);
    }
    
    @Test
    public void testMixedConstraintTypes() {
        // Test patterns with both string and numeric constraints
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person1");
        pattern1.addConstraint(new MockAlphaConstraint("name", "John"));
        pattern1.addConstraint(new MockAlphaConstraint("age", 42));
        pattern1.addConstraint(new MockAlphaConstraint("active", true));
        
        Pattern pattern2 = new Pattern(0, new ClassObjectType(Person.class), "$person2");
        pattern2.addConstraint(new MockAlphaConstraint("name", "John"));
        pattern2.addConstraint(new MockAlphaConstraint("age", 42));
        pattern2.addConstraint(new MockAlphaConstraint("active", true));
        
        Pattern pattern3 = new Pattern(0, new ClassObjectType(Person.class), "$person3");
        pattern3.addConstraint(new MockAlphaConstraint("name", "Jane")); // Different value
        pattern3.addConstraint(new MockAlphaConstraint("age", 42));
        pattern3.addConstraint(new MockAlphaConstraint("active", true));
        
        assertThat(PatternHashComparator.arePatternStructuresEqual(pattern1, pattern2))
            .as("Patterns with identical mixed constraints should be equal")
            .isTrue();
            
        assertThat(PatternHashComparator.arePatternStructuresEqual(pattern1, pattern3))
            .as("Patterns with different string values should not be equal")
            .isFalse();
    }
    
    @Test
    public void testConstraintOrderIndependence() {
        // Test that constraint order doesn't affect equality
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person1");
        pattern1.addConstraint(new MockAlphaConstraint("age", 42));
        pattern1.addConstraint(new MockAlphaConstraint("name", "John"));
        
        Pattern pattern2 = new Pattern(0, new ClassObjectType(Person.class), "$person2");
        pattern2.addConstraint(new MockAlphaConstraint("name", "John"));
        pattern2.addConstraint(new MockAlphaConstraint("age", 42));
        
        String hash1 = PatternHashComparator.generateNormalizedHash(pattern1);
        String hash2 = PatternHashComparator.generateNormalizedHash(pattern2);
        
        assertThat(hash1)
            .as("Constraint order should not affect hash (constraints are sorted)")
            .isEqualTo(hash2);
    }
    
    @Test
    public void testConstraintDetailsInHash() {
        // Test that constraint details are visible in the hash
        Pattern pattern = new Pattern(0, new ClassObjectType(Person.class), "$person");
        pattern.addConstraint(new MockAlphaConstraint("age", 42));
        
        String hash = PatternHashComparator.generateNormalizedHash(pattern);
        
        assertThat(hash)
            .as("Hash should contain constraint information")
            .contains("CONSTRAINTS:")
            .contains("MockAlphaConstraint");
    }
    
    // Helper methods
    
    private Pattern createPatternWithAlphaConstraint(Class<?> objectType, String fieldName, 
                                                   Object value, String variableName) {
        Pattern pattern = new Pattern(0, new ClassObjectType(objectType), variableName);
        MockAlphaConstraint constraint = new MockAlphaConstraint(fieldName, value);
        pattern.addConstraint(constraint);
        return pattern;
    }
    
    
    /**
     * Mock alpha constraint for testing - extends from existing test
     */
    static class MockAlphaConstraint extends BiLinearDetectionTest.MockAlphaConstraint {
        public MockAlphaConstraint(String fieldName, Object value) {
            super(fieldName, value);
        }
    }
    
}
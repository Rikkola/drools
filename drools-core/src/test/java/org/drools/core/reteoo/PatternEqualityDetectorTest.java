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
import org.drools.base.rule.Pattern;
import org.drools.base.rule.constraint.Constraint;
import org.drools.core.reteoo.builder.PatternEqualityDetector;
import org.drools.core.test.model.Person;
import org.drools.core.test.model.Cheese;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for PatternEqualityDetector
 */
public class PatternEqualityDetectorTest {

    @Test
    public void testIdenticalConstraintFreePatterns() {
        Pattern pattern1 = new Pattern(0, new ClassObjectType(Person.class), "$person");
        Pattern pattern2 = new Pattern(0, new ClassObjectType(Person.class), "$p");
        
        assertThat(PatternEqualityDetector.arePatternsSharable(pattern1, pattern2))
            .as("Identical constraint-free patterns should be sharable")
            .isTrue();
            
        assertThat(PatternEqualityDetector.haveConflictingConstraints(pattern1, pattern2))
            .as("Identical constraint-free patterns should not have conflicts")
            .isFalse();
    }
    
    @Test
    public void testDifferentObjectTypes() {
        Pattern personPattern = new Pattern(0, new ClassObjectType(Person.class), "$person");
        Pattern cheesePattern = new Pattern(1, new ClassObjectType(Cheese.class), "$cheese");
        
        assertThat(PatternEqualityDetector.arePatternsSharable(personPattern, cheesePattern))
            .as("Different object types should not be sharable")
            .isFalse();
            
        assertThat(PatternEqualityDetector.haveConflictingConstraints(personPattern, cheesePattern))
            .as("Different object types should be considered conflicting")
            .isTrue();
    }
    
    @Test
    public void testIdenticalAlphaConstraints() {
        Pattern pattern1 = createPatternWithConstraint(Person.class, "age", 42);
        Pattern pattern2 = createPatternWithConstraint(Person.class, "age", 42);
        
        assertThat(PatternEqualityDetector.arePatternsSharable(pattern1, pattern2))
            .as("Patterns with identical alpha constraints should be sharable")
            .isTrue();
            
        assertThat(PatternEqualityDetector.haveConflictingConstraints(pattern1, pattern2))
            .as("Patterns with identical alpha constraints should not have conflicts")
            .isFalse();
    }
    
    @Test
    public void testDifferentAlphaConstraintValues() {
        Pattern pattern1 = createPatternWithConstraint(Person.class, "age", 42);
        Pattern pattern2 = createPatternWithConstraint(Person.class, "age", 10);
        
        assertThat(PatternEqualityDetector.arePatternsSharable(pattern1, pattern2))
            .as("Patterns with different alpha constraint values should not be sharable")
            .isFalse();
            
        assertThat(PatternEqualityDetector.haveConflictingConstraints(pattern1, pattern2))
            .as("Patterns with different alpha constraint values should be conflicting")
            .isTrue();
    }
    
    @Test
    public void testDifferentAlphaConstraintFields() {
        Pattern pattern1 = createPatternWithConstraint(Person.class, "age", 42);
        Pattern pattern2 = createPatternWithConstraint(Person.class, "name", "John");
        
        assertThat(PatternEqualityDetector.arePatternsSharable(pattern1, pattern2))
            .as("Patterns with different constraint fields should not be sharable")
            .isFalse();
            
        assertThat(PatternEqualityDetector.haveConflictingConstraints(pattern1, pattern2))
            .as("Patterns with different constraint fields should be conflicting")
            .isTrue();
    }
    
    @Test
    public void testSequentialCompatibility() {
        Pattern personPattern = new Pattern(0, new ClassObjectType(Person.class), "$person");
        Pattern cheesePattern = new Pattern(1, new ClassObjectType(Cheese.class), "$cheese");
        
        assertThat(PatternEqualityDetector.areSequentialPatternsCompatible(personPattern, cheesePattern))
            .as("Different object types should be compatible for sequential joins")
            .isTrue();
    }
    
    @Test
    public void testSequentialSameObjectTypes() {
        Pattern person1 = new Pattern(0, new ClassObjectType(Person.class), "$person1");
        Pattern person2 = new Pattern(1, new ClassObjectType(Person.class), "$person2");
        
        assertThat(PatternEqualityDetector.areSequentialPatternsCompatible(person1, person2))
            .as("Same object types should not be compatible for sequential joins")
            .isFalse();
    }
    
    @Test
    public void testConstraintListIdentity() {
        MockAlphaConstraint constraint1 = new MockAlphaConstraint("age", 42);
        MockAlphaConstraint constraint2 = new MockAlphaConstraint("age", 42);
        MockAlphaConstraint constraint3 = new MockAlphaConstraint("age", 10);
        
        List<Constraint> list1 = Arrays.asList(constraint1);
        List<Constraint> list2 = Arrays.asList(constraint2);
        List<Constraint> list3 = Arrays.asList(constraint3);
        
        assertThat(PatternEqualityDetector.areConstraintListsIdentical(list1, list2))
            .as("Lists with identical constraints should be identical")
            .isTrue();
            
        assertThat(PatternEqualityDetector.areConstraintListsIdentical(list1, list3))
            .as("Lists with different constraint values should not be identical")
            .isFalse();
    }
    
    @Test
    public void testEmptyConstraintLists() {
        List<Constraint> empty1 = Collections.emptyList();
        List<Constraint> empty2 = Collections.emptyList();
        
        assertThat(PatternEqualityDetector.areConstraintListsIdentical(empty1, empty2))
            .as("Empty constraint lists should be identical")
            .isTrue();
    }
    
    @Test
    public void testConstraintHashConsistency() {
        MockAlphaConstraint constraint1 = new MockAlphaConstraint("age", 42);
        MockAlphaConstraint constraint2 = new MockAlphaConstraint("age", 42);
        MockAlphaConstraint constraint3 = new MockAlphaConstraint("name", "John");
        
        int hash1 = PatternEqualityDetector.calculateConstraintHash(constraint1);
        int hash2 = PatternEqualityDetector.calculateConstraintHash(constraint2);
        int hash3 = PatternEqualityDetector.calculateConstraintHash(constraint3);
        
        assertThat(hash1)
            .as("Identical constraints should have same hash")
            .isEqualTo(hash2);
            
        assertThat(hash1)
            .as("Different constraints should have different hash")
            .isNotEqualTo(hash3);
    }
    
    @Test
    public void testPatternSignatureGeneration() {
        Pattern constraintFree = new Pattern(0, new ClassObjectType(Person.class), "$person");
        Pattern withConstraint = createPatternWithConstraint(Person.class, "age", 42);
        
        String sig1 = PatternEqualityDetector.createPatternSignature(constraintFree);
        String sig2 = PatternEqualityDetector.createPatternSignature(withConstraint);
        
        assertThat(sig1)
            .as("Constraint-free pattern should have simple signature")
            .isEqualTo("org.drools.core.test.model.Person");
            
        assertThat(sig2)
            .as("Pattern with constraint should have signature including constraint hash")
            .startsWith("org.drools.core.test.model.Person[")
            .endsWith("]");
            
        assertThat(sig1)
            .as("Signatures should be different")
            .isNotEqualTo(sig2);
    }
    
    @Test
    public void testJoinSignatureGeneration() {
        Pattern personPattern = createPatternWithConstraint(Person.class, "age", 42);
        Pattern cheesePattern = createPatternWithConstraint(Cheese.class, "type", "stilton");
        
        String joinSig = PatternEqualityDetector.createJoinSignature(
            personPattern, cheesePattern, Collections.emptyList()
        );
        
        assertThat(joinSig)
            .as("Join signature should contain both pattern signatures")
            .contains("org.drools.core.test.model.Person")
            .contains("org.drools.core.test.model.Cheese")
            .contains("::");
    }
    
    // Helper methods
    
    private Pattern createPatternWithConstraint(Class<?> objectType, String fieldName, Object value) {
        Pattern pattern = new Pattern(0, new ClassObjectType(objectType), "$obj");
        MockAlphaConstraint constraint = new MockAlphaConstraint(fieldName, value);
        pattern.addConstraint(constraint);
        return pattern;
    }
    
    /**
     * Mock implementation of AlphaNodeFieldConstraint for testing
     */
    static class MockAlphaConstraint extends BiLinearDetectionTest.MockAlphaConstraint {
        public MockAlphaConstraint(String fieldName, Object value) {
            super(fieldName, value);
        }
    }
}
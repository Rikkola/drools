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

import org.drools.base.rule.Pattern;
import org.drools.base.rule.constraint.Constraint;
import org.drools.base.rule.constraint.BetaConstraint;

import java.util.List;
import java.util.Objects;

/**
 * Specialized class for detecting pattern equality for BiLinear optimization.
 * 
 * Handles comparison of patterns based on:
 * - Object types
 * - Alpha constraints (field-level constraints)
 * - Beta constraints (cross-pattern constraints)
 * - Constraint value equality
 */
public class PatternEqualityDetector {

    /**
     * Determines if two patterns can be considered identical for sharing purposes.
     * 
     * @param leftPattern First pattern to compare
     * @param rightPattern Second pattern to compare
     * @return true if patterns can be shared, false otherwise
     */
    public static boolean arePatternsSharable(Pattern leftPattern, Pattern rightPattern) {
        // Patterns must have the same object type
        if (!leftPattern.getObjectType().equals(rightPattern.getObjectType())) {
            return false;
        }
        
        // Must have the same number of constraints
        if (leftPattern.getConstraints().size() != rightPattern.getConstraints().size()) {
            return false;
        }
        
        // Both empty constraints - identical
        if (leftPattern.getConstraints().isEmpty() && rightPattern.getConstraints().isEmpty()) {
            return true;
        }
        
        // Compare constraint content for identity
        return areConstraintListsIdentical(leftPattern.getConstraints(), rightPattern.getConstraints());
    }
    
    /**
     * Determines if two patterns have conflicting constraint values that would prevent sharing.
     * 
     * @param leftPattern First pattern to compare  
     * @param rightPattern Second pattern to compare
     * @return true if patterns have conflicting values, false otherwise
     */
    public static boolean haveConflictingConstraints(Pattern leftPattern, Pattern rightPattern) {
        // Different object types always conflict
        if (!leftPattern.getObjectType().equals(rightPattern.getObjectType())) {
            return true;
        }
        
        // If either has no constraints, no conflict possible
        if (leftPattern.getConstraints().isEmpty() || rightPattern.getConstraints().isEmpty()) {
            return false;
        }
        
        // Different constraint counts suggest structural differences
        if (leftPattern.getConstraints().size() != rightPattern.getConstraints().size()) {
            return true;
        }
        
        // If we can't prove constraints are identical, assume potential conflicts
        return !areConstraintListsIdentical(leftPattern.getConstraints(), rightPattern.getConstraints());
    }
    
    /**
     * Determines if two patterns can be shared in a sequential join context.
     * 
     * This is more permissive than full equality - allows sharing when patterns
     * represent compatible sequential operations.
     * 
     * @param leftPattern First pattern in join sequence
     * @param rightPattern Second pattern in join sequence  
     * @return true if patterns can be shared in sequential context
     */
    public static boolean areSequentialPatternsCompatible(Pattern leftPattern, Pattern rightPattern) {
        // Must be different object types for meaningful join
        if (leftPattern.getObjectType().equals(rightPattern.getObjectType())) {
            return false;
        }
        
        // Case 1: Both patterns have no constraints - always compatible
        if (leftPattern.getConstraints().isEmpty() && rightPattern.getConstraints().isEmpty()) {
            return true;
        }
        
        // Case 2: Check if each pattern individually is compatible with corresponding patterns in other rules
        // This would require access to the corresponding patterns from other rules
        // For now, we'll use a conservative approach
        
        return !haveConflictingConstraints(leftPattern, rightPattern);
    }
    
    /**
     * Compares two constraint lists for semantic identity using hash-based comparison.
     * 
     * @param constraints1 First constraint list
     * @param constraints2 Second constraint list  
     * @return true if constraint lists are semantically identical
     */
    public static boolean areConstraintListsIdentical(List<Constraint> constraints1, List<Constraint> constraints2) {
        // Basic size check
        if (constraints1.size() != constraints2.size()) {
            return false;
        }
        
        // Both empty
        if (constraints1.isEmpty() && constraints2.isEmpty()) {
            return true;
        }
        
        // Hash-based comparison for semantic identity
        int[] hashes1 = constraints1.stream()
                .mapToInt(PatternEqualityDetector::calculateConstraintHash)
                .sorted()
                .toArray();
                
        int[] hashes2 = constraints2.stream()
                .mapToInt(PatternEqualityDetector::calculateConstraintHash)
                .sorted()
                .toArray();
        
        return java.util.Arrays.equals(hashes1, hashes2);
    }
    
    /**
     * Calculates a normalized hash for a constraint that preserves semantic identity
     * while abstracting away variable names.
     * 
     * @param constraint The constraint to hash
     * @return Normalized hash value
     */
    public static int calculateConstraintHash(Constraint constraint) {
        try {
            // Use the constraint's built-in hashCode as foundation
            // Most constraint implementations include field names, operators, and values
            int baseHash = constraint.hashCode();
            
            // Add constraint type information to distinguish different implementations
            int typeHash = constraint.getClass().getSimpleName().hashCode();
            
            // Combine for normalized signature
            return Objects.hash(typeHash, baseHash);
            
        } catch (Exception e) {
            // Fallback: use class-based hash if constraint hash fails
            return constraint.getClass().hashCode();
        }
    }
    
    /**
     * Creates a normalized signature for a pattern that includes both object type
     * and alpha constraint information.
     * 
     * @param pattern The pattern to create signature for
     * @return Normalized signature string
     */
    public static String createPatternSignature(Pattern pattern) {
        StringBuilder sig = new StringBuilder();
        
        // Add object type
        sig.append(pattern.getObjectType().getClassName());
        
        // Add constraint signatures sorted for consistency
        if (!pattern.getConstraints().isEmpty()) {
            sig.append("[");
            
            int[] constraintHashes = pattern.getConstraints().stream()
                    .filter(c -> !(c instanceof BetaConstraint)) // Only alpha constraints for individual pattern signature
                    .mapToInt(PatternEqualityDetector::calculateConstraintHash)
                    .sorted()
                    .toArray();
            
            for (int i = 0; i < constraintHashes.length; i++) {
                if (i > 0) sig.append(",");
                sig.append(constraintHashes[i]);
            }
            
            sig.append("]");
        }
        
        return sig.toString();
    }
    
    /**
     * Creates a normalized signature for a join pattern between two individual patterns.
     * 
     * @param leftPattern Left side of join
     * @param rightPattern Right side of join
     * @param betaConstraints Cross-pattern constraints
     * @return Normalized join signature
     */
    public static String createJoinSignature(Pattern leftPattern, Pattern rightPattern, List<BetaConstraint> betaConstraints) {
        StringBuilder sig = new StringBuilder();
        
        // Add individual pattern signatures
        sig.append(createPatternSignature(leftPattern))
           .append("::")
           .append(createPatternSignature(rightPattern));
        
        // Add beta constraint signatures
        if (!betaConstraints.isEmpty()) {
            sig.append("::");
            
            int[] betaHashes = betaConstraints.stream()
                    .mapToInt(PatternEqualityDetector::calculateConstraintHash)
                    .sorted()
                    .toArray();
            
            for (int i = 0; i < betaHashes.length; i++) {
                if (i > 0) sig.append(",");
                sig.append(betaHashes[i]);
            }
        }
        
        return sig.toString();
    }
}
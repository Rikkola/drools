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

import java.util.List;
import java.util.Objects;

import org.drools.base.base.ObjectType;
import org.drools.base.rule.constraint.BetaConstraint;
import org.drools.core.common.BetaConstraints;

/**
 * Enhanced key for identifying shared BiLinearJoinNode patterns.
 * 
 * This class provides a more sophisticated signature generation than the
 * simple string-based approach, including detailed constraint analysis,
 * object type compatibility, and hash-based equality for efficient lookup.
 */
public class SharedNodeKey {
    
    private final ObjectType leftObjectType;
    private final ObjectType rightObjectType;
    private final String constraintSignature;
    private final int hashCode;
    
    public SharedNodeKey(ObjectType leftObjectType, 
                        ObjectType rightObjectType, 
                        BetaConstraints betaConstraints) {
        this.leftObjectType = leftObjectType;
        this.rightObjectType = rightObjectType;
        this.constraintSignature = createConstraintSignature(betaConstraints);
        this.hashCode = computeHashCode();
    }
    
    public SharedNodeKey(ObjectType leftObjectType, 
                        ObjectType rightObjectType, 
                        List<BetaConstraint> constraints) {
        this.leftObjectType = leftObjectType;
        this.rightObjectType = rightObjectType;
        this.constraintSignature = createConstraintSignature(constraints);
        this.hashCode = computeHashCode();
    }
    
    /**
     * Creates a detailed signature from BetaConstraints that captures the
     * semantic meaning of constraints for accurate sharing detection.
     */
    private String createConstraintSignature(BetaConstraints betaConstraints) {
        if (betaConstraints == null) {
            return "EMPTY";
        }
        
        StringBuilder sig = new StringBuilder();
        
        // Include constraint type and properties
        sig.append(betaConstraints.getClass().getSimpleName()).append(":");
        
        // Add constraint details if available
        // This is a simplified approach - could be enhanced with more detailed analysis
        sig.append(betaConstraints.toString());
        
        return sig.toString();
    }
    
    /**
     * Creates a detailed signature from a list of BetaConstraints.
     */
    private String createConstraintSignature(List<BetaConstraint> constraints) {
        if (constraints == null || constraints.isEmpty()) {
            return "EMPTY";
        }
        
        StringBuilder sig = new StringBuilder();
        
        for (BetaConstraint constraint : constraints) {
            if (sig.length() > 0) {
                sig.append(";");
            }
            
            // Include constraint class and string representation
            sig.append(constraint.getClass().getSimpleName())
               .append(":")
               .append(constraint.toString());
        }
        
        return sig.toString();
    }
    
    /**
     * Generates a string signature for this key, compatible with the
     * existing SharedJoinPattern signature format.
     */
    public String toSignature() {
        StringBuilder sig = new StringBuilder();
        sig.append(leftObjectType.getClassName())
           .append("::")
           .append(rightObjectType.getClassName())
           .append("::")
           .append(constraintSignature);
        return sig.toString();
    }
    
    /**
     * Checks if this key represents a pattern that is compatible for sharing
     * with another key. This allows for more sophisticated sharing rules
     * beyond exact equality.
     */
    public boolean isCompatibleWith(SharedNodeKey other) {
        // For now, use exact equality
        // Could be enhanced to allow compatible but not identical patterns
        return equals(other);
    }
    
    /**
     * Gets the estimated sharing benefit of using this pattern.
     * Higher values indicate patterns that are more beneficial to share.
     */
    public double getSharingBenefit() {
        double benefit = 1.0; // Base benefit
        
        // Increase benefit for complex constraints (more expensive to duplicate)
        if (constraintSignature.length() > 50) {
            benefit += 0.5;
        }
        
        // Increase benefit for complex object types
        if (leftObjectType.getClassName().contains(".") || rightObjectType.getClassName().contains(".")) {
            benefit += 0.3;
        }
        
        return benefit;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SharedNodeKey that = (SharedNodeKey) obj;
        return Objects.equals(leftObjectType.getClassName(), that.leftObjectType.getClassName()) &&
               Objects.equals(rightObjectType.getClassName(), that.rightObjectType.getClassName()) &&
               Objects.equals(constraintSignature, that.constraintSignature);
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    private int computeHashCode() {
        return Objects.hash(leftObjectType.getClassName(), 
                          rightObjectType.getClassName(), 
                          constraintSignature);
    }
    
    @Override
    public String toString() {
        return "SharedNodeKey{" +
               "left=" + leftObjectType.getClassName() +
               ", right=" + rightObjectType.getClassName() +
               ", constraints='" + constraintSignature + '\'' +
               '}';
    }
    
    // Getters
    public ObjectType getLeftObjectType() { return leftObjectType; }
    public ObjectType getRightObjectType() { return rightObjectType; }
    public String getConstraintSignature() { return constraintSignature; }
}
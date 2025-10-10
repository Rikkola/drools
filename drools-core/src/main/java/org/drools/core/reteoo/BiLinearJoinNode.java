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

import org.drools.base.reteoo.NodeTypeEnums;
import org.drools.base.rule.Declaration;
import org.drools.core.common.BetaConstraints;
import org.drools.core.common.BiLinearBetaConstraints;
import org.drools.core.common.EmptyBetaConstraints;
import org.drools.core.common.MemoryFactory;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.util.FastIterator;

import java.util.HashMap;
import java.util.Map;

public class BiLinearJoinNode extends JoinNode {

    private static final long serialVersionUID = 510l;

    // Second left input for the bi-linear join - different from normal BetaNode
    protected LeftTupleSource secondLeftInput;
    
    // Cross-network declaration context for variable resolution
    protected BiLinearDeclarationContext declarationContext;
    
    // Enhanced constraints for cross-network variable resolution
    protected BiLinearBetaConstraints biLinearConstraints;

    public BiLinearJoinNode() {
    }

    public BiLinearJoinNode(final int id,
                           final LeftTupleSource leftInput,
                           final LeftTupleSource secondLeftInput,
                           final RightInputAdapterNode rightInput,
                           final BetaConstraints constraints,
                           final BuildContext context) {
        super(id, leftInput, rightInput, createBiLinearConstraints(constraints, leftInput, secondLeftInput), context);
        
        this.secondLeftInput = secondLeftInput;
        
        // Object count accounts for both left inputs
        this.setObjectCount(leftInput.getObjectCount() + secondLeftInput.getObjectCount());
        
        // Initialize BiLinear features after super() call
        // The constraints should now be BiLinearBetaConstraints due to createBiLinearConstraints()
        initializeBiLinearFeatures(this.getRawConstraints());
    }

    // Simple constructor for manual testing - uses EmptyBetaConstraints
    public BiLinearJoinNode(final int id,
                           final LeftTupleSource leftInput,
                           final LeftTupleSource secondLeftInput,
                           final RightInputAdapterNode rightInput,
                           final BuildContext context) {
        this(id, leftInput, secondLeftInput, rightInput, new EmptyBetaConstraints(), context);
    }
    
    /**
     * Enhanced constructor with explicit declaration context for cross-network variable resolution
     */
    public BiLinearJoinNode(final int id,
                           final LeftTupleSource leftInput,
                           final LeftTupleSource secondLeftInput,
                           final RightInputAdapterNode rightInput,
                           final BetaConstraints constraints,
                           final BuildContext context,
                           final BiLinearDeclarationContext declarationContext) {
        // Create BiLinearBetaConstraints with the explicit declaration context
        super(id, leftInput, rightInput, new BiLinearBetaConstraints(constraints, declarationContext), context);
        this.secondLeftInput = secondLeftInput;
        this.declarationContext = declarationContext;
        
        // Object count accounts for both left inputs
        this.setObjectCount(leftInput.getObjectCount() + secondLeftInput.getObjectCount());
        
        // Initialize BiLinear features with the explicit context
        initializeBiLinearFeatures(this.getRawConstraints());
    }
    
    /**
     * BiLinear nodes must register as sinks on both inputs to ensure proper RETE connectivity.
     * The duplicate prevention in CompositeLeftTupleSinkAdapter handles potential conflicts.
     */
    @Override
    public void doAttach(BuildContext context) {
        super.doAttach(context); // Registers on first left input
        
        // Register on second left input if it's different from first input
        // This is essential for mock sources to have proper sink connectivity
        if (secondLeftInput != null && !secondLeftInput.equals(getLeftTupleSource())) {
            secondLeftInput.addTupleSink(this, context);
        }
    }
    
    /**
     * Creates BiLinearBetaConstraints for the constructor to pass to super()
     */
    private static BetaConstraints createBiLinearConstraints(BetaConstraints originalConstraints,
                                                           LeftTupleSource leftInput,
                                                           LeftTupleSource secondLeftInput) {
        // Calculate offset for second network (first network size)
        int secondNetworkOffset = leftInput != null ? leftInput.getObjectCount() : 0;
        
        // Create declaration context with both network sources and offset
        BiLinearDeclarationContext declarationContext = new BiLinearDeclarationContext(
            leftInput,
            secondLeftInput,
            secondNetworkOffset
        );
        
        // Always wrap constraints for cross-network variable resolution
        // Even EmptyBetaConstraints need to be wrapped to support cross-network features
        BiLinearBetaConstraints result = new BiLinearBetaConstraints(originalConstraints, declarationContext);
        return result;
    }
    
    /**
     * Initializes BiLinear-specific features including cross-network variable resolution
     */
    private void initializeBiLinearFeatures(BetaConstraints constraints) {
        if (constraints instanceof BiLinearBetaConstraints) {
            biLinearConstraints = (BiLinearBetaConstraints) constraints;
            declarationContext = biLinearConstraints.getDeclarationContext();
        }
    }
    
    
    /**
     * Creates a declaration context from the two input networks
     */
    private BiLinearDeclarationContext createDeclarationContext(LeftTupleSource leftInput, 
                                                              LeftTupleSource secondLeftInput) {
        // Calculate offset for second network (first network size)
        int secondNetworkOffset = leftInput != null ? leftInput.getObjectCount() : 0;
        
        // Create context with both network sources and offset
        return new BiLinearDeclarationContext(
            leftInput,
            secondLeftInput,
            secondNetworkOffset
        );
    }
    
    /**
     * Creates a declaration context from the two input networks (backward compatibility)
     */
    private BiLinearDeclarationContext createDeclarationContext() {
        return createDeclarationContext(getLeftTupleSource(), secondLeftInput);
    }

    public LeftTupleSource getSecondLeftInput() {
        return secondLeftInput;
    }

    public void setSecondLeftInput(LeftTupleSource secondLeftInput) {
        this.secondLeftInput = secondLeftInput;
        
        // Reinitialize declaration context when second input changes
        this.declarationContext = createDeclarationContext();
        if (biLinearConstraints != null) {
            initializeBiLinearFeatures(biLinearConstraints.getWrappedConstraints());
        }
    }
    
    /**
     * Gets the cross-network declaration context
     */
    public BiLinearDeclarationContext getDeclarationContext() {
        return declarationContext;
    }
    
    /**
     * Sets the cross-network declaration context
     */
    public void setDeclarationContext(BiLinearDeclarationContext declarationContext) {
        this.declarationContext = declarationContext;
        
        // Update constraints with new declaration context
        if (biLinearConstraints != null) {
            initializeBiLinearFeatures(biLinearConstraints.getWrappedConstraints());
        }
    }
    
    /**
     * Gets the enhanced BiLinear constraints
     */
    public BiLinearBetaConstraints getBiLinearConstraints() {
        return biLinearConstraints;
    }
    
    /**
     * Gets the raw constraints (may be wrapped BiLinear constraints)
     */
    @Override
    public BetaConstraints getRawConstraints() {
        // Return BiLinear constraints if available, otherwise fallback to standard constraints
        return biLinearConstraints != null ? biLinearConstraints : super.getRawConstraints();
    }
    
    /**
     * Checks if this node has cross-network variable resolution capabilities
     */
    public boolean hasCrossNetworkVariableResolution() {
        return declarationContext != null && biLinearConstraints != null;
    }
    
    /**
     * Creates a standard LeftTuple from two network tuples instead of BiLinearTuple.
     * This prevents ClassCastException at terminal nodes by ensuring compatibility
     * with downstream processing that expects standard LeftTuple instances.
     */
    public TupleImpl createBiLinearTuple(TupleImpl firstNetworkTuple, 
                                       TupleImpl secondNetworkTuple, 
                                       LeftTupleSink sink) {
        // Create a standard LeftTuple using TupleFactory to ensure downstream compatibility
        return TupleFactory.createLeftTuple(firstNetworkTuple, secondNetworkTuple, sink);
    }

    @Override
    public int getType() {
        return NodeTypeEnums.BiLinearJoinNode;
    }

    /**
     * Override getRightIterator to properly handle second left input tuples stored in right memory
     */
    @Override
    public FastIterator<TupleImpl> getRightIterator(TupleMemory memory) {
        // For BiLinear, "right" memory actually contains left tuples from second network
        return memory.fastIterator();
    }

    /**
     * Override getFirstRightTuple to properly handle second left input tuples
     * Note: Returns TupleImpl cast to RightTuple for compatibility
     */
    @Override
    public RightTuple getFirstRightTuple(final TupleImpl leftTuple,
                                        final TupleMemory memory,
                                        final FastIterator<TupleImpl> it) {
        // For BiLinear, return first tuple from second left input memory
        // This is actually a LeftTuple but we cast it for compatibility
        return (RightTuple) memory.getFirst(leftTuple);
    }

    /**
     * BiLinear-specific method to get first tuple from second network as TupleImpl
     * This provides cleaner semantics than treating second network tuples as "right" tuples
     */
    public TupleImpl getFirstSecondNetworkTuple(final TupleImpl leftTuple,
                                               final TupleMemory memory,
                                               final FastIterator<TupleImpl> it) {
        // Return first tuple from second network memory
        return memory.getFirst(leftTuple);
    }

    /**
     * BiLinear-specific iterator for second network tuples
     */
    public FastIterator<TupleImpl> getSecondNetworkIterator(TupleMemory memory) {
        // Return iterator for second network tuples
        return memory.fastIterator();
    }

    /**
     * Create context for BiLinear constraints
     * BiLinearJoinNode always creates BiLinearContextEntry for proper constraint handling
     */
    public Object createContext() {
        // BiLinearJoinNode always uses BiLinearContextEntry, even for simple cases
        return new BiLinearContextEntry(declarationContext != null ? declarationContext : createDeclarationContext());
    }

    /**
     * Override memory creation to ensure BiLinear nodes always create BiLinearMemory
     * This prevents ClassCastException between BetaMemory and PathMemory
     */
    @Override
    public org.drools.core.common.Memory createMemory(org.drools.core.RuleBaseConfiguration config, org.drools.core.common.ReteEvaluator reteEvaluator) {
        // Create BiLinearMemory specifically for BiLinear nodes
        BiLinearBetaConstraints biLinearConstraints = getBiLinearConstraints();
        BiLinearMemory memory;
        
        if (biLinearConstraints != null) {
            // Create memory using constraints - this should return BiLinearMemory now
            BetaMemory baseBetaMemory = biLinearConstraints.createBetaMemory(config, getType());
            
            if (baseBetaMemory instanceof BiLinearMemory) {
                memory = (BiLinearMemory) baseBetaMemory;
            } else {
                memory = new BiLinearMemory(
                    baseBetaMemory.getLeftTupleMemory(),
                    baseBetaMemory.getRightTupleMemory(),
                    baseBetaMemory.getContext(),
                    getType()
                );
            }
        } else {
            // Create memory with default configuration
            memory = new BiLinearMemory();
        }
        return memory;
    }

    /**
     * Override equals to include secondLeftInput comparison for proper node sharing
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        // BiLinearJoinNode should only equal other BiLinearJoinNodes
        if (!(object instanceof BiLinearJoinNode) || this.hashCode() != object.hashCode()) {
            return false;
        }
        
        BiLinearJoinNode other = (BiLinearJoinNode) object;
        
        // Compare all the standard BetaNode fields plus secondLeftInput
        // Note: Can't access leftListenedProperties directly (it's private), 
        // but the hashCode comparison above should catch differences
        return this.getClass() == other.getClass() &&
               this.constraints.equals(other.constraints) &&
               this.rightInput.equals(other.rightInput) &&
               this.leftInput.getId() == other.leftInput.getId() &&
               this.secondLeftInput.getId() == other.secondLeftInput.getId();
    }

    @Override
    public String toString() {
        return "[BiLinearJoinNode(" + this.getId() + ") - " + 
               "FirstInput: " + (getLeftTupleSource() != null ? getLeftTupleSource().getId() : "null") + 
               ", SecondInput: " + (secondLeftInput != null ? secondLeftInput.getId() : "null") + 
               ", HasCrossNetworkVars: " + hasCrossNetworkVariableResolution() + "]";
    }
}
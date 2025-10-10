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

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.BaseTuple;
import org.drools.base.rule.ContextEntry;
import org.drools.base.rule.Declaration;
import org.kie.api.runtime.rule.FactHandle;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * BiLinearContextEntry provides enhanced context management for BiLinearJoinNode
 * constraint evaluation. It maintains variable context from both input networks
 * and enables cross-network variable resolution during constraint evaluation.
 * 
 * This specialized context entry is essential for enabling complex join conditions
 * that reference variables from both input networks in a BiLinearJoinNode.
 */
public class BiLinearContextEntry implements ContextEntry {
    
    /** Next context entry in the chain */
    protected ContextEntry next;
    
    /** Tuple from the first network (primary left input) */
    protected BaseTuple firstNetworkTuple;
    
    /** Tuple from the second network (secondary left input) */
    protected BaseTuple secondNetworkTuple;
    
    /** Right fact handle for constraint evaluation */
    protected FactHandle rightHandle;
    
    /** Declaration context for cross-network variable resolution */
    protected BiLinearDeclarationContext declarationContext;
    
    /** Value resolver for runtime evaluation */
    protected transient ValueResolver valueResolver;
    
    /** Combined tuple representing both networks */
    protected BiLinearTuple combinedTuple;
    
    /**
     * Default constructor for serialization
     */
    public BiLinearContextEntry() {
    }
    
    /**
     * Creates a BiLinearContextEntry with declaration context
     * 
     * @param declarationContext The cross-network declaration context
     */
    public BiLinearContextEntry(BiLinearDeclarationContext declarationContext) {
        this.declarationContext = declarationContext;
    }
    
    @Override
    public ContextEntry getNext() {
        return next;
    }
    
    @Override
    public void setNext(ContextEntry entry) {
        this.next = entry;
    }
    
    /**
     * Updates context from a BiLinearTuple containing both network tuples
     */
    @Override
    public void updateFromTuple(ValueResolver valueResolver, BaseTuple tuple) {
        this.valueResolver = valueResolver;
        
        if (tuple instanceof BiLinearTuple) {
            BiLinearTuple biLinearTuple = (BiLinearTuple) tuple;
            this.firstNetworkTuple = biLinearTuple.getFirstNetworkTuple();
            this.secondNetworkTuple = biLinearTuple.getSecondNetworkTuple();
            this.combinedTuple = biLinearTuple;
        } else {
            // Fallback for regular tuples - treat as first network
            this.firstNetworkTuple = tuple;
            this.secondNetworkTuple = null;
            this.combinedTuple = null;
        }
    }
    
    /**
     * Updates context from separate network tuples
     * This method is specific to BiLinearJoinNode processing
     * 
     * @param valueResolver The value resolver
     * @param firstNetworkTuple Tuple from first network
     * @param secondNetworkTuple Tuple from second network
     */
    public void updateFromBiLinearTuples(ValueResolver valueResolver, 
                                       BaseTuple firstNetworkTuple, 
                                       BaseTuple secondNetworkTuple) {
        this.valueResolver = valueResolver;
        this.firstNetworkTuple = firstNetworkTuple;
        this.secondNetworkTuple = secondNetworkTuple;
        
        // Create combined tuple for cross-network access
        if (firstNetworkTuple instanceof TupleImpl && secondNetworkTuple instanceof TupleImpl) {
            this.combinedTuple = new BiLinearTuple(
                (TupleImpl) firstNetworkTuple,
                (TupleImpl) secondNetworkTuple,
                null, // No right fact handle for this context
                null  // No sink needed for context tuple
            );
        }
    }
    
    @Override
    public void updateFromFactHandle(ValueResolver valueResolver, FactHandle handle) {
        this.valueResolver = valueResolver;
        this.rightHandle = handle;
    }
    
    @Override
    public void resetTuple() {
        firstNetworkTuple = null;
        secondNetworkTuple = null;
        combinedTuple = null;
    }
    
    @Override
    public void resetFactHandle() {
        valueResolver = null;
        rightHandle = null;
    }
    
    /**
     * Gets the value resolver
     */
    public ValueResolver getValueResolver() {
        return valueResolver;
    }
    
    /**
     * Gets the first network tuple
     */
    public BaseTuple getFirstNetworkTuple() {
        return firstNetworkTuple;
    }
    
    /**
     * Gets the second network tuple
     */
    public BaseTuple getSecondNetworkTuple() {
        return secondNetworkTuple;
    }
    
    /**
     * Gets the right fact handle
     */
    public FactHandle getRightHandle() {
        return rightHandle;
    }
    
    /**
     * Gets the combined tuple for cross-network access
     */
    public BiLinearTuple getCombinedTuple() {
        return combinedTuple;
    }
    
    /**
     * Gets the declaration context
     */
    public BiLinearDeclarationContext getDeclarationContext() {
        return declarationContext;
    }
    
    /**
     * Resolves a variable value across both networks
     * 
     * @param declaration The variable declaration to resolve
     * @return The variable value, or null if not found
     */
    public Object resolveVariable(Declaration declaration) {
        if (declaration == null) {
            return null;
        }
        
        // Try combined tuple first (preferred approach)
        if (combinedTuple != null) {
            try {
                return combinedTuple.getObject(declaration);
            } catch (Exception e) {
                // Fall back to individual network resolution
            }
        }
        
        // Determine which network contains this declaration
        if (declarationContext != null) {
            int network = declarationContext.getDeclarationNetwork(declaration.getIdentifier());
            
            if (network == 1 && firstNetworkTuple != null) {
                return firstNetworkTuple.getObject(declaration);
            } else if (network == 2 && secondNetworkTuple != null) {
                return secondNetworkTuple.getObject(declaration);
            }
        }
        
        // Fallback: try both networks
        if (firstNetworkTuple != null) {
            try {
                return firstNetworkTuple.getObject(declaration);
            } catch (Exception e) {
                // Not in first network, try second
            }
        }
        
        if (secondNetworkTuple != null) {
            try {
                return secondNetworkTuple.getObject(declaration);
            } catch (Exception e) {
                // Not in second network either
            }
        }
        
        return null;
    }
    
    /**
     * Checks if a declaration is available in either network
     * 
     * @param declaration The declaration to check
     * @return true if the declaration is available
     */
    public boolean hasDeclaration(Declaration declaration) {
        if (declarationContext != null) {
            return declarationContext.hasDeclaration(declaration.getIdentifier());
        }
        
        // Fallback check
        return resolveVariable(declaration) != null;
    }
    
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(next);
        out.writeObject(declarationContext);
        // Note: tuples and valueResolver are transient runtime state
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        next = (ContextEntry) in.readObject();
        declarationContext = (BiLinearDeclarationContext) in.readObject();
    }
    
    @Override
    public String toString() {
        return "BiLinearContextEntry{" +
                "hasFirstNetwork=" + (firstNetworkTuple != null) +
                ", hasSecondNetwork=" + (secondNetworkTuple != null) +
                ", hasRightHandle=" + (rightHandle != null) +
                ", hasCombinedTuple=" + (combinedTuple != null) +
                '}';
    }
}
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

import org.drools.base.rule.Declaration;
import org.drools.base.rule.Pattern;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * BiLinearDeclarationContext manages variable declarations across two input networks
 * for BiLinearJoinNode. It provides unified declaration lookup that can resolve
 * variables from either input network and handles potential naming conflicts.
 * 
 * This class is essential for enabling cross-network variable resolution in 
 * BiLinearJoinNode constraints and tuple construction.
 */
public class BiLinearDeclarationContext {
    
    /** Declarations from the first input network */
    private final Map<String, Declaration> firstNetworkDeclarations;
    
    /** Declarations from the second input network */
    private final Map<String, Declaration> secondNetworkDeclarations;
    
    /** Combined view of all declarations with conflict resolution */
    private final Map<String, Declaration> combinedDeclarations;
    
    /** Mapping of declaration names to their source network (1 or 2) */
    private final Map<String, Integer> declarationNetworkMapping;
    
    /** Offset applied to second network tuple indices */
    private final int secondNetworkOffset;
    
    /**
     * Creates a BiLinearDeclarationContext from two sets of declarations
     * 
     * @param firstNetworkDeclarations Declarations from the first input network
     * @param secondNetworkDeclarations Declarations from the second input network
     * @param secondNetworkOffset Offset for second network tuple indices
     */
    public BiLinearDeclarationContext(Map<String, Declaration> firstNetworkDeclarations,
                                     Map<String, Declaration> secondNetworkDeclarations,
                                     int secondNetworkOffset) {
        this.firstNetworkDeclarations = new HashMap<>(firstNetworkDeclarations);
        this.secondNetworkDeclarations = new HashMap<>(secondNetworkDeclarations);
        this.secondNetworkOffset = secondNetworkOffset;
        this.combinedDeclarations = new HashMap<>();
        this.declarationNetworkMapping = new HashMap<>();
        
        buildCombinedDeclarations();
    }
    
    /**
     * Alternative constructor that extracts declarations from tuple sources
     * 
     * @param firstNetworkSource Left tuple source for first network
     * @param secondNetworkSource Left tuple source for second network  
     * @param secondNetworkOffset Offset for second network tuple indices
     */
    public BiLinearDeclarationContext(LeftTupleSource firstNetworkSource,
                                     LeftTupleSource secondNetworkSource,
                                     int secondNetworkOffset) {
        this.secondNetworkOffset = secondNetworkOffset;
        this.firstNetworkDeclarations = extractDeclarations(firstNetworkSource);
        this.secondNetworkDeclarations = extractDeclarations(secondNetworkSource);
        this.combinedDeclarations = new HashMap<>();
        this.declarationNetworkMapping = new HashMap<>();
        
        buildCombinedDeclarations();
    }
    
    /**
     * Builds the combined declarations map, handling conflicts and applying offsets
     */
    private void buildCombinedDeclarations() {
        // Add first network declarations (no offset needed)
        for (Map.Entry<String, Declaration> entry : firstNetworkDeclarations.entrySet()) {
            String name = entry.getKey();
            Declaration declaration = entry.getValue();
            
            combinedDeclarations.put(name, declaration);
            declarationNetworkMapping.put(name, 1);
        }
        
        // Add second network declarations with offset and conflict resolution
        for (Map.Entry<String, Declaration> entry : secondNetworkDeclarations.entrySet()) {
            String name = entry.getKey();
            Declaration originalDeclaration = entry.getValue();
            
            // Check for naming conflicts
            if (firstNetworkDeclarations.containsKey(name)) {
                // Conflict detected - need to handle this
                handleDeclarationConflict(name, originalDeclaration);
            } else {
                // No conflict - create offset declaration for second network
                Declaration offsetDeclaration = createOffsetDeclaration(originalDeclaration);
                combinedDeclarations.put(name, offsetDeclaration);
                declarationNetworkMapping.put(name, 2);
            }
        }
    }
    
    /**
     * Handles naming conflicts between networks
     * For now, we prioritize first network declarations and create qualified names for second network
     */
    private void handleDeclarationConflict(String name, Declaration secondNetworkDeclaration) {
        // Create a qualified name for the second network declaration
        String qualifiedName = "secondNetwork_" + name;
        Declaration offsetDeclaration = createOffsetDeclaration(secondNetworkDeclaration);
        
        combinedDeclarations.put(qualifiedName, offsetDeclaration);
        declarationNetworkMapping.put(qualifiedName, 2);
    }
    
    /**
     * Creates a declaration with offset tuple index for second network
     */
    private Declaration createOffsetDeclaration(Declaration original) {
        // Create a new declaration with the same properties but offset tuple index
        Declaration offsetDeclaration = new Declaration(
            original.getIdentifier(),
            original.getExtractor(),
            createOffsetPattern(original.getPattern())
        );
        
        offsetDeclaration.setDeclarationClass(original.getDeclarationClass());
        
        return offsetDeclaration;
    }
    
    /**
     * Creates a pattern with offset tuple index for second network
     */
    private Pattern createOffsetPattern(Pattern original) {
        if (original == null) {
            return null;
        }
        
        // Create new pattern with offset tuple index
        Pattern offsetPattern = new Pattern(
            original.getPatternId(),
            original.getTupleIndex() + secondNetworkOffset,
            original.getObjectIndex() + secondNetworkOffset,
            original.getObjectType(),
            original.getDeclaration() != null ? original.getDeclaration().getIdentifier() : null
        );
        
        // Note: Pattern.setDeclaration may not be available in this version
        // Declaration will be set during network building process
        
        return offsetPattern;
    }
    
    /**
     * Extracts declarations from a left tuple source
     * For now, this is a simplified implementation that will be enhanced
     * as we integrate with the actual network building process
     */
    private Map<String, Declaration> extractDeclarations(LeftTupleSource source) {
        Map<String, Declaration> declarations = new HashMap<>();
        
        if (source == null) {
            return declarations;
        }
        
        // For now, return empty map - this will be populated during actual network construction
        // when we have access to the proper rule context and declaration scope resolver
        // This method will be enhanced in Phase 5 when we integrate with the network builder
        
        return declarations;
    }
    
    /**
     * Resolves a declaration by name across both networks
     * 
     * @param identifier The variable identifier to resolve
     * @return The declaration if found, null otherwise
     */
    public Declaration resolveDeclaration(String identifier) {
        return combinedDeclarations.get(identifier);
    }
    
    /**
     * Gets all available declarations from both networks
     * 
     * @return Map of all combined declarations
     */
    public Map<String, Declaration> getAllDeclarations() {
        return new HashMap<>(combinedDeclarations);
    }
    
    /**
     * Gets declarations from a specific network
     * 
     * @param networkNumber 1 for first network, 2 for second network
     * @return Map of declarations from the specified network
     */
    public Map<String, Declaration> getNetworkDeclarations(int networkNumber) {
        if (networkNumber == 1) {
            return new HashMap<>(firstNetworkDeclarations);
        } else if (networkNumber == 2) {
            return new HashMap<>(secondNetworkDeclarations);
        } else {
            throw new IllegalArgumentException("Network number must be 1 or 2, got: " + networkNumber);
        }
    }
    
    /**
     * Determines which network a declaration belongs to
     * 
     * @param identifier The variable identifier
     * @return 1 for first network, 2 for second network, 0 if not found
     */
    public int getDeclarationNetwork(String identifier) {
        return declarationNetworkMapping.getOrDefault(identifier, 0);
    }
    
    /**
     * Checks if a declaration exists in either network
     * 
     * @param identifier The variable identifier
     * @return true if the declaration exists
     */
    public boolean hasDeclaration(String identifier) {
        return combinedDeclarations.containsKey(identifier);
    }
    
    /**
     * Gets the set of all declaration names across both networks
     * 
     * @return Set of declaration names
     */
    public Set<String> getAllDeclarationNames() {
        return combinedDeclarations.keySet();
    }
    
    /**
     * Gets the offset used for second network tuple indices
     * 
     * @return The second network offset
     */
    public int getSecondNetworkOffset() {
        return secondNetworkOffset;
    }
    
    /**
     * Creates a copy of this context (useful for constraint evaluation)
     * 
     * @return A new BiLinearDeclarationContext with the same declarations
     */
    public BiLinearDeclarationContext copy() {
        return new BiLinearDeclarationContext(
            firstNetworkDeclarations,
            secondNetworkDeclarations,
            secondNetworkOffset
        );
    }
    
    @Override
    public String toString() {
        return "BiLinearDeclarationContext{" +
                "firstNetwork=" + firstNetworkDeclarations.size() + " declarations, " +
                "secondNetwork=" + secondNetworkDeclarations.size() + " declarations, " +
                "combined=" + combinedDeclarations.size() + " declarations, " +
                "offset=" + secondNetworkOffset +
                '}';
    }
}
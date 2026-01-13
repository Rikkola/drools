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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.drools.base.RuleBuildContext;
import org.drools.base.common.NetworkNode;
import org.drools.base.common.RuleBasePartitionId;
import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.base.rule.EntryPointId;
import org.drools.base.rule.GroupElement;
import org.drools.base.rule.Pattern;
import org.drools.base.rule.RuleComponent;
import org.drools.base.rule.RuleConditionElement;
import org.drools.base.rule.constraint.AlphaNodeFieldConstraint;
import org.drools.base.rule.constraint.BetaConstraint;
import org.drools.base.rule.constraint.XpathConstraint;
import org.drools.core.common.BaseNode;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.impl.InternalRuleBase;
import org.drools.core.reteoo.*;
import org.drools.core.time.TemporalDependencyMatrix;

import static org.drools.base.rule.TypeDeclaration.NEVER_EXPIRES;

/**
 * A build context for Reteoo Builder
 */
public class BuildContext implements RuleBuildContext {

    private final List<TerminalNode> terminals = new ArrayList<>();

    // tuple source to attach next node to
    private LeftTupleSource tupleSource;
    // object source to attach next node to
    private ObjectSource objectSource;
    // object type cache to check for cross products
    private List<Pattern> patterns;

    // rule base to add rules to
    private final InternalRuleBase ruleBase;
    // rule being added at this moment
    private RuleImpl rule;
    private GroupElement subRule;
    // the rule component being processed at the moment
    private final Deque<RuleComponent> ruleComponent = new ArrayDeque<>();
    // a build stack to track nested elements
    private Deque<RuleConditionElement>    buildstack;
    // beta constraints from the last pattern attached
    private List<BetaConstraint>           betaconstraints;
    // alpha constraints from the last pattern attached
    private List<AlphaNodeFieldConstraint> alphaConstraints;
    // xpath constraints from the last pattern attached
    private List<XpathConstraint>            xpathConstraints;

    // the current entry point
    private EntryPointId                     currentEntryPoint;
    private boolean                          tupleMemoryEnabled;
    private boolean                          query;

    private int                              subRuleIndex;

    private final List<PathEndNode>          pathEndNodes = new ArrayList<>();

    /**
     * Stores the list of nodes being added that require partitionIds
     */
    private List<BaseNode> nodes = new ArrayList<>();
    /**
     * Stores the id of the partition this rule will be added to
     */
    private RuleBasePartitionId              partitionId;
    /**
     * the calculate temporal distance matrix
     */
    private TemporalDependencyMatrix         temporal;
    private ObjectTypeNode                   rootObjectTypeNode;
    private Pattern[]                        lastBuiltPatterns;
    // The reason why this is here is because forall can inject a
    //  "this == " + BASE_IDENTIFIER $__forallBaseIdentifier
    // Which we don't want to actually count in the case of forall node linking    
    private boolean                          emptyForAllBetaConstraints;
    private boolean                          attachPQN;
    private boolean                          terminated;

    private String                           consequenceName;
    
    // BiLinear pattern chain opportunities for optimization
    private Map<String, SharedPatternChain> biLinearOpportunities;
    
    // Phase 6: Shared network registry for true network sharing
    private SharedNetworkRegistry sharedNetworkRegistry;
    
    // Package-level shared nodes for the current rule package
    private Map<String, org.drools.core.reteoo.BiLinearJoinNode> packageSharedNodes;
    

    private final Collection<InternalWorkingMemory> workingMemories;

    public BuildContext(InternalRuleBase ruleBase, Collection<InternalWorkingMemory> workingMemories) {
        this.ruleBase = ruleBase;
        this.workingMemories = workingMemories;
        this.tupleMemoryEnabled = true;
        this.currentEntryPoint = EntryPointId.DEFAULT;
        this.attachPQN = true;
        this.emptyForAllBetaConstraints = false;
        
    }

    public List<TerminalNode> getTerminals() {
        return terminals;
    }

    public boolean isEmptyForAllBetaConstraints() {
        return emptyForAllBetaConstraints;
    }

    void setEmptyForAllBetaConstraints() {
        this.emptyForAllBetaConstraints = true;
    }

    public void syncObjectTypesWithObjectCount() {
        if (this.patterns == null) {
            return;
        }
        if (tupleSource != null) {
            while (this.patterns.size() > tupleSource.getObjectCount()) {
                this.patterns.remove(this.patterns.size()-1);
            }
        }
    }

    /**
     * @return the objectSource
     */
    public ObjectSource getObjectSource() {
        return this.objectSource;
    }

    /**
     * @param objectSource the objectSource to set
     */
    public void setObjectSource(final ObjectSource objectSource) {
        this.objectSource = objectSource;
    }

    public List<Pattern> getPatterns() {
        return this.patterns == null ? Collections.emptyList() : this.patterns;
    }

    public void addPattern(Pattern pattern) {
        if (this.patterns == null) {
            this.patterns = new ArrayList<>();
        }
        this.patterns.add( pattern );
    }

    /**
     * @return the tupleSource
     */
    public LeftTupleSource getTupleSource() {
        return this.tupleSource;
    }

    /**
     * @param tupleSource the tupleSource to set
     */
    public void setTupleSource(final LeftTupleSource tupleSource) {
        this.tupleSource = tupleSource;
    }

    /**
     * Returns context rulebase
     */
    public InternalRuleBase getRuleBase() {
        return this.ruleBase;
    }

    /**
     * Return the array of working memories associated with the given
     * rulebase.
     */
    public Collection<InternalWorkingMemory> getWorkingMemories() {
        return workingMemories;
    }

    /**
     * Returns an Id for the next node
     */
    public int getNextNodeId() {
        return ruleBase.getReteooBuilder().getNodeIdsGenerator().getNextId();
    }

    public int getNextMemoryId() {
        return ruleBase.getReteooBuilder().getMemoryIdsGenerator().getNextId();
    }

    // Global reservation counter for BiLinear nodes (the only shared nodes that can have memory ID collisions)
    private static int biLinearMemoryIdCounter = 1;  // IDs 1-10 for BiLinear (expandable)
    
    // Rule-specific BiLinear memory tracking to prevent cross-rule contamination
    private static java.util.Map<String, Integer> ruleSpecificBiLinearMemoryIds = new java.util.HashMap<>();
    
    /**
     * Gets next reserved memory ID for BiLinearJoinNode with rule-specific isolation
     */
    public int getNextBiLinearMemoryId() {
        String ruleName = rule.getName();

        // TODO Memory Ids Generator for Bilinear Memory IDs? Or merge the two?

        return ruleSpecificBiLinearMemoryIds.computeIfAbsent(ruleName,
                k -> getNextGlobalBiLinearMemoryId());
    }

    private int getNextGlobalBiLinearMemoryId() {
        if (biLinearMemoryIdCounter > 10) {
            biLinearMemoryIdCounter = 1;
        }
        return biLinearMemoryIdCounter++;
    }

    /**
     * Method used to undo previous id assignment
     */
    public void releaseId(NetworkNode node) {
        ruleBase.getReteooBuilder().releaseId(node);
    }

    /**
     * Adds the rce to the build stack
     */
    public void push(final RuleConditionElement rce) {
        if (this.buildstack == null) {
            this.buildstack = new ArrayDeque<>();
        }
        this.buildstack.addLast(rce);
    }

    /**
     * Removes the top stack element
     */
    public RuleConditionElement pop() {
        return this.buildstack.removeLast();
    }

    /**
     * Returns the top stack element without removing it
     */
    public RuleConditionElement peek() {
        return this.buildstack.getLast();
    }

    public Collection<RuleConditionElement> getBuildstack() {
        return this.buildstack == null ? Collections.emptyList() : buildstack;
    }

    public List<BetaConstraint> getBetaconstraints() {
        return this.betaconstraints;
    }

    public void setBetaconstraints(final List<BetaConstraint> betaconstraints) {
        this.betaconstraints = betaconstraints;
    }

    public List<AlphaNodeFieldConstraint> getAlphaConstraints() {
        return alphaConstraints;
    }

    void setAlphaConstraints(List<AlphaNodeFieldConstraint> alphaConstraints) {
        this.alphaConstraints = alphaConstraints;
    }

    List<XpathConstraint> getXpathConstraints() {
        return xpathConstraints;
    }

    List<PathEndNode> getPathEndNodes() {
        return pathEndNodes;
    }

    public void addPathEndNode(PathEndNode node) {
        pathEndNodes.add(node);
    }

    void setXpathConstraints(List<XpathConstraint> xpathConstraints) {
        this.xpathConstraints = xpathConstraints;
    }

    public boolean isTupleMemoryEnabled() {
        return this.tupleMemoryEnabled;
    }

    public void setTupleMemoryEnabled(boolean hasLeftMemory) {
        this.tupleMemoryEnabled = hasLeftMemory;
    }

    public boolean isQuery() {
        return query;
    }

    /**
     * @return the currentEntryPoint
     */
    public EntryPointId getCurrentEntryPoint() {
        return currentEntryPoint;
    }

    /**
     * @param currentEntryPoint the currentEntryPoint to set
     */
    public void setCurrentEntryPoint(EntryPointId currentEntryPoint) {
        this.currentEntryPoint = currentEntryPoint;
    }

    /**
     * @return the nodes
     */
    public List<BaseNode> getNodes() {
        return nodes;
    }

    BaseNode getLastNode() {
        return nodes.get(nodes.size()-1);
    }

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(List<BaseNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * @return the partitionId
     */
    public RuleBasePartitionId getPartitionId() {
        return partitionId;
    }

    /**
     * @param partitionId the partitionId to set
     */
    public void setPartitionId(RuleBasePartitionId partitionId) {
        this.partitionId = partitionId;
    }

    public boolean isStreamMode() {
        // eager rules don't need to use the event queue
        return this.temporal != null && !rule.isEager();
    }

    public long getExpirationOffset(Pattern pattern) {
        return temporal != null ? temporal.getExpirationOffset( pattern ) : NEVER_EXPIRES;
    }

    void setTemporalDistance(TemporalDependencyMatrix temporal) {
        this.temporal = temporal;
    }

    public RuleImpl getRule() {
        return rule;
    }

    public void setRule(RuleImpl rule) {
        this.rule = rule;
        if (rule.isQuery()) {
            this.query = true;
        }
    }

    public GroupElement getSubRule() {
        return subRule;
    }

    void setSubRule(GroupElement subRule) {
        this.subRule = subRule;
    }

    /**
     * Removes the top element from the rule component stack.
     * The rule component stack is used to add trackability to
     * the ReteOO nodes so that they can be linked to the rule
     * components that originated them.
     */
    public RuleComponent popRuleComponent() {
        return this.ruleComponent.pop();
    }

    /**
     * Peeks at the top element from the rule component stack.
     * The rule component stack is used to add trackability to
     * the ReteOO nodes so that they can be linked to the rule
     * components that originated them.
     */
    public RuleComponent peekRuleComponent() {
        return this.ruleComponent.isEmpty() ? null : this.ruleComponent.peek();
    }

    /**
     * Adds the ruleComponent to the top of the rule component stack.
     * The rule component stack is used to add trackability to
     * the ReteOO nodes so that they can be linked to the rule
     * components that originated them.
     */
    public void pushRuleComponent(RuleComponent ruleComponent) {
        this.ruleComponent.push(ruleComponent);
    }

    public ObjectTypeNode getRootObjectTypeNode() {
        return rootObjectTypeNode;
    }

    public void setRootObjectTypeNode(ObjectTypeNode source) {
        rootObjectTypeNode = source;
    }

    public Pattern[] getLastBuiltPatterns() {
        return lastBuiltPatterns;
    }

    public void setLastBuiltPattern(Pattern lastBuiltPattern) {
        if (this.lastBuiltPatterns == null) {
            this.lastBuiltPatterns = new Pattern[]{lastBuiltPattern, null};
        } else {
            this.lastBuiltPatterns[1] = this.lastBuiltPatterns[0];
            this.lastBuiltPatterns[0] = lastBuiltPattern;
        }
    }

    boolean isAttachPQN() {
        return attachPQN;
    }

    void setAttachPQN(final boolean attachPQN) {
        this.attachPQN = attachPQN;
    }

    boolean isTerminated() {
        return terminated;
    }

    void terminate() {
        this.terminated = true;
    }

    public String getConsequenceName() {
        return consequenceName;
    }

    public void setConsequenceName( String consequenceName ) {
        this.consequenceName = consequenceName;
    }

    public int getSubRuleIndex() {
        return subRuleIndex;
    }

    public void setSubRuleIndex(int subRuleIndex) {
        this.subRuleIndex = subRuleIndex;
    }
    
    /**
     * @return the biLinearOpportunities
     */
    public Map<String, SharedPatternChain> getBiLinearOpportunities() {
        return biLinearOpportunities;
    }
    
    /**
     * @param biLinearOpportunities the biLinearOpportunities to set
     */
    public void setBiLinearOpportunities(Map<String, SharedPatternChain> biLinearOpportunities) {
        this.biLinearOpportunities = biLinearOpportunities;
    }
    
    // Hash lookup methods for BiLinear optimization
    
    /**
     * Gets the tailHash for the current ObjectSource.
     * Now simplified to use direct tailHash lookup since Pattern and ObjectTypeNode 
     * have consistent tailHash values.
     * 
     * @return tailHash for current ObjectSource, or null if not found
     */
    public String getPatternHashForCurrentSources() {
        if (biLinearOpportunities == null || biLinearOpportunities.isEmpty()) {
            return null;
        }
        
        // Direct tailHash lookup from ObjectTypeNode - much simpler and more reliable
        ObjectSource objectSource = getObjectSource();
        if (objectSource == null) {
            return null;
        }
        
        ObjectTypeNode objectTypeNode = objectSource.getObjectTypeNode();
        if (objectTypeNode == null) {
            return null;
        }
        
        String tailHash = objectTypeNode.getTailHash();
        
        // Verify this tailHash exists in our BiLinear opportunities
        if (tailHash != null && biLinearOpportunities.containsKey(tailHash)) {
            return tailHash;
        }
        
        // If no direct match, check for partial match (for scoped hashes)
        if (tailHash != null) {
            for (String key : biLinearOpportunities.keySet()) {
                if (key.contains(tailHash)) {
                    return key;
                }
            }
        }
        return null;
    }
    
    /**
     * Gets a pre-computed hash for a specific ObjectSource and TupleSource combination.
     * 
     * @param leftType The left source object type name
     * @param rightType The right source object type name
     * @return Pre-computed hash for the source pair, or null if not found
     */
    public String getPatternHashForSources(String leftType, String rightType) {
        if (biLinearOpportunities == null || biLinearOpportunities.isEmpty() || 
            leftType == null || rightType == null) {
            return null;
        }
        
        // Search through BiLinear opportunities for matching source pair
        for (SharedPatternChain chain : biLinearOpportunities.values()) {
            String pairHash = chain.getSourcePairHash(leftType, rightType);
            if (pairHash != null) {
                return pairHash;
            }
        }
        
        return null;
    }
    
    /**
     * Finds a SharedPatternChain that contains the specified source pair.
     * This allows accessing the full pattern chain context and its pre-computed hashes.
     * 
     * @param leftType The left source object type name
     * @param rightType The right source object type name
     * @return The SharedPatternChain containing the source pair, or null if not found
     */
    public SharedPatternChain findPatternChainForSources(String leftType, String rightType) {
        if (biLinearOpportunities == null || biLinearOpportunities.isEmpty() || 
            leftType == null || rightType == null) {
            return null;
        }
        
        // Search through BiLinear opportunities for a chain containing the source pair
        for (SharedPatternChain chain : biLinearOpportunities.values()) {
            if (chain.containsSourcePair(leftType, rightType)) {
                return chain;
            }
        }
        
        return null;
    }
    
    /**
     * Extracts simple object type name from a source node for hash lookup.
     * This uses the same logic as BiLinearNodeFactory to ensure consistency.
     * 
     * @param source The source node (ObjectSource or LeftTupleSource)
     * @return The simple object type name, or null if extraction fails
     */
    private String extractObjectTypeName(Object source) {
        if (source == null) {
            return null;
        }
        
        // This would use the same extraction logic as BiLinearNodeFactory.extractSimpleObjectTypeName()
        // For now, return the class simple name as a basic implementation
        String className = source.getClass().getSimpleName();
        
        // Remove common suffixes to get clean type names
        if (className.endsWith("Node")) {
            className = className.substring(0, className.length() - 4);
        }
        if (className.endsWith("Source")) {
            className = className.substring(0, className.length() - 6);
        }
        
        return className;
    }
    
    /**
     * Gets the shared network registry for Phase 6 network sharing.
     */
    public SharedNetworkRegistry getSharedNetworkRegistry() {
        return sharedNetworkRegistry;
    }
    
    /**
     * Sets the shared network registry for Phase 6 network sharing.
     */
    public void setSharedNetworkRegistry(SharedNetworkRegistry sharedNetworkRegistry) {
        this.sharedNetworkRegistry = sharedNetworkRegistry;
    }
    
    /**
     * Gets the package-level shared nodes for the current rule package.
     */
    public Map<String, org.drools.core.reteoo.BiLinearJoinNode> getPackageSharedNodes() {
        return packageSharedNodes;
    }
    
    /**
     * Sets the package-level shared nodes for the current rule package.
     */
    public void setPackageSharedNodes(Map<String, org.drools.core.reteoo.BiLinearJoinNode> packageSharedNodes) {
        this.packageSharedNodes = packageSharedNodes;
    }

}

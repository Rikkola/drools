package org.drools.core.reteoo.builder;

import org.drools.core.common.BetaConstraints;
import org.drools.core.reteoo.*;

import java.util.HashMap;
import java.util.Map;

public class BiLinearNodeFactory {

    // CRITICAL FIX: Use ThreadLocal to prevent test interference from global shared state
    // Each test session gets its own BiLinear node cache to prevent cross-test contamination
    private static final ThreadLocal<Map<String, BiLinearJoinNode>> threadLocalBiLinearSharedNodes =
            ThreadLocal.withInitial(HashMap::new);

    // Helper method to access thread-local cache
    private static Map<String, BiLinearJoinNode> getBiLinearSharedNodes() {
        return threadLocalBiLinearSharedNodes.get();
    }

    // Method to clear thread-local cache (useful for test isolation)
    public static void clearSharedNodesCache() {
        threadLocalBiLinearSharedNodes.remove();
    }

    public BiLinearJoinNode buildBiLinearJoinNode(BetaConstraints betaConstraints, BuildContext context) {

        // Get tailHash directly from ObjectSource - now simplified and reliable
        String tailHash = context.getPatternHashForCurrentSources();

        // Find the matching SharedPatternChain using the simplified tailHash approach
        SharedPatternChain sharedChain = findSharedPatternChainByHash(context, tailHash);

        if (BiLinearDetector.isSuitableForBiLinearOptimization(sharedChain)) {
            try {

                LeftTupleSource firstLeftInput = context.getTupleSource();

                // Check if a BiLinear node for this pattern already exists
                // Use tailHash in signature creation for consistency
                String sharingSignature = createBiLinearSharingSignature(
                        firstLeftInput, context.getObjectSource(), betaConstraints, tailHash);

                BiLinearJoinNode existingBiLinearNode = findExistingBiLinearNode(sharingSignature);

                if (existingBiLinearNode != null) {
                    existingBiLinearNode.setSecondLeftInput(firstLeftInput);
                    return existingBiLinearNode;
                }

                // Create right adapter for the object source
                JoinRightAdapterNode rightAdapter = new JoinRightAdapterNode(
                        context.getNextNodeId(),
                        context.getObjectSource(),
                        context
                );

                BiLinearJoinNode biLinearJoin = new BiLinearJoinNode(
                        context.getNextNodeId(),
                        firstLeftInput,
                        null, // TODO something cleaner
                        rightAdapter,
                        betaConstraints,
                        context
                );

                storeBiLinearNodeForSharing(context, sharingSignature, biLinearJoin);

                return biLinearJoin;
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Creates a mock second left source for testing BiLinear construction.
     * In production, this would traverse the other rule's network to find the actual source.
     * Enhanced with rule name hashing for proper isolation.
     */
    private static LeftTupleSource createMockSecondLeftSource(BuildContext context, String otherRuleName) {
        // Create a rule-specific hash that includes the rule name
        String ruleHash = generateRuleSpecificHash(otherRuleName, context);

        // Create a unique mock source that represents the other rule's network path
        // This simulates finding a different LeftTupleSource from the other rule
        if (context.getObjectSource() != null) {
            try {
                // Create a LeftInputAdapter with a different ID to simulate cross-rule source
                int mockId = context.getNextNodeId() + 1000; // Offset to ensure different ID

                return new LeftInputAdapterNode(
                        mockId,
                        context.getObjectSource(),
                        context
                );
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Finds a SharedPatternChain by hash instead of recalculating object type names.
     * This ensures hash consistency between detection and lookup phases.
     */
    private static SharedPatternChain findSharedPatternChainByHash(BuildContext context, String hash) {
        if (hash == null || context.getBiLinearOpportunities() == null) {
            return null;
        }

        // First try direct hash lookup in the opportunities map
        SharedPatternChain directMatch = context.getBiLinearOpportunities().get(hash);
        if (directMatch != null) {
            return directMatch;
        }

        // If direct lookup fails, search through all chains for matching hash patterns
        for (SharedPatternChain chain : context.getBiLinearOpportunities().values()) {
            if (chain.getChainHash() != null && chain.getChainHash().equals(hash)) {
                return chain;
            }

            // Also check if the hash matches any stored pattern hashes
            if (chain.getPatternToHashMap() != null && chain.getPatternToHashMap().containsValue(hash)) {
                return chain;
            }
        }

        return null;
    }

    /**
     * Generates a rule-specific hash that includes the rule name for proper isolation.
     * This ensures that BiLinear optimizations are properly scoped per rule.
     */
    private static String generateRuleSpecificHash(String ruleName, BuildContext context) {
        StringBuilder hashBuilder = new StringBuilder();

        // Include rule name in hash
        hashBuilder.append("rule:").append(ruleName != null ? ruleName : "unknown");

        // Include source types
        if (context.getTupleSource() != null) {
            hashBuilder.append(",tuple:").append(context.getTupleSource().getClass().getSimpleName());
        }
        if (context.getObjectSource() != null) {
            hashBuilder.append(",object:").append(context.getObjectSource().getClass().getSimpleName());
        }

        // Create a simple hash
        return Integer.toString(hashBuilder.toString().hashCode());
    }


    /**
     * Creates a signature that matches the detection phase format, including constraints
     */
    private static String createSignatureWithConstraints(String leftType, String rightType, BetaConstraints betaConstraints) {
        StringBuilder sig = new StringBuilder();
        sig.append(leftType)
                .append("::")
                .append(rightType)
                .append("::");

        // Add constraints to match detection phase signature format
        // Use toString() of betaConstraints which includes constraint information
        String constraintInfo = betaConstraints.toString();
        if (constraintInfo != null && !constraintInfo.isEmpty()) {
            sig.append(constraintInfo).append(";");
        }
        return sig.toString();
    }

    /**
     * Creates a unique signature for BiLinear node sharing across rules
     * This signature focuses on pattern semantics rather than specific node IDs
     * to enable sharing between rules with equivalent patterns
     */
    private static String createBiLinearSharingSignature(LeftTupleSource firstInput,
                                                         ObjectSource objectSource,
                                                         BetaConstraints betaConstraints,
                                                         String preComputedHash) {
        StringBuilder sig = new StringBuilder("BiLinear::");

        // Use pre-computed hash if available, otherwise fall back to type extraction
        if (preComputedHash != null && !preComputedHash.isEmpty()) {
            sig.append("HASH:").append(preComputedHash).append("::");
        } else {
            // Fallback to semantic type information for backward compatibility
            String firstType = extractSimpleObjectTypeName(firstInput);
            String rightType = extractSimpleObjectTypeName(objectSource);

            sig.append("L1Type:").append(firstType).append("::");
            sig.append("RType:").append(rightType).append("::");
        }

        // Include semantic constraint signature for logical equivalence
        sig.append("CONST:").append(createConstraintSignature(betaConstraints));


        return sig.toString();
    }

    /**
     * Creates a normalized constraint signature for sharing
     */
    private static String createConstraintSignature(BetaConstraints betaConstraints) {
        if (betaConstraints == null) {
            return "null";
        }

        // Use constraint class and basic properties for signature
        // This should be the same for equivalent constraints between rules
        StringBuilder constSig = new StringBuilder();
        constSig.append(betaConstraints.getClass().getSimpleName());

        // Add constraint count for basic differentiation
        if (betaConstraints.getConstraints() != null) {
            constSig.append(":count=").append(betaConstraints.getConstraints().length);
        }

        return constSig.toString();
    }

    /**
     * Finds an existing BiLinear node that can be shared
     */
    private static BiLinearJoinNode findExistingBiLinearNode(String sharingSignature) {
        // Check if we have a sharing cache (use static cache, not BuildContext)
        Map<String, BiLinearJoinNode> biLinearSharedNodes = getBiLinearSharedNodes();

        if (biLinearSharedNodes != null) {

            BiLinearJoinNode existingNode = biLinearSharedNodes.get(sharingSignature);
            if (existingNode != null) {
                return existingNode;
            }
        }

        return null;
    }


    /**
     * Stores a BiLinear node for future sharing
     */
    private static void storeBiLinearNodeForSharing(BuildContext context,
                                                    String sharingSignature,
                                                    BiLinearJoinNode biLinearNode) {
        // Store directly in static cache
        getBiLinearSharedNodes().put(sharingSignature, biLinearNode);
    }

    /**
     * Simple object type name extraction
     */
    private static String extractSimpleObjectTypeName(Object source) {
        if (source instanceof ObjectTypeNode) {
            return ((ObjectTypeNode) source).getObjectType().getClassName();
        } else if (source instanceof AlphaNode) {
            return extractSimpleObjectTypeName(((AlphaNode) source).getParent());
        } else if (source instanceof LeftInputAdapterNode) {
            return extractSimpleObjectTypeName(((LeftInputAdapterNode) source).getParent());
        }
        return "Unknown";
    }

}

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

import org.drools.base.rule.Declaration;
import org.drools.base.rule.Pattern;
import org.drools.base.rule.constraint.BetaConstraint;
import org.drools.base.rule.constraint.Constraint;

import java.util.*;
import java.util.regex.Matcher;

/**
 * Compares patterns by creating normalized hashes that abstract away variable names
 * while preserving the semantic structure of patterns and constraints.
 * 
 * The hash generation process normalizes variable names to ensure that patterns
 * with identical structure but different variable names are considered equal.
 */
public class PatternHashComparator {

    /**
     * Compares two patterns by generating normalized hashes.
     * 
     * @param pattern1 First pattern to compare
     * @param pattern2 Second pattern to compare
     * @return true if patterns have identical normalized structure
     */
    public static boolean arePatternStructuresEqual(Pattern pattern1, Pattern pattern2) {
        if (pattern1 == null || pattern2 == null) {
            return pattern1 == pattern2;
        }
        
        String hash1 = generateNormalizedHash(pattern1);
        String hash2 = generateNormalizedHash(pattern2);
        
        return hash1.equals(hash2);
    }
    
    /**
     * Generates a normalized hash for a pattern that abstracts away variable names
     * while preserving the semantic structure.
     * 
     * @param pattern The pattern to generate hash for
     * @return Normalized hash string
     */
    public static String generateNormalizedHash(Pattern pattern) {
        if (pattern == null) {
            return "null";
        }
        
        StringBuilder hash = new StringBuilder();
        
        // Add object type
        hash.append("TYPE:").append(pattern.getObjectType().getClassName());
        
        // Add pattern index (order in rule)
        hash.append(";IDX:").append(pattern.getObjectIndex());
        
        // Process constraints with variable normalization
        List<String> normalizedConstraints = new ArrayList<>();
        
        for (Constraint constraint : pattern.getConstraints()) {
            String normalizedConstraint = normalizeConstraint(constraint, pattern);
            normalizedConstraints.add(normalizedConstraint);
        }
        
        // Sort constraints for consistent ordering
        Collections.sort(normalizedConstraints);
        
        // Add constraints to hash
        if (!normalizedConstraints.isEmpty()) {
            hash.append(";CONSTRAINTS:[");
            for (int i = 0; i < normalizedConstraints.size(); i++) {
                if (i > 0) hash.append(",");
                hash.append(normalizedConstraints.get(i));
            }
            hash.append("]");
        }
        
        return hash.toString();
    }
    
    /**
     * Normalizes a constraint by replacing variable names with normalized placeholders
     * while preserving field names, operators, and literal values.
     * 
     * @param constraint The constraint to normalize
     * @param ownerPattern The pattern that owns this constraint
     * @return Normalized constraint string
     */
    private static String normalizeConstraint(Constraint constraint, Pattern ownerPattern) {
        StringBuilder normalized = new StringBuilder();
        
        // Add constraint type
        normalized.append(constraint.getClass().getSimpleName());
        
        if (constraint instanceof BetaConstraint) {
            BetaConstraint betaConstraint = (BetaConstraint) constraint;
            normalized.append(":BETA");
            
            // Add required declarations with normalized variable names
            Declaration[] declarations = betaConstraint.getRequiredDeclarations();
            if (declarations != null && declarations.length > 0) {
                normalized.append(":DECLS[");
                
                Map<String, String> variableMap = new HashMap<>();
                int varCounter = 0;
                
                for (int i = 0; i < declarations.length; i++) {
                    if (i > 0) normalized.append(",");
                    
                    Declaration decl = declarations[i];
                    String varName = decl.getIdentifier();
                    String normalizedVar = variableMap.get(varName);
                    
                    if (normalizedVar == null) {
                        normalizedVar = "VAR" + varCounter++;
                        variableMap.put(varName, normalizedVar);
                    }
                    
                    normalized.append(normalizedVar)
                             .append(":");
                    
                    Class<?> declarationClass = decl.getDeclarationClass();
                    if (declarationClass != null) {
                        normalized.append(declarationClass.getSimpleName());
                    } else {
                        normalized.append("UNKNOWN_TYPE");
                    }
                    
                    // Add field information if available
                    if (decl.getExtractor() != null) {
                        normalized.append(".")
                                 .append(normalizeFieldName(decl.getExtractor().toString()));
                    }
                }
                
                normalized.append("]");
            }
            
            // Add operator/evaluator information
            normalized.append(":OP:").append(extractOperatorInfo(betaConstraint));
            
        } else {
            // Alpha constraint - Enhanced handling
            normalized.append(":ALPHA");
            
            // Try to extract more detailed information for alpha constraints
            String detailedAlphaInfo = extractDetailedAlphaConstraintInfo(constraint);
            normalized.append(":").append(detailedAlphaInfo);
        }
        
        return normalized.toString();
    }
    
    /**
     * Extracts detailed information from alpha constraints including field names,
     * operators, and literal values while normalizing variable references.
     */
    private static String extractDetailedAlphaConstraintInfo(Constraint constraint) {
        StringBuilder info = new StringBuilder();
        
        String constraintStr = constraint.toString();
        
        // Extract field name, operator, and value information
        // This is a heuristic approach that tries to identify common patterns
        
        // Look for field access patterns like "fieldName" or "getFieldName()"
        String fieldInfo = extractFieldInfo(constraintStr);
        if (!fieldInfo.isEmpty()) {
            info.append("FIELD:").append(fieldInfo);
        }
        
        // Extract operator information
        String operator = extractOperatorFromString(constraintStr);
        if (!operator.isEmpty()) {
            info.append(":OP:").append(operator);
        }
        
        // Extract literal values while normalizing variable references
        String valueInfo = extractValueInfo(constraintStr);
        if (!valueInfo.isEmpty()) {
            info.append(":VAL:").append(valueInfo);
        }
        
        // If we couldn't extract detailed info, fall back to normalized string
        if (info.length() == 0) {
            info.append(normalizeConstraintString(constraintStr));
        }
        
        return info.toString();
    }
    
    /**
     * Extracts field information from constraint string.
     */
    private static String extractFieldInfo(String constraintStr) {
        // Look for common field access patterns
        // Pattern: fieldName or getFieldName() or this.fieldName
        java.util.regex.Pattern fieldPattern = java.util.regex.Pattern.compile(
            "(?:get|is)([A-Z][a-zA-Z0-9_]*)|\\b([a-z][a-zA-Z0-9_]*)\\s*[=<>!]|this\\.([a-zA-Z_][a-zA-Z0-9_]*)"
        );
        
        java.util.regex.Matcher matcher = fieldPattern.matcher(constraintStr);
        if (matcher.find()) {
            // Return the first non-null group
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    return matcher.group(i).toLowerCase();
                }
            }
        }
        
        return "";
    }
    
    /**
     * Extracts operator information from constraint string.
     */
    private static String extractOperatorFromString(String constraintStr) {
        // Look for comparison operators
        if (constraintStr.contains("==")) return "EQUALS";
        if (constraintStr.contains("!=")) return "NOT_EQUALS";
        if (constraintStr.contains(">=")) return "GREATER_EQUAL";
        if (constraintStr.contains("<=")) return "LESS_EQUAL";
        if (constraintStr.contains(">")) return "GREATER";
        if (constraintStr.contains("<")) return "LESS";
        if (constraintStr.contains("matches")) return "MATCHES";
        if (constraintStr.contains("contains")) return "CONTAINS";
        if (constraintStr.contains("memberOf")) return "MEMBER_OF";
        if (constraintStr.contains("soundslike")) return "SOUNDS_LIKE";
        
        return "";
    }
    
    /**
     * Extracts value information from constraint string, normalizing variable references.
     */
    private static String extractValueInfo(String constraintStr) {
        StringBuilder values = new StringBuilder();
        
        // Look for literal values (numbers, quoted strings, boolean values, and unquoted identifiers)
        java.util.regex.Pattern literalPattern = java.util.regex.Pattern.compile(
            "\"([^\"]*)\"|'([^']*)'|(\\b\\d+\\.?\\d*\\b)|(\\btrue\\b|\\bfalse\\b|\\bnull\\b)|==\\s*([a-zA-Z_][a-zA-Z0-9_]*)"
        );
        
        java.util.regex.Matcher matcher = literalPattern.matcher(constraintStr);
        Set<String> foundValues = new HashSet<>();
        
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    foundValues.add("LIT:" + matcher.group(i));
                }
            }
        }
        
        // Also normalize any variable references to generic placeholders
        String normalizedVars = constraintStr.replaceAll("\\$[a-zA-Z_][a-zA-Z0-9_]*", "\\$VAR");
        
        // Add found literal values
        if (!foundValues.isEmpty()) {
            List<String> sortedValues = new ArrayList<>(foundValues);
            Collections.sort(sortedValues); // Sort for consistent ordering
            values.append(String.join(",", sortedValues));
        }
        
        return values.toString();
    }
    
    /**
     * Extracts operator information from a beta constraint.
     */
    private static String extractOperatorInfo(BetaConstraint betaConstraint) {
        try {
            String constraintStr = betaConstraint.toString();
            
            // Look for common operators
            if (constraintStr.contains("==")) return "EQUALS";
            if (constraintStr.contains("!=")) return "NOT_EQUALS"; 
            if (constraintStr.contains(">=")) return "GREATER_EQUAL";
            if (constraintStr.contains("<=")) return "LESS_EQUAL";
            if (constraintStr.contains(">")) return "GREATER";
            if (constraintStr.contains("<")) return "LESS";
            if (constraintStr.contains("matches")) return "MATCHES";
            if (constraintStr.contains("contains")) return "CONTAINS";
            if (constraintStr.contains("memberOf")) return "MEMBER_OF";
            
            // Fallback to constraint class simple name
            return betaConstraint.getClass().getSimpleName();
            
        } catch (Exception e) {
            return "UNKNOWN_OP";
        }
    }
    
    /**
     * Normalizes a constraint string by replacing variable names with placeholders
     * while preserving field names and literal values.
     */
    private static String normalizeConstraintString(String constraintStr) {
        if (constraintStr == null) {
            return "null";
        }
        
        // Remove package names for cleaner comparison
        String normalized = constraintStr.replaceAll("[a-zA-Z_][a-zA-Z0-9_]*\\.[a-zA-Z_][a-zA-Z0-9_]*\\.", "");
        
        // Replace variable patterns (identifiers that are likely variables)
        // This is a heuristic approach - variable names typically follow $varName pattern
        normalized = normalized.replaceAll("\\$[a-zA-Z_][a-zA-Z0-9_]*", "\\$VAR");
        
        // Replace other potential variable patterns
        normalized = normalized.replaceAll("\\b[a-z][a-zA-Z0-9_]*(?=\\s*[=<>!])", "VAR");
        
        // Normalize whitespace
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized;
    }
    
    /**
     * Normalizes field names by extracting the core field identifier.
     */
    private static String normalizeFieldName(String fieldStr) {
        if (fieldStr == null) {
            return "FIELD";
        }
        
        // Extract field name from extractor string
        // Common patterns: "FieldExtractor: fieldName" or similar
        java.util.regex.Pattern fieldPattern = java.util.regex.Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*$|\\s*[\\(\\)\\[\\]])");
        Matcher matcher = fieldPattern.matcher(fieldStr);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Fallback: return sanitized version
        return fieldStr.replaceAll("[^a-zA-Z0-9_]", "").trim();
    }
    
    /**
     * Compares two patterns and returns a detailed comparison result.
     * 
     * @param pattern1 First pattern
     * @param pattern2 Second pattern
     * @return ComparisonResult containing equality status and details
     */
    public static ComparisonResult comparePatterns(Pattern pattern1, Pattern pattern2) {
        if (pattern1 == null || pattern2 == null) {
            return new ComparisonResult(pattern1 == pattern2, "Null pattern comparison");
        }
        
        String hash1 = generateNormalizedHash(pattern1);
        String hash2 = generateNormalizedHash(pattern2);
        boolean equal = hash1.equals(hash2);
        
        StringBuilder details = new StringBuilder();
        details.append("Pattern 1 hash: ").append(hash1).append("\n");
        details.append("Pattern 2 hash: ").append(hash2).append("\n");
        details.append("Equal: ").append(equal);
        
        return new ComparisonResult(equal, details.toString());
    }
    
    /**
     * Result of pattern comparison containing equality status and details.
     */
    public static class ComparisonResult {
        private final boolean equal;
        private final String details;
        
        public ComparisonResult(boolean equal, String details) {
            this.equal = equal;
            this.details = details;
        }
        
        public boolean isEqual() {
            return equal;
        }
        
        public String getDetails() {
            return details;
        }
        
        @Override
        public String toString() {
            return "ComparisonResult{equal=" + equal + ", details='" + details + "'}";
        }
    }
}
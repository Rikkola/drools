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

import org.drools.base.definitions.rule.impl.RuleImpl;

import java.util.*;

/**
 * Detects potential recursion patterns in rules with FROM expressions
 * by analyzing data flow and variable dependencies.
 */
public class RecursionDetector {
    
    /**
     * Result of recursion analysis
     */
    public static class RecursionAnalysisResult {
        private final boolean hasRecursionRisk;
        private final String reason;
        private final Set<String> recursionPaths;
        private final int complexityScore;
        
        public RecursionAnalysisResult(boolean hasRecursionRisk, String reason, 
                                     Set<String> recursionPaths, int complexityScore) {
            this.hasRecursionRisk = hasRecursionRisk;
            this.reason = reason;
            this.recursionPaths = recursionPaths;
            this.complexityScore = complexityScore;
        }
        
        public boolean hasRecursionRisk() { return hasRecursionRisk; }
        public String getReason() { return reason; }
        public Set<String> getRecursionPaths() { return recursionPaths; }
        public int getComplexityScore() { return complexityScore; }
    }
    
    /**
     * Represents a variable and its dependencies
     */
    private static class VariableNode {
        private final String name;
        private final Set<String> dependencies = new HashSet<>();
        private final Set<String> influences = new HashSet<>();
        private boolean isFromSource = false;
        private boolean isModified = false;
        
        public VariableNode(String name) {
            this.name = name;
        }
        
        public String getName() { return name; }
        public Set<String> getDependencies() { return dependencies; }
        public Set<String> getInfluences() { return influences; }
        public boolean isFromSource() { return isFromSource; }
        public void setFromSource(boolean fromSource) { isFromSource = fromSource; }
        public boolean isModified() { return isModified; }
        public void setModified(boolean modified) { isModified = modified; }
    }
    
    /**
     * Analyzes a rule for recursion patterns in FROM expressions
     * Uses text-based analysis since AST is not available in drools-core
     */
    public static RecursionAnalysisResult analyzeRecursion(RuleImpl rule) {
        if (rule == null || rule.getLhs() == null) {
            return new RecursionAnalysisResult(false, "No rule to analyze", 
                                             Collections.emptySet(), 0);
        }
        
        // Use text-based recursion analysis
        return analyzeRecursionInText(rule.toString());
    }
    
    /**
     * Analyzes rule text for recursion patterns
     */
    private static RecursionAnalysisResult analyzeRecursionInText(String ruleText) {
        if (ruleText == null || !ruleText.contains(" from ")) {
            return new RecursionAnalysisResult(false, "No FROM expressions found", 
                                             Collections.emptySet(), 0);
        }
        
        Set<String> recursionPaths = new HashSet<>();
        boolean hasRecursion = false;
        StringBuilder reasonBuilder = new StringBuilder();
        int complexityScore = 0;
        
        // Extract FROM expressions and analyze for recursion patterns
        List<String> fromExpressions = extractFromExpressions(ruleText);
        List<String> variables = extractAllVariables(ruleText);
        
        complexityScore += fromExpressions.size() * 10;
        complexityScore += variables.size() * 2;
        
        // Check for dangerous recursion patterns
        for (String fromExpr : fromExpressions) {
            // Check for nested FROM
            if (fromExpr.toLowerCase().contains("from")) {
                hasRecursion = true;
                recursionPaths.add("nested_from");
                reasonBuilder.append("Nested FROM expression detected; ");
                complexityScore += 30;
            }
            
            // Check for self-reference patterns
            for (String var : variables) {
                if (fromExpr.contains(var) && ruleText.contains(var + ":") && 
                    ruleText.indexOf(var + ":") < ruleText.indexOf("from " + fromExpr)) {
                    hasRecursion = true;
                    recursionPaths.add("self_reference:" + var);
                    reasonBuilder.append("Self-reference pattern with variable: ").append(var).append("; ");
                    complexityScore += 25;
                }
            }
            
            // Check for working memory access
            if (fromExpr.toLowerCase().contains("working") || 
                fromExpr.toLowerCase().contains("memory") ||
                fromExpr.toLowerCase().contains("session")) {
                complexityScore += 20;
                reasonBuilder.append("Working memory access detected; ");
            }
        }
        
        // Check for eval expressions that might modify variables
        if (ruleText.contains("eval(")) {
            complexityScore += 15;
            reasonBuilder.append("Eval expressions present; ");
        }
        
        String reason = reasonBuilder.length() > 0 ? 
            reasonBuilder.toString() : "No recursion patterns detected";
        
        return new RecursionAnalysisResult(hasRecursion, reason, recursionPaths, complexityScore);
    }
    
    /**
     * Extracts FROM expressions from rule text
     */
    private static List<String> extractFromExpressions(String ruleText) {
        List<String> expressions = new ArrayList<>();
        if (ruleText == null) return expressions;
        
        String[] lines = ruleText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains(" from ")) {
                int fromIndex = line.indexOf(" from ");
                if (fromIndex > 0 && fromIndex + 6 < line.length()) {
                    String expression = line.substring(fromIndex + 6).trim();
                    expression = expression.replaceAll("[;,\\s]*$", "");
                    if (!expression.isEmpty()) {
                        expressions.add(expression);
                    }
                }
            }
        }
        
        return expressions;
    }
    
    /**
     * Extracts all variable references from rule text
     */
    private static List<String> extractAllVariables(String ruleText) {
        Set<String> variableSet = new HashSet<>();
        if (ruleText == null) return new ArrayList<>();
        
        String[] tokens = ruleText.split("\\s+");
        for (String token : tokens) {
            if (token.startsWith("$") && token.length() > 1) {
                String var = token.replaceAll("[^a-zA-Z0-9_$]", "");
                if (var.length() > 1) {
                    variableSet.add(var);
                }
            }
        }
        
        return new ArrayList<>(variableSet);
    }
}
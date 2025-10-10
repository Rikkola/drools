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
package org.drools.mvel.integrationtests;

import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.builder.BiLinearDetector;
import org.drools.core.reteoo.builder.PatternChainHasher;
import org.drools.core.reteoo.builder.SharedPatternChain;
import org.drools.mvel.integrationtests.phreak.A;
import org.drools.mvel.integrationtests.phreak.B;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.io.ResourceType;
import org.kie.internal.utils.KieHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to analyze whether BiLinearSimpleTest scenarios actually share pattern tails
 */
public class BiLinearPatternTailAnalysisTest {

    @Test
    public void analyzeSimpleScenarioPatternTails() {
        System.out.println("\n🔍 ANALYZING SIMPLE SCENARIO PATTERN TAILS");
        
        // Enable BiLinear
        System.setProperty("drools.bilinear.enabled", "true");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object)\n" +
            "then\n" +
            "    System.out.println(\"Rule1 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object)\n" +
            "then\n" +
            "    System.out.println(\"Rule2 fired\");\n" +
            "end\n";

        try {
            KieBase kieBase = new KieHelper().addContent(drl, ResourceType.DRL).build();
            analyzePatternTails(kieBase, "Simple Scenario");
        } finally {
            System.setProperty("drools.bilinear.enabled", "false");
        }
    }

    @Test
    public void analyzeComplexScenarioPatternTails() {
        System.out.println("\n🔍 ANALYZING COMPLEX SCENARIO PATTERN TAILS");
        
        // Enable BiLinear
        System.setProperty("drools.bilinear.enabled", "true");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"ComplexRule1\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object)\n" +
            "    $c : A(object < 100)\n" +
            "then\n" +
            "    System.out.println(\"ComplexRule1 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"ComplexRule2\"\n" +
            "when\n" +
            "    $x : A(object > 5)\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object)\n" +
            "then\n" +
            "    System.out.println(\"ComplexRule2 fired\");\n" +
            "end\n";

        try {
            KieBase kieBase = new KieHelper().addContent(drl, ResourceType.DRL).build();
            analyzePatternTails(kieBase, "Complex Scenario");
        } finally {
            System.setProperty("drools.bilinear.enabled", "false");
        }
    }

    private void analyzePatternTails(KieBase kieBase, String scenarioName) {
        System.out.println("🔍 Analyzing Pattern Tails for: " + scenarioName);
        
        // Extract rules
        List<RuleImpl> rules = new ArrayList<>();
        if (!kieBase.getKiePackages().isEmpty()) {
            KiePackage pkg = kieBase.getKiePackages().iterator().next();
            for (Rule rule : pkg.getRules()) {
                if (rule instanceof RuleImpl) {
                    rules.add((RuleImpl) rule);
                }
            }
        }

        System.out.println("📊 Found " + rules.size() + " rules for analysis");

        // Analyze each rule's pattern chain hashes
        for (RuleImpl rule : rules) {
            PatternChainHasher.ChainHashResult result = PatternChainHasher.generateChainHashes(rule);
            
            System.out.println("\n🔗 Rule: " + rule.getName());
            System.out.println("   Patterns: " + result.getTailHashes().size());
            
            for (PatternChainHasher.TailHash tailHash : result.getTailHashes()) {
                System.out.println("   - Tail[" + tailHash.getStartIndex() + ", len=" + tailHash.getLength() + "]: " + 
                    tailHash.getHash().substring(0, Math.min(80, tailHash.getHash().length())) + "...");
            }
        }

        // Test BiLinear detection
        Map<String, SharedPatternChain> opportunities = BiLinearDetector.detectBiLinearOpportunities(rules, "testKieBase");
        
        System.out.println("\n📈 BiLinear Detection Results:");
        System.out.println("   Opportunities found: " + opportunities.size());
        
        for (Map.Entry<String, SharedPatternChain> entry : opportunities.entrySet()) {
            SharedPatternChain pattern = entry.getValue();
            System.out.println("   - Pattern signature: " + entry.getKey());
            System.out.println("     Participating rules: " + pattern.getParticipatingRules().size());
            System.out.println("     Can optimize: " + pattern.canOptimizeWithBiLinear());
        }

        if (opportunities.isEmpty()) {
            System.out.println("   ❌ No BiLinear opportunities detected");
        } else {
            System.out.println("   ✅ BiLinear opportunities successfully detected!");
        }
    }
}
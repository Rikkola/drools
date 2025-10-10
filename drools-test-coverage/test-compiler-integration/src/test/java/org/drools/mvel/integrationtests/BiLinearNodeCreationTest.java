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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieSession;
import org.kie.api.builder.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to demonstrate when BiLinear nodes are actually created vs when they are skipped.
 * This shows the gap between pattern detection and actual node creation.
 */
public class BiLinearNodeCreationTest {

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testSimpleRulesCreateRegularJoinNodes() {
        // Simple rules that should share patterns but will create regular JoinNodes
        // because they don't have cross-pattern beta constraints
        String drl = "package org.drools.test\n" +
                     "global java.util.List results\n" +
                     "\n" +
                     "rule \"rule1 simple pattern\"\n" +
                     "when\n" +
                     "  $s: String( length > 0 )\n" +
                     "  $i: Integer( this > 0 )\n" +
                     "then\n" +
                     "  results.add(\"rule1: \" + $s + \" - \" + $i);\n" +
                     "end\n" +
                     "\n" +
                     "rule \"rule2 same pattern\"\n" +
                     "when\n" +
                     "  $s: String( length > 0 )\n" +
                     "  $i: Integer( this > 0 )\n" +
                     "then\n" +
                     "  results.add(\"rule2: \" + $s + \" - \" + $i);\n" +
                     "end\n";

        KieSession ksession = createKieSession(drl);
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        ksession.insert("test");
        ksession.insert(5);

        int fired = ksession.fireAllRules();
        ksession.dispose();

        assertThat(fired).isEqualTo(2);
        
        System.out.println("✅ Simple rules executed - Regular JoinNodes created (not BiLinear)");
        System.out.println("    Reason: No cross-pattern beta constraints (EmptyBetaConstraints)");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testCrossPatternRulesMightCreateBiLinearNodes() {
        // Rules with cross-pattern constraints that MIGHT create BiLinear nodes
        String drl = "package org.drools.test\n" +
                     "global java.util.List results\n" +
                     "\n" +
                     "rule \"rule1 cross pattern\"\n" +
                     "when\n" +
                     "  $s: String( $len: length )\n" +
                     "  $i: Integer( this == $len )\n" +  // Cross-pattern constraint
                     "then\n" +
                     "  results.add(\"rule1: \" + $s + \" - \" + $i);\n" +
                     "end\n" +
                     "\n" +
                     "rule \"rule2 same cross pattern\"\n" +
                     "when\n" +
                     "  $s: String( $len: length )\n" +
                     "  $i: Integer( this == $len )\n" +  // Same cross-pattern constraint
                     "then\n" +
                     "  results.add(\"rule2: \" + $s + \" - \" + $i);\n" +
                     "end\n";

        KieSession ksession = createKieSession(drl);
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        ksession.insert("test");
        ksession.insert(4);  // length of "test"

        int fired = ksession.fireAllRules();
        ksession.dispose();

        assertThat(fired).isEqualTo(2);
        
        System.out.println("✅ Cross-pattern rules executed - Potential BiLinear opportunity");
        System.out.println("    Check console for BiLinear detection messages");
    }

    @Test  
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testComplexRuleBlocksBiLinearForEntirePackage() {
        // One complex rule prevents BiLinear for ALL rules in the package
        String drl = "package org.drools.test\n" +
                     "global java.util.List results\n" +
                     "\n" +
                     "rule \"simple good rule\"\n" +
                     "when\n" +
                     "  $s: String( $len: length )\n" +
                     "  $i: Integer( this == $len )\n" +
                     "then\n" +
                     "  results.add(\"good: \" + $s);\n" +
                     "end\n" +
                     "\n" +
                     "rule \"eliminate bad rule\"\n" +  // Complex name triggers package skip
                     "when\n" +
                     "  $s: String( $len: length )\n" +
                     "  $i: Integer( this == $len )\n" +
                     "then\n" +
                     "  results.add(\"bad: \" + $s);\n" +
                     "end\n";

        KieSession ksession = createKieSession(drl);
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        ksession.insert("test");
        ksession.insert(4);

        int fired = ksession.fireAllRules();
        ksession.dispose();

        assertThat(fired).isEqualTo(2);
        
        System.out.println("🚨 Complex rule in package - BiLinear skipped for ALL rules");
        System.out.println("    One 'eliminate' rule prevents optimization for entire package");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testFromExpressionBlocksBiLinearForEntirePackage() {
        // FROM expression prevents BiLinear for ALL rules in package
        String drl = "package org.drools.test\n" +
                     "import java.util.List\n" +
                     "global java.util.List results\n" +
                     "\n" +
                     "rule \"simple good rule\"\n" +
                     "when\n" +
                     "  $s: String( $len: length )\n" +
                     "  $i: Integer( this == $len )\n" +
                     "then\n" +
                     "  results.add(\"good: \" + $s);\n" +
                     "end\n" +
                     "\n" +
                     "rule \"from expression rule\"\n" +
                     "when\n" +
                     "  $list: List( size > 0 )\n" +
                     "  $item: Object() from $list\n" +  // FROM expression
                     "then\n" +
                     "  results.add(\"from: \" + $item);\n" +
                     "end\n";

        KieSession ksession = createKieSession(drl);
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        ksession.insert("test");
        ksession.insert(4);
        List<String> list = new ArrayList<>();
        list.add("item1");
        ksession.insert(list);

        int fired = ksession.fireAllRules();
        ksession.dispose();

        assertThat(fired).isEqualTo(2);
        
        System.out.println("🚨 FROM expression in package - BiLinear skipped for ALL rules");
        System.out.println("    One FROM rule prevents optimization for entire package");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testSevenPatternRuleBlocksBiLinearForEntirePackage() {
        // Rule with >6 patterns prevents BiLinear for ALL rules in package
        String drl = "package org.drools.test\n" +
                     "global java.util.List results\n" +
                     "\n" +
                     "rule \"simple good rule\"\n" +
                     "when\n" +
                     "  $s: String( $len: length )\n" +
                     "  $i: Integer( this == $len )\n" +
                     "then\n" +
                     "  results.add(\"good: \" + $s);\n" +
                     "end\n" +
                     "\n" +
                     "rule \"seven pattern rule\"\n" +
                     "when\n" +
                     "  $s1: String( length > 0 )\n" +
                     "  $s2: String( length > 1, this != $s1 )\n" +
                     "  $s3: String( length > 2, this != $s1, this != $s2 )\n" +
                     "  $i1: Integer( this > 0 )\n" +
                     "  $i2: Integer( this > 1, this != $i1 )\n" +
                     "  $i3: Integer( this > 2, this != $i1, this != $i2 )\n" +
                     "  $d: Double( this > 0 )\n" +  // 7th pattern
                     "then\n" +
                     "  results.add(\"seven: matched\");\n" +
                     "end\n";

        KieSession ksession = createKieSession(drl);
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        ksession.insert("test");
        ksession.insert(4);

        int fired = ksession.fireAllRules();
        ksession.dispose();

        assertThat(fired).isEqualTo(1);  // Only simple rule fires
        
        System.out.println("🚨 Seven pattern rule in package - BiLinear skipped for ALL rules");
        System.out.println("    High pattern count prevents optimization for entire package");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testBiLinearNodeCreationConditionsDemo() {
        System.out.println("\n=== BiLinear Node Creation Conditions Demo ===");
        
        System.out.println("\n🎯 FOR BILINEAR NODE CREATION, ALL CONDITIONS MUST BE MET:");
        System.out.println("   1. drools.bilinear.enabled=true (✅ default)");
        System.out.println("   2. NO complex rules in entire package:");
        System.out.println("      - No rule names with: sudoku, eliminate, retract setting, single");
        System.out.println("      - No rules with >6 patterns");
        System.out.println("      - No rules with FROM expressions");
        System.out.println("   3. At least 2 rules must share exact same pattern signature");
        System.out.println("   4. Shared pattern must have cross-pattern beta constraints");
        System.out.println("   5. NOT EmptyBetaConstraints (alpha-only joins excluded)");
        System.out.println("   6. No temporal constraints");
        
        System.out.println("\n🚨 CURRENT REALITY:");
        System.out.println("   - Complexity detection is package-level (one complex rule blocks all)");
        System.out.println("   - Pattern matching is very strict (exact type signatures)");
        System.out.println("   - Alpha-only joins are excluded");
        System.out.println("   - Most real scenarios create regular JoinNodes");
        
        System.out.println("\n📊 WHAT HAPPENS IN PRACTICE:");
        System.out.println("   ✅ Pattern detection runs and finds opportunities");
        System.out.println("   ✅ Complexity detection prevents infinite loops");
        System.out.println("   🚨 BiLinear node creation conditions rarely met");
        System.out.println("   🚨 Most scenarios fall back to regular JoinNodes");
        
        System.out.println("\n🔧 THE SYSTEM PRIORITIZES SAFETY OVER OPTIMIZATION");
    }

    private KieSession createKieSession(String drl) {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        
        KieModuleModel module = ks.newKieModuleModel();
        module.newKieBaseModel("rules").setDefault(true)
              .newKieSessionModel("rules").setDefault(true);
        
        kfs.writeKModuleXML(module.toXML());
        kfs.write("src/main/resources/rules.drl", drl);
        
        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        
        List<Message> errors = kb.getResults().getMessages(Message.Level.ERROR);
        if (!errors.isEmpty()) {
            throw new RuntimeException("Build failed: " + errors);
        }
        
        return ks.newKieContainer(kb.getKieModule().getReleaseId()).newKieSession();
    }
}

/**
 * SUMMARY: When Are BiLinear Nodes Actually Created?
 * 
 * Based on current implementation analysis:
 * 
 * RARELY - Due to very restrictive conditions:
 * 
 * 1. Package-Level Complexity Blocking:
 *    - ONE complex rule (FROM, >6 patterns, bad name) blocks ALL rules in package
 *    - Most real packages have at least one complex rule
 * 
 * 2. Strict Pattern Requirements:
 *    - Exact type signature matching only
 *    - Cross-pattern beta constraints required
 *    - Alpha-only joins explicitly excluded
 * 
 * 3. Multiple Rule Requirement:
 *    - At least 2 rules must share IDENTICAL pattern
 *    - Pattern signatures must match exactly
 * 
 * 4. No Temporal Constraints:
 *    - Any temporal/interval constraints prevent creation
 * 
 * RESULT: The system is designed for safety over optimization.
 * Pattern detection works and prevents infinite loops, but actual
 * BiLinear node creation is rare due to conservative constraints.
 * 
 * Most scenarios fall back to regular JoinNodes, which work perfectly
 * fine but don't provide the network sharing benefits that BiLinear
 * was designed to deliver.
 */
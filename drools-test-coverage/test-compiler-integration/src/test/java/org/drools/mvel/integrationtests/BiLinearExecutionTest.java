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

import org.drools.mvel.integrationtests.phreak.A;
import org.drools.mvel.integrationtests.phreak.B;
import org.drools.core.reteoo.ReteDumper;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test that demonstrates the BiLinear execution issue.
 * With BiLinear enabled: rules don't fire
 * With BiLinear disabled: rules fire correctly
 */
public class BiLinearExecutionTest {

    @Test
    public void testBiLinearSharedNodeConnection() {
        System.out.println("\n🔥 Test: BiLinear Shared Node Connection");
        System.out.println("=======================================");

        System.setProperty("drools.bilinear.enabled", "true");

        String drl =
                "import " + A.class.getCanonicalName() + "\n" +
                        "import " + B.class.getCanonicalName() + "\n" +
                        "\n" +
                        "rule \"SimpleRule1\"\n" +
                        "when\n" +
                        "    $a : A(object > 0)\n" +
                        "    $b : B(object > 1)\n" +  // Both should match
                        "then\n" +
                        "end\n" +
                        "\n" +
                        "rule \"SimpleRule2\"\n" +
                        "when\n" +
                        "    $b : B(object > 1)\n" +  // Should match B(10)
                        "then\n" +
                        "end\n";

        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        KieBase kieBase = kieHelper.build();

        KieSession session = kieBase.newKieSession();
        
        // Set up agenda listener to capture rule firings
        List<String> rulesFired = new ArrayList<>();
        session.addEventListener(new org.kie.api.event.rule.DefaultAgendaEventListener() {
            @Override
            public void afterMatchFired(org.kie.api.event.rule.AfterMatchFiredEvent event) {
                String ruleName = event.getMatch().getRule().getName();
                rulesFired.add(ruleName);
                System.out.println("🎯 Agenda Listener: " + ruleName + " fired");
            }
        });

        // Dump the Rete network structure to see if BiLinear nodes are created
        System.out.println("\n🔍 NETWORK STRUCTURE:");
        System.out.println("====================");
        ReteDumper.dumpRete(kieBase);
        System.out.println("====================\n");

        System.out.println("📊 Before firing: Inserting A(5), B(10)");
        session.insert(new A(5));    // A.object = 5 > 0, matches
        session.insert(new B(10));   // B.object = 10 > 0, matches

        int firedWithBiLinear = session.fireAllRules();
        session.dispose();

        System.out.println("\n📊 After firing:");
        System.out.println("   Total rules fired: " + firedWithBiLinear);
        System.out.println("   Rules captured by listener: " + rulesFired.size());
        System.out.println("   Rule firings details:");
        for (int i = 0; i < rulesFired.size(); i++) {
            System.out.println("     " + (i + 1) + ". " + rulesFired.get(i));
        }

        // Count specific rule firings
        long rule1Count = rulesFired.stream().filter(s -> s.equals("SimpleRule1")).count();
        long rule2Count = rulesFired.stream().filter(s -> s.equals("SimpleRule2")).count();
        
        System.out.println("\n📊 Rule firing breakdown:");
        System.out.println("   SimpleRule1 fired: " + rule1Count + " times");
        System.out.println("   SimpleRule2 fired: " + rule2Count + " times");
        System.out.println("\n✅ Expected behavior:");
        System.out.println("   SimpleRule1: 1 time");
        System.out.println("   SimpleRule2: 1 time");
        System.out.println("   Total: 2 firings");

        // Test with BiLinear DISABLED to verify identical behavior
        System.setProperty("drools.bilinear.enabled", "false");
        System.out.println("\n🔍 Testing with BiLinear DISABLED for comparison...");
        
        KieHelper kieHelperOff = new KieHelper();
        kieHelperOff.addContent(drl, ResourceType.DRL);
        KieBase kieBaseOff = kieHelperOff.build();
        
        KieSession sessionOff = kieBaseOff.newKieSession();
        
        List<String> rulesFiredOff = new ArrayList<>();
        sessionOff.addEventListener(new org.kie.api.event.rule.DefaultAgendaEventListener() {
            @Override
            public void afterMatchFired(org.kie.api.event.rule.AfterMatchFiredEvent event) {
                String ruleName = event.getMatch().getRule().getName();
                rulesFiredOff.add(ruleName);
                System.out.println("🎯 BiLinear OFF - Agenda Listener: " + ruleName + " fired");
            }
        });
        
        sessionOff.insert(new A(5));
        sessionOff.insert(new B(10));
        
        int firedWithoutBiLinear = sessionOff.fireAllRules();
        sessionOff.dispose();
        
        System.out.println("\n📊 BiLinear OFF Results:");
        System.out.println("   Total rules fired: " + firedWithoutBiLinear);
        System.out.println("   Rules captured by listener: " + rulesFiredOff.size());
        for (int i = 0; i < rulesFiredOff.size(); i++) {
            System.out.println("     " + (i + 1) + ". " + rulesFiredOff.get(i));
        }
        
        // Reset BiLinear to enabled
        System.setProperty("drools.bilinear.enabled", "true");
        
        // CRITICAL: BiLinear optimization should be transparent - results must be identical
        System.out.println("\n🔍 Comparing BiLinear ON vs OFF:");
        System.out.println("   BiLinear ON:  " + firedWithBiLinear + " rules fired");
        System.out.println("   BiLinear OFF: " + firedWithoutBiLinear + " rules fired");
        System.out.println("   Results should be identical!");
        
        // Verify BiLinear optimization is transparent
        assertThat(firedWithBiLinear).as("BiLinear ON should fire same number of rules as BiLinear OFF").isEqualTo(firedWithoutBiLinear);
        assertThat(rulesFired).as("BiLinear ON should fire same rules as BiLinear OFF").containsExactlyInAnyOrderElementsOf(rulesFiredOff);
        
        // Verify expected rule firings (should be 2 regardless)
        assertThat(firedWithBiLinear).as("Total rules fired should be 2").isEqualTo(2);
        assertThat(rule1Count).as("SimpleRule1 should fire exactly once").isEqualTo(1);
        assertThat(rule2Count).as("SimpleRule2 should fire exactly once").isEqualTo(1);
        assertThat(rulesFired).as("Should contain exactly the expected rule firings").containsExactlyInAnyOrder(
            "SimpleRule1",
            "SimpleRule2"
        );
    }

    @Test
    public void testBiLinearExecutionIssue() {
        System.out.println("\n🔥 BiLinear Execution Test");
        System.out.println("==========================================");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"SimpleRule1\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > ((Integer)$a.object))\n" +  // Cross-pattern constraint triggers BiLinear
            "then\n" +
            "    System.out.println(\"SimpleRule1 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
            "end\n" +
            "\n" +
            "rule \"SimpleRule2\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > ((Integer)$a.object))\n" +  // Same pattern - should create BiLinear optimization
            "then\n" +
            "    System.out.println(\"SimpleRule2 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
            "end\n";

        // Test with BiLinear ENABLED 
        System.setProperty("drools.bilinear.enabled", "true");
        System.out.println("🔍 Testing with BiLinear ENABLED...");
        
        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        KieBase kieBase = kieHelper.build();
        
        KieSession session = kieBase.newKieSession();
        
        // Set up agenda listener to capture rule firings
        List<String> rulesFiredWithBiLinear = new ArrayList<>();
        session.addEventListener(new org.kie.api.event.rule.DefaultAgendaEventListener() {
            @Override
            public void afterMatchFired(org.kie.api.event.rule.AfterMatchFiredEvent event) {
                String ruleName = event.getMatch().getRule().getName();
                rulesFiredWithBiLinear.add(ruleName);
                System.out.println("🎯 BiLinear ON - Agenda Listener: " + ruleName + " fired");
            }
        });
        
        session.insert(new A(5));    // A.object = 5
        session.insert(new B(10));   // B.object = 10 > 5, so constraint should match
        
        int firedWithBiLinear = session.fireAllRules();
        session.dispose();
        
        System.out.println("📊 Rules fired with BiLinear enabled: " + firedWithBiLinear);
        System.out.println("📊 Rules captured by listener: " + rulesFiredWithBiLinear.size());
        for (int i = 0; i < rulesFiredWithBiLinear.size(); i++) {
            System.out.println("     " + (i + 1) + ". " + rulesFiredWithBiLinear.get(i));
        }
        
        // Test with BiLinear DISABLED 
        System.setProperty("drools.bilinear.enabled", "false");
        System.out.println("\n🔍 Testing with BiLinear DISABLED...");
        
        kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        kieBase = kieHelper.build();
        
        session = kieBase.newKieSession();
        
        // Set up agenda listener for disabled BiLinear
        List<String> rulesFiredWithoutBiLinear = new ArrayList<>();
        session.addEventListener(new org.kie.api.event.rule.DefaultAgendaEventListener() {
            @Override
            public void afterMatchFired(org.kie.api.event.rule.AfterMatchFiredEvent event) {
                String ruleName = event.getMatch().getRule().getName();
                rulesFiredWithoutBiLinear.add(ruleName);
                System.out.println("🎯 BiLinear OFF - Agenda Listener: " + ruleName + " fired");
            }
        });
        
        session.insert(new A(5));
        session.insert(new B(10));
        
        int firedWithoutBiLinear = session.fireAllRules();
        session.dispose();
        
        System.out.println("📊 Rules fired with BiLinear disabled: " + firedWithoutBiLinear);
        System.out.println("📊 Rules captured by listener: " + rulesFiredWithoutBiLinear.size());
        for (int i = 0; i < rulesFiredWithoutBiLinear.size(); i++) {
            System.out.println("     " + (i + 1) + ". " + rulesFiredWithoutBiLinear.get(i));
        }
        
        // Reset to default
        System.setProperty("drools.bilinear.enabled", "true");
        
        System.out.println("\n📋 Results Summary:");
        System.out.println("   • BiLinear enabled:  " + firedWithBiLinear + " rules fired");
        System.out.println("   • BiLinear disabled: " + firedWithoutBiLinear + " rules fired");
        
        if (firedWithBiLinear == 0 && firedWithoutBiLinear == 2) {
            System.out.println("   ❌ CONFIRMED: BiLinear optimization prevents rule execution!");
            System.out.println("   This demonstrates the execution issue we need to fix.");
        } else if (firedWithBiLinear == firedWithoutBiLinear) {
            System.out.println("   ✅ BiLinear optimization works correctly!");
        } else {
            System.out.println("   ⚠️  Unexpected result pattern");
        }
        
        // Verify expected rule firings
        assertThat(firedWithoutBiLinear).as("Rules should work without BiLinear").isEqualTo(2);
        assertThat(rulesFiredWithoutBiLinear).as("Should contain both rules without BiLinear").containsExactlyInAnyOrder(
            "SimpleRule1", "SimpleRule2"
        );
        
        // Currently expect BiLinear to work too (this was working in our earlier test)
        assertThat(firedWithBiLinear).as("Rules should also work with BiLinear").isEqualTo(2);
        assertThat(rulesFiredWithBiLinear).as("Should contain both rules with BiLinear").containsExactlyInAnyOrder(
            "SimpleRule1", "SimpleRule2"
        );
        
        System.out.println("\n✅ BiLinear execution test completed");
    }

    @Test
    public void testSimplestDecisionTableBiLinearBug() {
        System.out.println("\n🔥 Simplest Decision Table BiLinear Bug Reproduction");
        System.out.println("===================================================");
        
        // This is the equivalent DRL that a 2-row decision table would generate
        // CORRECT: Rules should share the TAIL pattern (B), not the head pattern (A)
        // Rule1: Driver(age=25) -> Policy(type=COMPREHENSIVE) [tail: Policy pattern]
        // Rule2: Driver(age=30) -> Policy(type=COMPREHENSIVE) [tail: Policy pattern - SHARED!]
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"Insurance_Row1\"\n" +
            "when\n" +
            "    $a : A(object == 25)\n" +               // Different head pattern (Driver age=25)
            "    $b : B(object > 50, object < 100)\n" +  // SHARED TAIL pattern (Policy constraints)
            "then\n" +
            "    System.out.println(\"Insurance_Row1 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
            "end\n" +
            "\n" +
            "rule \"Insurance_Row2\"\n" +
            "when\n" +
            "    $a : A(object == 30)\n" +               // Different head pattern (Driver age=30)
            "    $b : B(object > 50, object < 100)\n" +  // SAME TAIL pattern (Policy constraints)
            "then\n" +
            "    System.out.println(\"Insurance_Row2 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
            "end\n";

        // Test with BiLinear ENABLED (should reproduce the bug)
        System.setProperty("drools.bilinear.enabled", "true");
        System.out.println("🔍 Testing decision table pattern with BiLinear ENABLED...");

        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        KieBase kieBase = kieHelper.build();
        
        KieSession session = kieBase.newKieSession();
        
        // Set up agenda listener to capture rule firings
        List<String> rulesFired = new ArrayList<>();
        session.addEventListener(new org.kie.api.event.rule.DefaultAgendaEventListener() {
            @Override
            public void afterMatchFired(org.kie.api.event.rule.AfterMatchFiredEvent event) {
                String ruleName = event.getMatch().getRule().getName();
                rulesFired.add(ruleName);
                System.out.println("🎯 Agenda Listener: " + ruleName + " fired");
            }
        });

        // Dump the Rete network structure to see if BiLinear nodes are created
        System.out.println("\n🔍 NETWORK STRUCTURE:");
        System.out.println("====================");
        ReteDumper.dumpRete(kieBase);
        System.out.println("====================\n");

        // Insert facts that match both rules (corrected for tail sharing)
        session.insert(new A(25));   // A.object = 25 (matches Rule1) ✓
        session.insert(new A(30));   // A.object = 30 (matches Rule2) ✓
        session.insert(new B(75));   // B.object = 75 (50 < 75 < 100, matches BOTH rules via shared tail) ✓
        
        System.out.println("📊 Before firing: Inserted A(25), A(30), B(75)");
        
        int fired = session.fireAllRules();
        session.dispose();
        
        System.out.println("\n📊 After firing:");
        System.out.println("   Total rules fired: " + fired);
        System.out.println("   Rules captured by listener: " + rulesFired.size());
        System.out.println("   Rule firings details:");
        for (int i = 0; i < rulesFired.size(); i++) {
            System.out.println("     " + (i + 1) + ". " + rulesFired.get(i));
        }
        
        // Count specific rule firings
        long row1Count = rulesFired.stream().filter(s -> s.equals("Insurance_Row1")).count();
        long row2Count = rulesFired.stream().filter(s -> s.equals("Insurance_Row2")).count();
        
        System.out.println("\n📊 Rule firing breakdown:");
        System.out.println("   Insurance_Row1 fired: " + row1Count + " times");
        System.out.println("   Insurance_Row2 fired: " + row2Count + " times");
        System.out.println("\n✅ Expected behavior:");
        System.out.println("   Insurance_Row1: 1 time");
        System.out.println("   Insurance_Row2: 1 time");
        System.out.println("   Total: 2 firings");
        
        if (fired != 2) {
            System.out.println("\n❌ BUG DETECTED: BiLinear optimization causing incorrect rule execution!");
            System.out.println("   Issue: Rules firing " + fired + " times instead of 2");
            if (row1Count > 1 || row2Count > 1) {
                System.out.println("   Root cause: Duplicate rule firings due to BiLinear memory sharing");
            }
        }

        // Test with BiLinear DISABLED to verify identical behavior
        System.setProperty("drools.bilinear.enabled", "false");
        System.out.println("\n🔍 Testing with BiLinear DISABLED for comparison...");
        
        KieHelper kieHelperOff = new KieHelper();
        kieHelperOff.addContent(drl, ResourceType.DRL);
        KieBase kieBaseOff = kieHelperOff.build();
        
        KieSession sessionOff = kieBaseOff.newKieSession();
        
        List<String> rulesFiredOff = new ArrayList<>();
        sessionOff.addEventListener(new org.kie.api.event.rule.DefaultAgendaEventListener() {
            @Override
            public void afterMatchFired(org.kie.api.event.rule.AfterMatchFiredEvent event) {
                String ruleName = event.getMatch().getRule().getName();
                rulesFiredOff.add(ruleName);
                System.out.println("🎯 BiLinear OFF - Agenda Listener: " + ruleName + " fired");
            }
        });
        
        sessionOff.insert(new A(25));
        sessionOff.insert(new A(30));
        sessionOff.insert(new B(75));
        
        int firedOff = sessionOff.fireAllRules();
        sessionOff.dispose();
        
        // Count rule firings for OFF case
        long row1CountOff = rulesFiredOff.stream().filter(s -> s.equals("Insurance_Row1")).count();
        long row2CountOff = rulesFiredOff.stream().filter(s -> s.equals("Insurance_Row2")).count();
        
        System.out.println("\n📊 BiLinear OFF Results:");
        System.out.println("   Total rules fired: " + firedOff);
        System.out.println("   Rules captured by listener: " + rulesFiredOff.size());
        for (int i = 0; i < rulesFiredOff.size(); i++) {
            System.out.println("     " + (i + 1) + ". " + rulesFiredOff.get(i));
        }
        System.out.println("   Insurance_Row1 fired: " + row1CountOff + " times");
        System.out.println("   Insurance_Row2 fired: " + row2CountOff + " times");
        
        // Reset BiLinear to enabled
        System.setProperty("drools.bilinear.enabled", "true");
        
        // CRITICAL: BiLinear optimization should be transparent - results must be identical
        System.out.println("\n🔍 Comparing BiLinear ON vs OFF:");
        System.out.println("   BiLinear ON:  " + fired + " rules fired (Row1: " + row1Count + ", Row2: " + row2Count + ")");
        System.out.println("   BiLinear OFF: " + firedOff + " rules fired (Row1: " + row1CountOff + ", Row2: " + row2CountOff + ")");
        
        if (fired != firedOff) {
            System.out.println("   ❌ CRITICAL BUG: BiLinear optimization changes rule execution behavior!");
            System.out.println("   ❌ BiLinear should be transparent and not affect rule firing counts!");
        } else {
            System.out.println("   ✅ BiLinear optimization is transparent - identical results!");
        }
        
        // CRITICAL: Verify BiLinear optimization is completely transparent
        assertThat(fired).as("BiLinear ON should fire same number of rules as BiLinear OFF").isEqualTo(firedOff);
        assertThat(row1Count).as("Insurance_Row1 should fire same number of times with BiLinear ON/OFF").isEqualTo(row1CountOff);
        assertThat(row2Count).as("Insurance_Row2 should fire same number of times with BiLinear ON/OFF").isEqualTo(row2CountOff);
        assertThat(rulesFired).as("BiLinear ON should fire same rules as BiLinear OFF").containsExactlyInAnyOrderElementsOf(rulesFiredOff);
        
        // Verify expected rule firings (should be 2 regardless of BiLinear setting)
        assertThat(fired).as("Total rules fired should be 2").isEqualTo(2);
        assertThat(row1Count).as("Insurance_Row1 should fire exactly once").isEqualTo(1);
        assertThat(row2Count).as("Insurance_Row2 should fire exactly once").isEqualTo(1);
        assertThat(rulesFired).as("Should contain exactly the expected rule firings").containsExactlyInAnyOrder(
            "Insurance_Row1",
            "Insurance_Row2"
        );
    }
}
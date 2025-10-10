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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple tests to isolate BiLinear rule execution bugs without decision table complexity.
 * These tests replicate the issues seen in ExternalSpreadsheetCompilerTest.
 */
public class BiLinearRuleExecutionBugTest {
    
    @BeforeEach
    public void setUp() {
        System.setProperty("drools.bilinear.enabled", "true");
        System.out.println("\n🧪 BiLinear Bug Test Setup - BiLinear ENABLED");
    }
    
    @AfterEach
    public void tearDown() {
        System.setProperty("drools.bilinear.enabled", "true"); // Reset to default
    }
    
    private KieSession createSession(String drl) {
        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        KieBase kieBase = kieHelper.build();
        return kieBase.newKieSession();
    }
    
    /**
     * Test 1: Multiple Rules with Shared Patterns (Replicates testIntegration issue)
     * Problem: Rules firing multiple times when they share patterns via BiLinear optimization
     */
    @Test
    public void testBiLinearDuplicateRuleFiring() {
        System.out.println("\n🔥 Test 1: BiLinear Duplicate Rule Firing");
        System.out.println("==========================================");
        
        String drl = 
            "import " + A.class.getCanonicalName() + ";\n" +
            "import " + B.class.getCanonicalName() + ";\n" +
            "global java.util.List list;\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    A(object == 42)\n" +
            "    B(object > 30)\n" +  // This should match B(35)
            "then\n" +
            "    list.add(\"Rule1 fired\");\n" +
            "    System.out.println(\"Rule1 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" + 
            "when\n" +
            "    A(object == 42)\n" +  // Same A pattern - should trigger BiLinear sharing
            "    B(object > 20)\n" +   // This should match B(35) too
            "then\n" +
            "    list.add(\"Rule2 fired\");\n" +
            "    System.out.println(\"Rule2 fired\");\n" +
            "end\n";
        
        KieSession session = createSession(drl);
        List<String> results = new ArrayList<>();
        session.setGlobal("list", results);
        
        // Insert facts that should match both rules
        session.insert(new A(42));  // Matches both A(object == 42) patterns  
        session.insert(new B(35));  // Matches both B(object > 30) and B(object > 20)
        
        System.out.println("📊 Inserted: A(42), B(35)");
        System.out.println("📊 Expected: Both Rule1 and Rule2 should fire exactly once each");
        
        int fireCount = session.fireAllRules();
        session.dispose();
        
        System.out.println("📊 Total rules fired: " + fireCount);
        System.out.println("📊 Results list: " + results);
        System.out.println("📊 Results count: " + results.size());
        
        // Expected behavior: Both rules should fire exactly once
        assertThat(fireCount).as("Both rules should fire exactly once").isEqualTo(2);
        assertThat(results).as("Should have exactly 2 entries").hasSize(2);
        assertThat(results).as("Should contain both rule firings").containsExactlyInAnyOrder("Rule1 fired", "Rule2 fired");
    }
    
    /**
     * Test 2: Shared Pattern with Variable Binding (Replicates variable context issues)  
     */
    @Test
    public void testBiLinearVariableBinding() {
        System.out.println("\n🔥 Test 2: BiLinear Variable Binding");
        System.out.println("====================================");
        
        String drl =
            "import " + A.class.getCanonicalName() + ";\n" +
            "import " + B.class.getCanonicalName() + ";\n" +
            "global java.util.List list;\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    $a: A(object == 42)\n" +
            "    $b: B(object > 30)\n" +
            "then\n" +
            "    list.add(\"Rule1: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
            "    System.out.println(\"Rule1: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    $a: A(object == 25)\n" +  // Different A constraint
            "    $b: B(object > 30)\n" +   // Same B pattern - triggers BiLinear sharing
            "then\n" +
            "    list.add(\"Rule2: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
            "    System.out.println(\"Rule2: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
            "end\n";
        
        KieSession session = createSession(drl);
        List<String> results = new ArrayList<>();
        session.setGlobal("list", results);
        
        // Insert facts: A(42) should only match Rule1, A(25) should only match Rule2
        session.insert(new A(42));
        session.insert(new A(25)); 
        session.insert(new B(35));  // Should match both rules' B constraint
        
        System.out.println("📊 Inserted: A(42), A(25), B(35)");
        System.out.println("📊 Expected: Both rules fire with correct variable bindings");
        
        int fireCount = session.fireAllRules();
        session.dispose();
        
        System.out.println("📊 Total rules fired: " + fireCount);
        System.out.println("📊 Results list: " + results);
        
        // Expected: Both rules fire with correct variable bindings
        assertThat(fireCount).as("Both rules should fire").isEqualTo(2);
        assertThat(results).as("Should have correct variable bindings").containsExactlyInAnyOrder(
            "Rule1: A=42, B=35",
            "Rule2: A=25, B=35"
        );
    }
    
    /**
     * Test 3: Object Modification in RHS (Replicates testPricing issue)
     */
    @Test  
    public void testBiLinearRhsObjectModification() {
        System.out.println("\n🔥 Test 3: BiLinear RHS Object Modification");
        System.out.println("===========================================");
        
        String drl =
            "import " + A.class.getCanonicalName() + ";\n" +
            "import " + B.class.getCanonicalName() + ";\n" +
            "\n" +
            "rule \"SetValueRule1\"\n" +
            "when\n" +
            "    A(object >= 40)\n" +
            "    $b: B(object == 10)\n" +  // Specific B value
            "then\n" +
            "    $b.setObject(120);\n" +   // Should set B.object to 120
            "    System.out.println(\"SetValueRule1: Set B to 120\");\n" +
            "end\n" +
            "\n" +
            "rule \"SetValueRule2\"\n" +
            "when\n" +
            "    A(object >= 20)\n" +    // Different A constraint  
            "    $b: B(object == 5)\n" +  // Different B constraint but same pattern structure
            "then\n" +
            "    $b.setObject(80);\n" +   // Should set B.object to 80
            "    System.out.println(\"SetValueRule2: Set B to 80\");\n" +
            "end\n";
        
        KieSession session = createSession(drl);
        
        B targetB1 = new B(10);  // Should be modified by Rule1
        B targetB2 = new B(5);   // Should be modified by Rule2
        
        session.insert(new A(45));  // Matches both A constraints
        session.insert(targetB1);
        session.insert(targetB2);
        
        System.out.println("📊 Inserted: A(45), B(10), B(5)");
        System.out.println("📊 Expected: B(10) → 120, B(5) → 80");
        System.out.println("📊 Initial: B1.object=" + targetB1.getObject() + ", B2.object=" + targetB2.getObject());
        
        int fireCount = session.fireAllRules();
        session.dispose();
        
        System.out.println("📊 Total rules fired: " + fireCount);
        System.out.println("📊 Final: B1.object=" + targetB1.getObject() + ", B2.object=" + targetB2.getObject());
        
        // Expected: Both rules fire and correctly modify their target objects
        assertThat(fireCount).as("Both rules should fire").isEqualTo(2);
        assertThat(targetB1.getObject()).as("B1 should be set to 120 by Rule1").isEqualTo(120);
        assertThat(targetB2.getObject()).as("B2 should be set to 80 by Rule2").isEqualTo(80);
    }
    
    /**
     * Test 4: Cross-Rule Interference Test
     * Tests if shared BiLinear nodes cause rules to interfere with each other
     */
    @Test
    public void testBiLinearCrossRuleInterference() {
        System.out.println("\n🔥 Test 4: BiLinear Cross-Rule Interference");
        System.out.println("===========================================");
        
        String drl =
            "import " + A.class.getCanonicalName() + ";\n" +
            "import " + B.class.getCanonicalName() + ";\n" +
            "global java.util.List list;\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    A(object > 40)\n" +     // Should match A(50) and A(60)
            "    B(object == 100)\n" +  // Should match B(100)
            "then\n" +
            "    list.add(\"Rule1\");\n" +
            "    System.out.println(\"Rule1 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    A(object > 40)\n" +     // Same A pattern - BiLinear sharing
            "    B(object == 200)\n" +  // Should match B(200) 
            "then\n" +
            "    list.add(\"Rule2\");\n" +
            "    System.out.println(\"Rule2 fired\");\n" +
            "end\n";
        
        KieSession session = createSession(drl);
        List<String> results = new ArrayList<>();
        session.setGlobal("list", results);
        
        // Insert facts where each rule should fire exactly once per matching combination
        session.insert(new A(50));   // Matches A(object > 40) for both rules
        session.insert(new A(60));   // Matches A(object > 40) for both rules  
        session.insert(new B(100));  // Only matches Rule1
        session.insert(new B(200));  // Only matches Rule2
        
        System.out.println("📊 Inserted: A(50), A(60), B(100), B(200)");
        System.out.println("📊 Expected: Rule1 fires 2 times (A50+B100, A60+B100), Rule2 fires 2 times (A50+B200, A60+B200)");
        
        int fireCount = session.fireAllRules();
        session.dispose();
        
        System.out.println("📊 Total rules fired: " + fireCount);
        System.out.println("📊 Results list: " + results);
        
        // Count occurrences
        long rule1Count = results.stream().filter(s -> s.equals("Rule1")).count();
        long rule2Count = results.stream().filter(s -> s.equals("Rule2")).count();
        
        System.out.println("📊 Rule1 fired: " + rule1Count + " times");
        System.out.println("📊 Rule2 fired: " + rule2Count + " times");
        
        // Expected: Each rule should fire exactly 2 times (2 A's × 1 matching B each)
        assertThat(fireCount).as("Total firings should be 4").isEqualTo(4);
        assertThat(rule1Count).as("Rule1 should fire 2 times").isEqualTo(2);
        assertThat(rule2Count).as("Rule2 should fire 2 times").isEqualTo(2);
    }
    
    /**
     * Comparison test: Run the same rules with BiLinear disabled to verify expected behavior
     */
    @Test
    public void testExpectedBehaviorWithoutBiLinear() {
        System.out.println("\n🔥 Control Test: Expected Behavior WITHOUT BiLinear");
        System.out.println("==================================================");
        
        // Temporarily disable BiLinear
        System.setProperty("drools.bilinear.enabled", "false");
        
        try {
            // Run the same test as testBiLinearDuplicateRuleFiring but with BiLinear disabled
            String drl = 
                "import " + A.class.getCanonicalName() + ";\n" +
                "import " + B.class.getCanonicalName() + ";\n" +
                "global java.util.List list;\n" +
                "\n" +
                "rule \"Rule1\"\n" +
                "when\n" +
                "    A(object == 42)\n" +
                "    B(object > 30)\n" +
                "then\n" +
                "    list.add(\"Rule1 fired\");\n" +
                "end\n" +
                "\n" +
                "rule \"Rule2\"\n" + 
                "when\n" +
                "    A(object == 42)\n" +
                "    B(object > 20)\n" +
                "then\n" +
                "    list.add(\"Rule2 fired\");\n" +
                "end\n";
            
            KieSession session = createSession(drl);
            List<String> results = new ArrayList<>();
            session.setGlobal("list", results);
            
            session.insert(new A(42));
            session.insert(new B(35));
            
            int fireCount = session.fireAllRules();
            session.dispose();
            
            System.out.println("📊 WITHOUT BiLinear - Total rules fired: " + fireCount);
            System.out.println("📊 WITHOUT BiLinear - Results: " + results);
            
            // This should be the expected behavior
            assertThat(fireCount).as("Without BiLinear: Both rules should fire").isEqualTo(2);
            assertThat(results).as("Without BiLinear: Should have both rule firings").containsExactlyInAnyOrder("Rule1 fired", "Rule2 fired");
            
        } finally {
            // Restore BiLinear enabled state
            System.setProperty("drools.bilinear.enabled", "true");
        }
    }
}
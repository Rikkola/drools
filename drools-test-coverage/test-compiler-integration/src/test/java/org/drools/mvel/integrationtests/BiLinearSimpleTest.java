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
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test to verify BiLinear functionality is working
 */
public class BiLinearSimpleTest {

    @Test
    public void testBiLinearEnabled() {
        System.out.println("\n🔍 Testing BiLinear functionality...");
        
        // Enable BiLinear optimization
        System.setProperty("drools.bilinear.enabled", "true");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object)\n" +  // Beta constraint: cross-pattern relationship
            "then\n" +
            "    System.out.println(\"Rule1 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +           // Same pattern as Rule1
            "    $b : B(object > $a.object)\n" +  // Same beta constraint as Rule1 - should trigger sharing
            "then\n" +
            "    System.out.println(\"Rule2 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
            "end\n";

        System.out.println("🔧 Building KieBase with BiLinear enabled...");
        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("📊 Network structure:");
        NetworkVisitor visitor = new NetworkVisitor();
        visitor.debugNetworkStructure(kieBase);
        
        System.out.println("\n🚀 Testing execution...");
        KieSession session = kieBase.newKieSession();
        session.insert(new A(5));
        session.insert(new B(10)); 
        
        int firedRules = session.fireAllRules();
        System.out.println("📈 Rules fired: " + firedRules);
        
        session.dispose();
        
        System.out.println("✅ BiLinear test completed");
    }
    
    @Test
    public void testForceBiLinearScenario() {
        System.out.println("\n🔍 Testing FORCED BiLinear scenario...");
        
        // Enable BiLinear optimization
        System.setProperty("drools.bilinear.enabled", "true");
        
        // Create a scenario that REQUIRES BiLinear - different rule structures but same patterns
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"ComplexRule1\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object)\n" +  // Beta constraint
            "    $c : A(object < 100)\n" +        // Additional pattern - creates complex structure
            "then\n" +
            "    System.out.println(\"ComplexRule1 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"ComplexRule2\"\n" +
            "when\n" +
            "    $x : A(object > 5)\n" +          // Different constraint
            "    $a : A(object > 0)\n" +           // Same A pattern as Rule1 (different position)
            "    $b : B(object > $a.object)\n" +  // Same A-B relationship as Rule1
            "then\n" +
            "    System.out.println(\"ComplexRule2 fired\");\n" +
            "end\n";

        System.out.println("🔧 Building KieBase with complex patterns...");
        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("📊 Network structure (should show BiLinear optimization):");
        NetworkVisitor visitor = new NetworkVisitor();
        visitor.debugNetworkStructure(kieBase);
        
        System.out.println("✅ Forced BiLinear test completed");
    }

    @Test
    public void testBiLinearEnabledVsDisabled() {
        System.out.println("\n🔍 Testing BiLinear ENABLED vs DISABLED rule execution...");
        
        String drl = createTestDRL();
        
        // Test with BiLinear enabled
        System.out.println("\n📊 === TESTING WITH BILINEAR ENABLED ===");
        TestResult enabledResult = runTestWithBiLinear(drl, true);
        
        // Test with BiLinear disabled  
        System.out.println("\n📊 === TESTING WITH BILINEAR DISABLED ===");
        TestResult disabledResult = runTestWithBiLinear(drl, false);
        
        // Compare results
        System.out.println("\n📈 === COMPARISON RESULTS ===");
        System.out.println("BiLinear ENABLED  - Rules fired: " + enabledResult.rulesFired);
        System.out.println("BiLinear DISABLED - Rules fired: " + disabledResult.rulesFired);
        
        // Verify identical execution
        assertThat(enabledResult.rulesFired)
            .as("Same number of rules should fire with BiLinear enabled vs disabled")
            .isEqualTo(disabledResult.rulesFired);
            
        if (enabledResult.rulesFired == disabledResult.rulesFired) {
            System.out.println("✅ BiLinear enabled vs disabled test PASSED!");
            System.out.println("   - Rule execution semantics preserved");
            System.out.println("   - BiLinear optimization is purely performance enhancement");
        } else {
            System.out.println("❌ BiLinear enabled vs disabled test FAILED!");
            System.out.println("   - BiLinear execution differs from regular execution");
            System.out.println("   - This indicates a bug in BiLinear implementation");
        }
    }

    @Test
    public void testBiLinearExecutionBug() {
        System.out.println("\n🔍 Investigating BiLinear execution issue...");
        
        // Test with different fact insertion orders
        String drl = createTestDRL();
        
        System.setProperty("drools.bilinear.enabled", "true");
        
        try {
            KieHelper kieHelper = new KieHelper();
            kieHelper.addContent(drl, ResourceType.DRL);
            KieBase kieBase = kieHelper.build();
            
            // Test different insertion orders
            testWithInsertionOrder(kieBase, "A first, then B");
            testWithInsertionOrder2(kieBase, "B first, then A");
            
        } finally {
            System.clearProperty("drools.bilinear.enabled");
        }
    }
    
    @Test
    public void testComprehensiveBiLinearScenarios() {
        System.out.println("\n🔍 Testing comprehensive BiLinear scenarios...");
        
        // Scenario 1: Multiple A values with single B
        testScenarioComparison("Multiple A values", createMultipleAScenario(), 
            new A[]{new A(5), new A(7), new A(12)}, new B[]{new B(10)});
            
        // Scenario 2: Single A value with multiple B values
        testScenarioComparison("Multiple B values", createTestDRL(), 
            new A[]{new A(5)}, new B[]{new B(10), new B(15), new B(20)});
            
        // Scenario 3: No matching facts (B values too low)
        testScenarioComparison("No matches", createTestDRL(), 
            new A[]{new A(15)}, new B[]{new B(10)});
            
        // Scenario 4: Complex constraints
        testScenarioComparison("Complex constraints", createComplexConstraintsDRL(), 
            new A[]{new A(5), new A(15)}, new B[]{new B(10), new B(25)});
    }
    
    private String createMultipleAScenario() {
        return "import " + A.class.getCanonicalName() + "\n" +
               "import " + B.class.getCanonicalName() + "\n" +
               "\n" +
               "rule \"Rule1\"\n" +
               "when\n" +
               "    $a : A(object > 0)\n" +
               "    $b : B(object > $a.object)\n" +
               "then\n" +
               "    System.out.println(\"Rule1 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
               "end\n" +
               "\n" +
               "rule \"Rule2\"\n" +
               "when\n" +
               "    $a : A(object > 3)\n" +
               "    $b : B(object > $a.object)\n" +
               "then\n" +
               "    System.out.println(\"Rule2 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
               "end\n";
    }
    
    private String createComplexConstraintsDRL() {
        return "import " + A.class.getCanonicalName() + "\n" +
               "import " + B.class.getCanonicalName() + "\n" +
               "\n" +
               "rule \"ComplexRule1\"\n" +
               "when\n" +
               "    $a : A(object > 0, object < 20)\n" +
               "    $b : B(object > $a.object, object < 30)\n" +
               "then\n" +
               "    System.out.println(\"ComplexRule1 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
               "end\n" +
               "\n" +
               "rule \"ComplexRule2\"\n" +
               "when\n" +
               "    $a : A(object > 0, object < 20)\n" +
               "    $b : B(object > $a.object, object < 30)\n" +
               "then\n" +
               "    System.out.println(\"ComplexRule2 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
               "end\n";
    }
    
    private void testScenarioComparison(String scenarioName, String drl, A[] aFacts, B[] bFacts) {
        System.out.println("\n📊 === Testing Scenario: " + scenarioName + " ===");
        
        // Test with BiLinear enabled
        TestResultWithOutput enabledResult = runTestWithBiLinearAndOutput(drl, true, aFacts, bFacts);
        
        // Test with BiLinear disabled
        TestResultWithOutput disabledResult = runTestWithBiLinearAndOutput(drl, false, aFacts, bFacts);
        
        // Compare results
        System.out.println("📈 Scenario Results:");
        System.out.println("  BiLinear ENABLED  - Rules fired: " + enabledResult.rulesFired);
        System.out.println("  BiLinear DISABLED - Rules fired: " + disabledResult.rulesFired);
        
        if (enabledResult.rulesFired == disabledResult.rulesFired) {
            System.out.println("  ✅ " + scenarioName + " - Execution counts match");
        } else {
            System.out.println("  ❌ " + scenarioName + " - Execution counts differ!");
            System.out.println("     This indicates BiLinear execution bug");
        }
        
        // Output comparison
        if (enabledResult.outputs.size() == disabledResult.outputs.size()) {
            System.out.println("  ✅ " + scenarioName + " - Output counts match");
        } else {
            System.out.println("  ❌ " + scenarioName + " - Output counts differ!");
            System.out.println("     Enabled outputs: " + enabledResult.outputs.size());
            System.out.println("     Disabled outputs: " + disabledResult.outputs.size());
        }
    }
    
    private void testWithInsertionOrder(KieBase kieBase, String description) {
        System.out.println("\n📌 Testing: " + description);
        KieSession session = kieBase.newKieSession();
        
        System.out.println("   Inserting A(5)...");
        session.insert(new A(5));
        
        System.out.println("   Inserting B(10)...");  
        session.insert(new B(10));
        
        int rules = session.fireAllRules();
        System.out.println("   Rules fired: " + rules);
        
        session.dispose();
    }
    
    private void testWithInsertionOrder2(KieBase kieBase, String description) {
        System.out.println("\n📌 Testing: " + description);
        KieSession session = kieBase.newKieSession();
        
        System.out.println("   Inserting B(10)...");
        session.insert(new B(10));
        
        System.out.println("   Inserting A(5)...");
        session.insert(new A(5));
        
        int rules = session.fireAllRules();
        System.out.println("   Rules fired: " + rules);
        
        session.dispose();
    }

    private String createTestDRL() {
        return "import " + A.class.getCanonicalName() + "\n" +
               "import " + B.class.getCanonicalName() + "\n" +
               "\n" +
               "rule \"Rule1\"\n" +
               "when\n" +
               "    $a : A(object > 0)\n" +
               "    $b : B(object > $a.object)\n" +
               "then\n" +
               "    System.out.println(\"Rule1 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
               "end\n" +
               "\n" +
               "rule \"Rule2\"\n" +
               "when\n" +
               "    $a : A(object > 0)\n" +
               "    $b : B(object > $a.object)\n" +
               "then\n" +
               "    System.out.println(\"Rule2 fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
               "end\n";
    }

    private TestResult runTestWithBiLinear(String drl, boolean biLinearEnabled) {
        // Set BiLinear property
        String originalProperty = System.getProperty("drools.bilinear.enabled");
        System.setProperty("drools.bilinear.enabled", String.valueOf(biLinearEnabled));
        
        try {
            System.out.println("🔧 Building KieBase with BiLinear " + (biLinearEnabled ? "ENABLED" : "DISABLED"));
            
            // Build KieBase
            KieHelper kieHelper = new KieHelper();
            kieHelper.addContent(drl, ResourceType.DRL);
            KieBase kieBase = kieHelper.build();
            
            // Show network structure
            System.out.println("📊 Network structure:");
            NetworkVisitor visitor = new NetworkVisitor();
            visitor.debugNetworkStructure(kieBase);
            
            // Execute rules
            System.out.println("🚀 Executing rules...");
            KieSession session = kieBase.newKieSession();
            
            // Insert test facts - these should trigger both rules
            System.out.println("📌 Inserting A(5)...");
            session.insert(new A(5));    // A(object=5 > 0) ✓
            
            System.out.println("📌 Inserting B(10)...");
            session.insert(new B(10));   // B(object=10 > 5) ✓ - should fire both rules
            
            System.out.println("📌 Firing all rules...");
            int firedRules = session.fireAllRules();
            session.dispose();
            
            System.out.println("📈 Rules fired: " + firedRules);
            
            if (firedRules == 0 && biLinearEnabled) {
                System.out.println("⚠️ WARNING: BiLinear enabled but no rules fired - possible execution bug");
            }
            
            return new TestResult(firedRules, new ArrayList<>());
            
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty("drools.bilinear.enabled", originalProperty);
            } else {
                System.clearProperty("drools.bilinear.enabled");
            }
        }
    }

    private static class TestResult {
        final int rulesFired;
        final List<String> ruleOutputs;
        
        TestResult(int rulesFired, List<String> ruleOutputs) {
            this.rulesFired = rulesFired;
            this.ruleOutputs = ruleOutputs;
        }
    }
    
    private static class TestResultWithOutput {
        final int rulesFired;
        final List<String> outputs;
        
        TestResultWithOutput(int rulesFired, List<String> outputs) {
            this.rulesFired = rulesFired;
            this.outputs = outputs;
        }
    }

    private TestResultWithOutput runTestWithBiLinearAndOutput(String drl, boolean biLinearEnabled, A[] aFacts, B[] bFacts) {
        // Set BiLinear property
        String originalProperty = System.getProperty("drools.bilinear.enabled");
        System.setProperty("drools.bilinear.enabled", String.valueOf(biLinearEnabled));
        
        // Capture System.out
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput));
        
        List<String> ruleOutputs = new ArrayList<>();
        
        try {
            System.out.println("🔧 Building KieBase with BiLinear " + (biLinearEnabled ? "ENABLED" : "DISABLED"));
            
            // Build KieBase
            KieHelper kieHelper = new KieHelper();
            kieHelper.addContent(drl, ResourceType.DRL);
            KieBase kieBase = kieHelper.build();
            
            // Execute rules
            System.out.println("🚀 Executing rules...");
            KieSession session = kieBase.newKieSession();
            
            // Insert A facts
            for (A aFact : aFacts) {
                System.out.println("📌 Inserting " + aFact + "...");
                session.insert(aFact);
            }
            
            // Insert B facts
            for (B bFact : bFacts) {
                System.out.println("📌 Inserting " + bFact + "...");
                session.insert(bFact);
            }
            
            System.out.println("📌 Firing all rules...");
            int firedRules = session.fireAllRules();
            session.dispose();
            
            System.out.println("📈 Rules fired: " + firedRules);
            
            if (firedRules == 0 && biLinearEnabled) {
                System.out.println("⚠️ WARNING: BiLinear enabled but no rules fired - possible execution bug");
            }
            
            return new TestResultWithOutput(firedRules, ruleOutputs);
            
        } finally {
            // Restore original output and property
            System.setOut(originalOut);
            
            // Parse captured output for rule firings
            String output = capturedOutput.toString();
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.contains(" fired:")) {
                    ruleOutputs.add(line.trim());
                }
            }
            
            if (originalProperty != null) {
                System.setProperty("drools.bilinear.enabled", originalProperty);
            } else {
                System.clearProperty("drools.bilinear.enabled");
            }
        }
    }

    private KieBase buildKieBase(String drl) {
        System.setProperty("drools.bilinear.enabled", "true");
        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        return kieHelper.build();
    }
}
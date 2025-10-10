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
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

/**
 * Minimal test to isolate the BiLinear memory issue: PathMemory vs BetaMemory ClassCastException.
 * This test strips away all complexity to focus on the core memory creation problem.
 */
public class BiLinearMemoryIssueTest {
    
    // BiLinear optimization toggle - controlled by system property
    private static final String BILINEAR_ENABLED_PROPERTY = "drools.bilinear.enabled";
    
    // Helper to run tests with BiLinear enabled/disabled
    private void runWithBiLinearToggle(String testName, String drl, boolean enabled) {
        String originalValue = System.getProperty(BILINEAR_ENABLED_PROPERTY);
        try {
            System.setProperty(BILINEAR_ENABLED_PROPERTY, String.valueOf(enabled));
            String mode = enabled ? "ENABLED" : "DISABLED";
            System.out.println("\n=== " + testName + " - BiLinear " + mode + " ===");
            testDrlPattern(testName + "_BiLinear" + mode, drl);
        } finally {
            if (originalValue != null) {
                System.setProperty(BILINEAR_ENABLED_PROPERTY, originalValue);
            } else {
                System.clearProperty(BILINEAR_ENABLED_PROPERTY);
            }
        }
    }

    public static class Person {
        private String name;
        private int age;
        
        public Person() {}
        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
    
    public static class Item {
        private String type;
        private int value;
        
        public Item() {}
        public Item(String type, int value) {
            this.type = type;
            this.value = value;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }

    @Test
    public void testMinimalBiLinearMemoryIssue() {
        System.out.println("\n=== BiLinear Memory Issue Test ===");
        
        // Minimal DRL that should trigger BiLinear optimization
        String drl = 
            "package org.drools.test;\n" +
            "import " + Person.class.getCanonicalName() + ";\n" +
            "import " + Item.class.getCanonicalName() + ";\n" +
            "\n" +
            "rule \"SimpleRule\"\n" +
            "when\n" +
            "    $p : Person(age > 20)\n" +
            "    $i : Item(value > 10)\n" +
            "then\n" +
            "    System.out.println(\"Match: \" + $p.getName() + \" + \" + $i.getType());\n" +
            "end\n";

        try {
            System.out.println("Creating KieBase...");
            KieBase kbase = new KieHelper().addContent(drl, ResourceType.DRL).build();
            
            System.out.println("Creating KieSession...");
            KieSession session = kbase.newKieSession();
            
            System.out.println("Inserting facts (this should trigger memory creation)...");
            session.insert(new Person("Alice", 25));
            session.insert(new Item("Widget", 15));
            
            System.out.println("Firing rules...");
            int fired = session.fireAllRules();
            
            session.dispose();
            System.out.println("✅ Test completed successfully. Rules fired: " + fired);
            
        } catch (Exception e) {
            System.out.println("❌ Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("BiLinear memory issue reproduced", e);
        }
    }

    @Test  
    public void testEvenSimplerPattern() {
        System.out.println("\n=== Even Simpler BiLinear Test ===");
        
        // Absolute minimal pattern
        String drl = 
            "package org.drools.test;\n" +
            "import " + Person.class.getCanonicalName() + ";\n" +
            "import " + Item.class.getCanonicalName() + ";\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    Person()\n" +
            "    Item()\n" +
            "then\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    Person()\n" +
            "    Item()\n" +
            "then\n" +
            "end\n";

        try {
            KieBase kbase = new KieHelper().addContent(drl, ResourceType.DRL).build();
            KieSession session = kbase.newKieSession();
            
            // Single fact insertion to trigger memory issue
            session.insert(new Person("Test", 1));
            
            session.dispose();
            System.out.println("✅ Simplified test passed");
            
        } catch (Exception e) {
            System.out.println("❌ Simplified test failed: " + e.getMessage());
            throw e;
        }
    }

    @Test
    public void testStep0_SingleConstraint() {
        System.out.println("\n=== Step 0: Single Constraint ===");
        
        String drl = 
            "package org.drools.test;\n" +
            "import " + Person.class.getCanonicalName() + ";\n" +
            "import " + Item.class.getCanonicalName() + ";\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    Person(age > 10)\n" +
            "    Item()\n" +
            "then\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    Person(age > 15)\n" +
            "    Item()\n" +
            "then\n" +
            "end\n";

        System.err.println("🚨 STARTING MEMORY TRACKING TEST");
        testDrlPattern("SingleConstraint", drl);
    }
    
    @Test  
    public void testComparisonBiLinearEnabledVsDisabled() {
        String drl = 
            "package org.drools.test;\n" +
            "import " + Person.class.getCanonicalName() + ";\n" +
            "import " + Item.class.getCanonicalName() + ";\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    Person(age > 10)\n" +
            "    Item()\n" +
            "then\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    Person(age > 15)\n" +
            "    Item()\n" +
            "then\n" +
            "end\n";
            
        // First test with BiLinear DISABLED - this should work
        runWithBiLinearToggle("ConstraintComparison", drl, false);
        
        // Then test with BiLinear ENABLED - this should fail with memory issue  
        runWithBiLinearToggle("ConstraintComparison", drl, true);
    }
    
    @Test
    public void testSimplePatternsWithBiLinearToggle() {
        String drl = 
            "package org.drools.test;\n" +
            "import " + Person.class.getCanonicalName() + ";\n" +
            "import " + Item.class.getCanonicalName() + ";\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    Person()\n" +
            "    Item()\n" +
            "then\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    Person()\n" +
            "    Item()\n" +
            "then\n" +
            "end\n";
            
        // Both modes should work for simple patterns (no constraints)
        runWithBiLinearToggle("SimplePatterns", drl, false);
        runWithBiLinearToggle("SimplePatterns", drl, true);
    }

    @Test
    public void testStep1_BasicConstraints() {
        System.out.println("\n=== Step 1: Basic Constraints ===");
        
        String drl = 
            "package org.drools.test;\n" +
            "import " + Person.class.getCanonicalName() + ";\n" +
            "import " + Item.class.getCanonicalName() + ";\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    Person(age > 10)\n" +
            "    Item(value > 5)\n" +
            "then\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    Person(age > 15)\n" +
            "    Item(value > 8)\n" +
            "then\n" +
            "end\n";

        testDrlPattern("BasicConstraints", drl);
    }

    @Test
    public void testStep2_MoreRules() {
        System.out.println("\n=== Step 2: More Rules ===");
        
        String drl = 
            "package org.drools.test;\n" +
            "import " + Person.class.getCanonicalName() + ";\n" +
            "import " + Item.class.getCanonicalName() + ";\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    Person(age > 10)\n" +
            "    Item(value > 5)\n" +
            "then\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    Person(age > 15)\n" +
            "    Item(value > 8)\n" +
            "then\n" +
            "end\n" +
            "\n" +
            "rule \"Rule3\"\n" +
            "when\n" +
            "    Person(age > 20)\n" +
            "    Item(value > 10)\n" +
            "then\n" +
            "end\n";

        testDrlPattern("MoreRules", drl);
    }

    @Test
    public void testStep3_SimilarPatterns() {
        System.out.println("\n=== Step 3: Very Similar Patterns (Like Original Test) ===");
        
        String drl = 
            "package org.drools.test;\n" +
            "import " + Person.class.getCanonicalName() + ";\n" +
            "import " + Item.class.getCanonicalName() + ";\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    $p : Person(age > 18)\n" +
            "    $i : Item(value > 10)\n" +
            "then\n" +
            "    System.out.println(\"Rule1: \" + $p.getName() + \" + \" + $i.getType());\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    $p : Person(age > 21)\n" +
            "    $i : Item(value > 15)\n" +
            "then\n" +
            "    System.out.println(\"Rule2: \" + $p.getName() + \" + \" + $i.getType());\n" +
            "end\n" +
            "\n" +
            "rule \"Rule3\"\n" +
            "when\n" +
            "    $p : Person(age > 25)\n" +
            "    $i : Item(value > 20)\n" +
            "then\n" +
            "    System.out.println(\"Rule3: \" + $p.getName() + \" + \" + $i.getType());\n" +
            "end\n";

        testDrlPattern("SimilarPatterns", drl);
    }

    @Test
    public void testStep4_ExactOriginalPattern() {
        System.out.println("\n=== Step 4: Exact Original Pattern (Person + Cheese) ===");
        
        // Use the exact same classes and constraints as the original failing test
        String drl = 
            "package org.drools.test;\n" +
            "import org.drools.mvel.integrationtests.BiLinearInfiniteLoopTest.Person;\n" +
            "import org.drools.mvel.integrationtests.BiLinearInfiniteLoopTest.Cheese;\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    $p : Person(age > 18)\n" +
            "    $c : Cheese(price > 10)\n" +
            "then\n" +
            "    System.out.println(\"Rule1: \" + $p.getName() + \" can buy \" + $c.getType());\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    $p : Person(age > 21)\n" +
            "    $c : Cheese(price > 15)\n" +
            "then\n" +
            "    System.out.println(\"Rule2: \" + $p.getName() + \" can buy premium \" + $c.getType());\n" +
            "end\n" +
            "\n" +
            "rule \"Rule3\"\n" +
            "when\n" +
            "    $p : Person(age > 25)\n" +
            "    $c : Cheese(price > 20)\n" +
            "then\n" +
            "    System.out.println(\"Rule3: \" + $p.getName() + \" can buy luxury \" + $c.getType());\n" +
            "end\n";

        testDrlPattern("ExactOriginal", drl);
    }

    @Test
    public void testStep5_ArrayConstraints() {
        System.out.println("\n=== Step 5: Array-style Constraints (Original Issue Trigger) ===");
        
        // This mimics the pattern from the original failing test that had "Person[]" and "Cheese[]" in debug output
        String drl = 
            "package org.drools.test;\n" +
            "import org.drools.mvel.integrationtests.BiLinearInfiniteLoopTest.Person;\n" +
            "import org.drools.mvel.integrationtests.BiLinearInfiniteLoopTest.Cheese;\n" +
            "\n" +
            "rule \"Rule1\"\n" +
            "when\n" +
            "    $p : Person(age > 18)\n" +
            "    $c : Cheese(price > 10)\n" +
            "then\n" +
            "    System.out.println(\"Rule1: \" + $p.getName() + \" can buy \" + $c.getType());\n" +
            "end\n" +
            "\n" +
            "rule \"Rule2\"\n" +
            "when\n" +
            "    $p : Person(age > 21)\n" +
            "    $c : Cheese(price > 15)\n" +
            "then\n" +
            "    System.out.println(\"Rule2: \" + $p.getName() + \" can buy premium \" + $c.getType());\n" +
            "end\n" +
            "\n" +
            "rule \"Rule3\"\n" +
            "when\n" +
            "    $p : Person(age > 25)\n" +
            "    $c : Cheese(price > 20)\n" +
            "then\n" +
            "    System.out.println(\"Rule3: \" + $p.getName() + \" can buy luxury \" + $c.getType());\n" +
            "end\n";

        testDrlPatternWithCheese("ArrayConstraints", drl);
    }

    // Helper method to test DRL patterns consistently
    private void testDrlPattern(String testName, String drl) {
        try {
            System.out.println("Creating KieBase for " + testName + "...");
            KieBase kbase = new KieHelper().addContent(drl, ResourceType.DRL).build();
            
            System.out.println("Creating KieSession...");
            KieSession session = kbase.newKieSession();
            
            System.out.println("Inserting facts...");
            session.insert(new Person("Alice", 30));
            session.insert(new Item("Widget", 25));
            
            System.out.println("Firing rules...");
            int fired = session.fireAllRules();
            
            session.dispose();
            System.out.println("✅ " + testName + " test passed. Rules fired: " + fired);
            
        } catch (Exception e) {
            System.out.println("❌ " + testName + " test failed: " + e.getMessage());
            if (e.getMessage().contains("PathMemory") && e.getMessage().contains("BetaMemory")) {
                System.out.println("*** MEMORY ISSUE TRIGGERED in " + testName + " ***");
            }
            throw new RuntimeException(testName + " memory issue reproduced", e);
        }
    }

    // Helper method to test with Cheese objects (from original failing test)
    private void testDrlPatternWithCheese(String testName, String drl) {
        try {
            System.out.println("Creating KieBase for " + testName + "...");
            KieBase kbase = new KieHelper().addContent(drl, ResourceType.DRL).build();
            
            System.out.println("Creating KieSession...");
            KieSession session = kbase.newKieSession();
            
            System.out.println("Inserting facts...");
            session.insert(new org.drools.mvel.integrationtests.BiLinearInfiniteLoopTest.Person("John", 30));
            session.insert(new org.drools.mvel.integrationtests.BiLinearInfiniteLoopTest.Cheese("Cheddar", 25));
            
            System.out.println("Firing rules...");
            int fired = session.fireAllRules();
            
            session.dispose();
            System.out.println("✅ " + testName + " test passed. Rules fired: " + fired);
            
        } catch (Exception e) {
            System.out.println("❌ " + testName + " test failed: " + e.getMessage());
            if (e.getMessage().contains("PathMemory") && e.getMessage().contains("BetaMemory")) {
                System.out.println("*** MEMORY ISSUE TRIGGERED in " + testName + " ***");
            }
            throw new RuntimeException(testName + " memory issue reproduced", e);
        }
    }
}
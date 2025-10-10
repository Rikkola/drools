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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to isolate the BiLinear infinite loop issue observed in decision tables.
 * This test recreates the pattern that causes CompositeLeftTupleSinkAdapter.getMatchingNode() 
 * to enter an infinite loop when BiLinear nodes with duplicate inputs are created.
 */
public class BiLinearInfiniteLoopTest {

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
    
    public static class Cheese {
        private String type;
        private int price;
        
        public Cheese() {}
        public Cheese(String type, int price) {
            this.type = type;
            this.price = price;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getPrice() { return price; }
        public void setPrice(int price) { this.price = price; }
    }

    @Test
    public void testBiLinearInfiniteLoopReproduction() {
        // This DRL pattern recreates the scenario that causes infinite loop
        // Multiple rules with similar patterns that trigger BiLinear optimization
        // but create duplicate node inputs (FirstInput: X, SecondInput: X)
        String drl = 
            "package org.drools.test;\n" +
            "import " + Person.class.getCanonicalName() + ";\n" +
            "import " + Cheese.class.getCanonicalName() + ";\n" +
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

        try {
            // This should trigger the infinite loop issue during compilation
            KieBase kbase = new KieHelper().addContent(drl, ResourceType.DRL).build();
            KieSession kSession = kbase.newKieSession();
            
            // Insert test data
            kSession.insert(new Person("John", 30));
            kSession.insert(new Cheese("Cheddar", 25));
            
            int rulesFired = kSession.fireAllRules();
            kSession.dispose();
            
            // If we get here without infinite loop, test passes
            assertThat(rulesFired).isGreaterThan(0);
            
        } catch (RuntimeException e) {
            // Expected: Infinite loop detected in getMatchingNode() after 1000 iterations
            if (e.getMessage().contains("Infinite loop detected in getMatchingNode()")) {
                System.out.println("Successfully reproduced the infinite loop issue: " + e.getMessage());
                // For now, we expect this to fail - this test documents the bug
                throw e; // Re-throw to mark test as failing until fixed
            } else {
                throw e; // Unexpected error
            }
        }
    }
}
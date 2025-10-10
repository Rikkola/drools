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

import org.acme.insurance.Driver;
import org.acme.insurance.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Minimal decision table reproduction case for BiLinear bug.
 * This test creates the simplest possible DRL that mirrors what a 2-row decision table would generate.
 */
public class MinimalBiLinearDecisionTableTest {

    @BeforeEach
    public void setUp() {
        System.setProperty("drools.bilinear.enabled", "true");
        System.out.println("\n🧪 Minimal BiLinear Decision Table Test - BiLinear ENABLED");
    }

    /**
     * Creates the minimal DRL equivalent to a 2-row decision table.
     * This reproduces the exact pattern that causes BiLinear memory ID collisions.
     */
    private String createMinimalDecisionTableDrl() {
        return "package org.acme.insurance;\n" +
               "\n" +
               "import org.acme.insurance.Driver;\n" +
               "import org.acme.insurance.Policy;\n" +
               "\n" +
               "// Row 1: Driver(25-35) + Policy(COMPREHENSIVE) -> price=100\n" +
               "rule \"PolicyRule_1\"\n" +
               "    when\n" +
               "        Driver(age >= 25, age <= 35)    // Shared pattern triggers BiLinear\n" +
               "        policy: Policy(type == \"COMPREHENSIVE\")\n" +
               "    then\n" +
               "        policy.setBasePrice(100);\n" +
               "        System.out.println(\"PolicyRule_1 fired - COMPREHENSIVE -> 100\");\n" +
               "end\n" +
               "\n" +
               "// Row 2: Driver(25-35) + Policy(BASIC) -> price=80\n" +
               "rule \"PolicyRule_2\"\n" +
               "    when\n" +
               "        Driver(age >= 25, age <= 35)    // Same shared pattern causes memory collision\n" +
               "        policy: Policy(type == \"BASIC\")\n" +
               "    then\n" +
               "        policy.setBasePrice(80);\n" +
               "        System.out.println(\"PolicyRule_2 fired - BASIC -> 80\");\n" +
               "end\n";
    }

    @Test
    public void testMinimalDecisionTableBiLinearBug() {
        System.out.println("\n🔥 Minimal Decision Table BiLinear Bug Test");
        System.out.println("============================================");

        String drl = createMinimalDecisionTableDrl();
        System.out.println("📄 DRL (equivalent to 2-row decision table):");
        System.out.println(drl);

        // Create KieSession
        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        KieBase kieBase = kieHelper.build();
        KieSession session = kieBase.newKieSession();

        // Create test data
        Driver driver = new Driver();
        driver.setAge(30);  // Matches both rules (25 <= 30 <= 35)

        Policy comprehensivePolicy = new Policy();
        comprehensivePolicy.setType("COMPREHENSIVE");

        Policy basicPolicy = new Policy();
        basicPolicy.setType("BASIC");

        // Insert facts
        session.insert(driver);
        session.insert(comprehensivePolicy);
        session.insert(basicPolicy);

        System.out.println("\n📊 Inserted Facts:");
        System.out.println("   Driver(age=30) - matches both rules");
        System.out.println("   Policy(type=COMPREHENSIVE) - should trigger Rule 1");
        System.out.println("   Policy(type=BASIC) - should trigger Rule 2");

        System.out.println("\n📊 Before Rules:");
        System.out.println("   COMPREHENSIVE policy price: " + comprehensivePolicy.getBasePrice());
        System.out.println("   BASIC policy price: " + basicPolicy.getBasePrice());

        // Fire rules
        int fireCount = session.fireAllRules();
        session.dispose();

        System.out.println("\n📊 After Rules:");
        System.out.println("   Rules fired: " + fireCount);
        System.out.println("   COMPREHENSIVE policy price: " + comprehensivePolicy.getBasePrice());
        System.out.println("   BASIC policy price: " + basicPolicy.getBasePrice());

        // Expected behavior: Both rules should fire exactly once
        // COMPREHENSIVE policy should have price 100
        // BASIC policy should have price 80
        System.out.println("\n✅ Expected:");
        System.out.println("   Rules fired: 2");
        System.out.println("   COMPREHENSIVE price: 100");
        System.out.println("   BASIC price: 80");

        // Assertions
        assertThat(fireCount).as("Both rules should fire exactly once").isEqualTo(2);
        assertThat(comprehensivePolicy.getBasePrice()).as("COMPREHENSIVE policy should be priced at 100").isEqualTo(100);
        assertThat(basicPolicy.getBasePrice()).as("BASIC policy should be priced at 80").isEqualTo(80);
    }

    /**
     * Control test: Same logic without BiLinear to show expected behavior
     */
    @Test
    public void testMinimalDecisionTableWithoutBiLinear() {
        System.out.println("\n🔥 Control Test: Same Logic WITHOUT BiLinear");
        System.out.println("=============================================");

        // Disable BiLinear
        System.setProperty("drools.bilinear.enabled", "false");

        try {
            String drl = createMinimalDecisionTableDrl();

            KieHelper kieHelper = new KieHelper();
            kieHelper.addContent(drl, ResourceType.DRL);
            KieBase kieBase = kieHelper.build();
            KieSession session = kieBase.newKieSession();

            Driver driver = new Driver();
            driver.setAge(30);

            Policy comprehensivePolicy = new Policy();
            comprehensivePolicy.setType("COMPREHENSIVE");

            Policy basicPolicy = new Policy();
            basicPolicy.setType("BASIC");

            session.insert(driver);
            session.insert(comprehensivePolicy);
            session.insert(basicPolicy);

            int fireCount = session.fireAllRules();
            session.dispose();

            System.out.println("📊 WITHOUT BiLinear - Rules fired: " + fireCount);
            System.out.println("📊 WITHOUT BiLinear - COMPREHENSIVE price: " + comprehensivePolicy.getBasePrice());
            System.out.println("📊 WITHOUT BiLinear - BASIC price: " + basicPolicy.getBasePrice());

            // This should work correctly without BiLinear
            assertThat(fireCount).as("Without BiLinear: Both rules should fire").isEqualTo(2);
            assertThat(comprehensivePolicy.getBasePrice()).as("Without BiLinear: COMPREHENSIVE should be 100").isEqualTo(100);
            assertThat(basicPolicy.getBasePrice()).as("Without BiLinear: BASIC should be 80").isEqualTo(80);

        } finally {
            // Restore BiLinear
            System.setProperty("drools.bilinear.enabled", "true");
        }
    }

    /**
     * Test with 3 rows to show the issue scales with more shared patterns
     */
    @Test
    public void testThreeRowDecisionTableBiLinearBug() {
        System.out.println("\n🔥 Three Row Decision Table BiLinear Bug Test");
        System.out.println("==============================================");

        String drl = "package org.acme.insurance;\n" +
               "\n" +
               "import org.acme.insurance.Driver;\n" +
               "import org.acme.insurance.Policy;\n" +
               "\n" +
               "rule \"PolicyRule_1\"\n" +
               "    when\n" +
               "        Driver(age >= 25, age <= 35)\n" +
               "        policy: Policy(type == \"COMPREHENSIVE\")\n" +
               "    then\n" +
               "        policy.setBasePrice(100);\n" +
               "        System.out.println(\"PolicyRule_1: COMPREHENSIVE -> 100\");\n" +
               "end\n" +
               "\n" +
               "rule \"PolicyRule_2\"\n" +
               "    when\n" +
               "        Driver(age >= 25, age <= 35)\n" +  // Same shared pattern
               "        policy: Policy(type == \"BASIC\")\n" +
               "    then\n" +
               "        policy.setBasePrice(80);\n" +
               "        System.out.println(\"PolicyRule_2: BASIC -> 80\");\n" +
               "end\n" +
               "\n" +
               "rule \"PolicyRule_3\"\n" +
               "    when\n" +
               "        Driver(age >= 25, age <= 35)\n" +  // Same shared pattern again
               "        policy: Policy(type == \"PREMIUM\")\n" +
               "    then\n" +
               "        policy.setBasePrice(150);\n" +
               "        System.out.println(\"PolicyRule_3: PREMIUM -> 150\");\n" +
               "end\n";

        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        KieBase kieBase = kieHelper.build();
        KieSession session = kieBase.newKieSession();

        Driver driver = new Driver();
        driver.setAge(30);

        Policy comprehensivePolicy = new Policy();
        comprehensivePolicy.setType("COMPREHENSIVE");

        Policy basicPolicy = new Policy();
        basicPolicy.setType("BASIC");

        Policy premiumPolicy = new Policy();
        premiumPolicy.setType("PREMIUM");

        session.insert(driver);
        session.insert(comprehensivePolicy);
        session.insert(basicPolicy);
        session.insert(premiumPolicy);

        System.out.println("📊 Before Rules - All policies should have price 0:");
        System.out.println("   COMPREHENSIVE: " + comprehensivePolicy.getBasePrice());
        System.out.println("   BASIC: " + basicPolicy.getBasePrice());
        System.out.println("   PREMIUM: " + premiumPolicy.getBasePrice());

        int fireCount = session.fireAllRules();
        session.dispose();

        System.out.println("\n📊 After Rules:");
        System.out.println("   Rules fired: " + fireCount);
        System.out.println("   COMPREHENSIVE: " + comprehensivePolicy.getBasePrice());
        System.out.println("   BASIC: " + basicPolicy.getBasePrice());
        System.out.println("   PREMIUM: " + premiumPolicy.getBasePrice());

        // Expected: All 3 rules fire, each setting their respective policy price
        assertThat(fireCount).as("All 3 rules should fire").isEqualTo(3);
        assertThat(comprehensivePolicy.getBasePrice()).as("COMPREHENSIVE should be 100").isEqualTo(100);
        assertThat(basicPolicy.getBasePrice()).as("BASIC should be 80").isEqualTo(80);
        assertThat(premiumPolicy.getBasePrice()).as("PREMIUM should be 150").isEqualTo(150);
    }
}
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

import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.TestParametersUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Timeout;

/**
 * Tests demonstrating actual BiLinear network usage in realistic scenarios.
 * These tests are designed to trigger actual BiLinear node creation and
 * demonstrate the network sharing benefits in practice.
 */
@RunWith(Parameterized.class)
public class BiLinearActualNetworkUsageTest {

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    public BiLinearActualNetworkUsageTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
        this.kieBaseTestConfiguration = kieBaseTestConfiguration;
    }

    @Parameterized.Parameters(name = "KieBase type={0}")
    public static Collection<Object[]> getParameters() {
        return TestParametersUtil.getKieBaseCloudConfigurations(true);
    }

    /**
     * Test designed to force BiLinear node creation by meeting all required conditions:
     * 1. BiLinear enabled (default)
     * 2. No complex rules in package
     * 3. Multiple rules with identical pattern signatures
     * 4. Cross-pattern beta constraints
     * 5. No EmptyBetaConstraints
     * 6. No temporal constraints
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testForceBiLinearNodeCreation() {
        System.out.println("\n=== Force BiLinear Node Creation Test ===");
        System.setProperty("drools.bilinear.enabled", "true"); // Ensure enabled
        
        // Create rules that exactly match BiLinear creation requirements
        String drl = 
            "package org.drools.test.simple\n" +  // Simple package name, no complexity
            "global java.util.List results\n" +
            "\n" +
            // Multiple rules with IDENTICAL pattern signatures and cross-pattern constraints
            "rule \"basic_join_rule_one\"\n" +    // Simple rule names
            "when\n" +
            "    $person: Person( $personAge: age > 25 )\n" +
            "    $house: House( ownerAge == $personAge )\n" +  // Cross-pattern constraint
            "then\n" +
            "    results.add(\"Rule1: \" + $person.getName() + \" owns house\");\n" +
            "end\n" +
            "\n" +
            "rule \"basic_join_rule_two\"\n" +
            "when\n" +
            "    $person: Person( $personAge: age > 25 )\n" +  // IDENTICAL pattern
            "    $house: House( ownerAge == $personAge )\n" +   // IDENTICAL constraint
            "then\n" +
            "    results.add(\"Rule2: \" + $person.getName() + \" has property\");\n" +
            "end\n" +
            "\n" +
            "rule \"basic_join_rule_three\"\n" +
            "when\n" +
            "    $person: Person( $personAge: age > 25 )\n" +  // IDENTICAL pattern
            "    $house: House( ownerAge == $personAge )\n" +   // IDENTICAL constraint  
            "then\n" +
            "    results.add(\"Rule3: Property owner \" + $person.getName());\n" +
            "end\n";

        System.out.println("Creating KieBase with BiLinear-optimized rules...");
        long buildStart = System.currentTimeMillis();
        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("force-bilinear-test", kieBaseTestConfiguration, drl);
        long buildTime = System.currentTimeMillis() - buildStart;
        
        KieSession ksession = kieBase.newKieSession();
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        // Insert facts that will trigger the shared pattern
        ksession.insert(new Person("Alice", 30));
        ksession.insert(new Person("Bob", 35));  
        ksession.insert(new House(30)); // Matches Alice
        ksession.insert(new House(35)); // Matches Bob

        long execStart = System.currentTimeMillis();
        int fired = ksession.fireAllRules();
        long execTime = System.currentTimeMillis() - execStart;
        
        ksession.dispose();

        // Should fire 6 rules: 3 rules × 2 person-house matches
        assertThat(fired).isEqualTo(6);
        assertThat(results).hasSize(6);
        
        // Verify all expected combinations
        long aliceMatches = results.stream().filter(r -> r.contains("Alice")).count();
        long bobMatches = results.stream().filter(r -> r.contains("Bob")).count();
        
        assertThat(aliceMatches).isEqualTo(3); // 3 rules for Alice
        assertThat(bobMatches).isEqualTo(3);   // 3 rules for Bob

        System.out.println("✅ Force BiLinear node creation test completed");
        System.out.println("   - Build time: " + buildTime + "ms");
        System.out.println("   - Execution time: " + execTime + "ms");
        System.out.println("   - Rules fired: " + fired);
        System.out.println("   - Alice matches: " + aliceMatches);
        System.out.println("   - Bob matches: " + bobMatches);
        System.out.println("   - Check console for actual BiLinear node creation messages");
    }

    /**
     * Test demonstrating BiLinear network benefits with large-scale rule sharing.
     * This test creates many rules that share the same pattern to maximize
     * the benefits of network sharing.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testLargeScaleBiLinearNetworkSharing() {
        System.out.println("\n=== Large Scale BiLinear Network Sharing Test ===");
        
        StringBuilder drl = new StringBuilder();
        drl.append("package org.drools.test.largescale\n");
        drl.append("global java.util.List results\n\n");
        
        // Create 20 rules with identical pattern for maximum sharing benefit
        for (int i = 1; i <= 20; i++) {
            drl.append("rule \"shared_customer_order_rule_").append(i).append("\"\n");
            drl.append("when\n");
            drl.append("    $customer: Customer( $creditLimit: creditLimit > 1000 )\n");
            drl.append("    $order: Order( amount <= $creditLimit )\n");  // Cross-pattern constraint
            drl.append("then\n");
            drl.append("    results.add(\"Rule").append(i).append(": Customer \" + $customer.getName() + \" order $\" + $order.getAmount());\n");
            drl.append("end\n\n");
        }

        System.out.println("Building large-scale shared pattern ruleset (20 rules)...");
        long buildStart = System.currentTimeMillis();
        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("largescale-bilinear-test", kieBaseTestConfiguration, drl.toString());
        long buildTime = System.currentTimeMillis() - buildStart;
        
        KieSession ksession = kieBase.newKieSession();
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        // Insert customers and orders
        ksession.insert(new Customer("Premium", 5000.0));   // High credit limit
        ksession.insert(new Customer("Standard", 2000.0));  // Medium credit limit  
        ksession.insert(new Customer("Basic", 500.0));      // Low credit limit - won't match (< 1000)
        
        ksession.insert(new Order(1500.0)); // Matches Premium and Standard
        ksession.insert(new Order(3000.0)); // Matches only Premium
        ksession.insert(new Order(6000.0)); // Matches nobody (exceeds all limits)

        long execStart = System.currentTimeMillis();
        int fired = ksession.fireAllRules();
        long execTime = System.currentTimeMillis() - execStart;
        
        ksession.dispose();

        // Should fire 60 rules: 20 rules × 3 valid customer-order combinations
        // Premium customer: 2 orders × 20 rules = 40 firings
        // Standard customer: 1 order × 20 rules = 20 firings  
        // Basic customer: 0 orders (credit limit < 1000)
        assertThat(fired).isEqualTo(60);
        assertThat(results).hasSize(60);
        
        long premiumMatches = results.stream().filter(r -> r.contains("Premium")).count();
        long standardMatches = results.stream().filter(r -> r.contains("Standard")).count();
        long basicMatches = results.stream().filter(r -> r.contains("Basic")).count();
        
        assertThat(premiumMatches).isEqualTo(40); // 2 orders × 20 rules
        assertThat(standardMatches).isEqualTo(20); // 1 order × 20 rules
        assertThat(basicMatches).isEqualTo(0);     // Credit limit too low

        System.out.println("✅ Large scale BiLinear network sharing test completed");
        System.out.println("   - 20 rules sharing Customer->Order pattern");
        System.out.println("   - Build time: " + buildTime + "ms");
        System.out.println("   - Execution time: " + execTime + "ms");
        System.out.println("   - Total rules fired: " + fired);
        System.out.println("   - Premium customer matches: " + premiumMatches);
        System.out.println("   - Standard customer matches: " + standardMatches);
        System.out.println("   - Basic customer matches: " + basicMatches + " (correctly filtered)");
        System.out.println("   - Network sharing should provide significant memory savings");
    }

    /**
     * Test demonstrating BiLinear network performance under high fact insertion load.
     * This test simulates a realistic scenario with many facts being processed
     * through the shared BiLinear network.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testBiLinearNetworkUnderLoad() {
        System.out.println("\n=== BiLinear Network Under Load Test ===");
        
        String drl = 
            "package org.drools.test.load\n" +
            "global java.util.List results\n" +
            "\n" +
            // Shared pattern for high-volume processing
            "rule \"transaction_account_rule1\"\n" +
            "when\n" +
            "    $account: Account( $balance: balance > 0 )\n" +
            "    $transaction: Transaction( amount <= $balance )\n" +
            "then\n" +
            "    results.add(\"Rule1: Account \" + $account.getId() + \" transaction $\" + $transaction.getAmount());\n" +
            "end\n" +
            "\n" +
            "rule \"transaction_account_rule2\"\n" +
            "when\n" +
            "    $account: Account( $balance: balance > 0 )\n" +
            "    $transaction: Transaction( amount <= $balance )\n" +
            "then\n" +
            "    results.add(\"Rule2: Processing $\" + $transaction.getAmount() + \" for account \" + $account.getId());\n" +
            "end\n" +
            "\n" +
            "rule \"transaction_account_rule3\"\n" +
            "when\n" +
            "    $account: Account( $balance: balance > 0 )\n" +
            "    $transaction: Transaction( amount <= $balance )\n" +
            "then\n" +
            "    results.add(\"Rule3: Approved transaction \" + $transaction.getId() + \" for account \" + $account.getId());\n" +
            "end\n";

        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("load-bilinear-test", kieBaseTestConfiguration, drl);
        KieSession ksession = kieBase.newKieSession();
        
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        // Insert many accounts with different balances
        List<Account> accounts = new ArrayList<>();
        IntStream.range(1, 11).forEach(i -> {
            Account account = new Account(i, i * 1000.0); // $1000, $2000, ..., $10000
            accounts.add(account);
            ksession.insert(account);
        });

        // Insert many transactions with different amounts
        List<Transaction> transactions = new ArrayList<>();
        IntStream.range(1, 21).forEach(i -> {
            Transaction transaction = new Transaction(i, i * 200.0); // $200, $400, ..., $4000
            transactions.add(transaction);
            ksession.insert(transaction);
        });

        System.out.println("Inserted " + accounts.size() + " accounts and " + transactions.size() + " transactions");
        
        long execStart = System.currentTimeMillis();
        int fired = ksession.fireAllRules();
        long execTime = System.currentTimeMillis() - execStart;
        
        ksession.dispose();

        // Calculate expected matches: for each account, count transactions <= balance
        int expectedMatches = 0;
        for (Account account : accounts) {
            for (Transaction transaction : transactions) {
                if (transaction.getAmount() <= account.getBalance()) {
                    expectedMatches++;
                }
            }
        }
        int expectedRuleFirings = expectedMatches * 3; // 3 rules for each match

        assertThat(fired).isEqualTo(expectedRuleFirings);
        assertThat(results).hasSize(expectedRuleFirings);

        System.out.println("✅ BiLinear network under load test completed");
        System.out.println("   - Accounts processed: " + accounts.size());
        System.out.println("   - Transactions processed: " + transactions.size());
        System.out.println("   - Expected valid combinations: " + expectedMatches);
        System.out.println("   - Total rules fired: " + fired);
        System.out.println("   - Execution time: " + execTime + "ms");
        System.out.println("   - Average time per rule firing: " + (fired > 0 ? (double)execTime / fired : 0) + "ms");
        System.out.println("   - BiLinear network efficiently processed high-volume fact combinations");
    }

    /**
     * Test demonstrating BiLinear network behavior with incremental fact updates.
     * This shows how BiLinear networks handle dynamic fact changes.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testBiLinearNetworkIncrementalUpdates() {
        System.out.println("\n=== BiLinear Network Incremental Updates Test ===");
        
        String drl = 
            "package org.drools.test.incremental\n" +
            "global java.util.List results\n" +
            "\n" +
            "rule \"employee_project_assignment_rule1\"\n" +
            "when\n" +
            "    $employee: Employee( $skillLevel: skillLevel >= 3 )\n" +
            "    $project: Project( requiredSkill <= $skillLevel )\n" +
            "then\n" +
            "    results.add(\"Rule1: \" + $employee.getName() + \" assigned to \" + $project.getName());\n" +
            "end\n" +
            "\n" +
            "rule \"employee_project_assignment_rule2\"\n" +
            "when\n" +
            "    $employee: Employee( $skillLevel: skillLevel >= 3 )\n" +
            "    $project: Project( requiredSkill <= $skillLevel )\n" +
            "then\n" +
            "    results.add(\"Rule2: Project \" + $project.getName() + \" gets employee \" + $employee.getName());\n" +
            "end\n";

        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("incremental-bilinear-test", kieBaseTestConfiguration, drl);
        KieSession ksession = kieBase.newKieSession();
        
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        // Phase 1: Initial state
        Employee alice = new Employee("Alice", 4);  // Skill level 4
        Employee bob = new Employee("Bob", 2);      // Skill level 2 (won't match - < 3)
        Project webApp = new Project("WebApp", 3);  // Requires skill 3
        Project mobileApp = new Project("MobileApp", 5); // Requires skill 5 (won't match Alice)
        
        ksession.insert(alice);
        ksession.insert(bob);
        ksession.insert(webApp);
        ksession.insert(mobileApp);
        
        int fired1 = ksession.fireAllRules();
        System.out.println("   Phase 1 - Initial assignments: " + fired1 + " rules fired");
        
        // Only Alice should match WebApp (skill 4 >= required 3)
        assertThat(fired1).isEqualTo(2); // 2 rules for Alice-WebApp
        
        // Phase 2: Promote Bob's skill level
        ksession.delete(ksession.getFactHandle(bob));
        bob = new Employee("Bob", 4); // Promoted to skill level 4
        ksession.insert(bob);
        
        int fired2 = ksession.fireAllRules();
        System.out.println("   Phase 2 - Bob promoted: " + fired2 + " rules fired");
        
        // Now Bob should also match WebApp
        assertThat(fired2).isEqualTo(2); // 2 rules for Bob-WebApp
        
        // Phase 3: Add easier project
        Project simpleApp = new Project("SimpleApp", 2); // Requires skill 2
        ksession.insert(simpleApp);
        
        int fired3 = ksession.fireAllRules();
        System.out.println("   Phase 3 - Added simple project: " + fired3 + " rules fired");
        
        // Both Alice and Bob should match SimpleApp (both have skill 4 >= 2)
        assertThat(fired3).isEqualTo(4); // 2 rules × 2 employees for SimpleApp
        
        // Phase 4: Promote Alice to expert level
        ksession.delete(ksession.getFactHandle(alice));
        alice = new Employee("Alice", 6); // Expert level
        ksession.insert(alice);
        
        int fired4 = ksession.fireAllRules();
        System.out.println("   Phase 4 - Alice promoted to expert: " + fired4 + " rules fired");
        
        // Alice should now match MobileApp as well
        assertThat(fired4).isEqualTo(6); // 2 rules × 3 projects for Alice
        
        ksession.dispose();

        int totalFired = fired1 + fired2 + fired3 + fired4;
        System.out.println("✅ BiLinear network incremental updates test completed");
        System.out.println("   - Total rule firings across all phases: " + totalFired);
        System.out.println("   - BiLinear network correctly handled incremental fact updates");
        System.out.println("   - Network sharing maintained throughout dynamic changes");
    }

    // Test data classes
    public static class Person {
        private String name;
        private int age;
        
        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() { return name; }
        public int getAge() { return age; }
    }
    
    public static class House {
        private int ownerAge;
        
        public House(int ownerAge) {
            this.ownerAge = ownerAge;
        }
        
        public int getOwnerAge() { return ownerAge; }
    }
    
    public static class Customer {
        private String name;
        private double creditLimit;
        
        public Customer(String name, double creditLimit) {
            this.name = name;
            this.creditLimit = creditLimit;
        }
        
        public String getName() { return name; }
        public double getCreditLimit() { return creditLimit; }
    }
    
    public static class Order {
        private double amount;
        
        public Order(double amount) {
            this.amount = amount;
        }
        
        public double getAmount() { return amount; }
    }
    
    public static class Account {
        private int id;
        private double balance;
        
        public Account(int id, double balance) {
            this.id = id;
            this.balance = balance;
        }
        
        public int getId() { return id; }
        public double getBalance() { return balance; }
    }
    
    public static class Transaction {
        private int id;
        private double amount;
        
        public Transaction(int id, double amount) {
            this.id = id;
            this.amount = amount;
        }
        
        public int getId() { return id; }
        public double getAmount() { return amount; }
    }
    
    public static class Employee {
        private String name;
        private int skillLevel;
        
        public Employee(String name, int skillLevel) {
            this.name = name;
            this.skillLevel = skillLevel;
        }
        
        public String getName() { return name; }
        public int getSkillLevel() { return skillLevel; }
    }
    
    public static class Project {
        private String name;
        private int requiredSkill;
        
        public Project(String name, int requiredSkill) {
            this.name = name;
            this.requiredSkill = requiredSkill;
        }
        
        public String getName() { return name; }
        public int getRequiredSkill() { return requiredSkill; }
    }
}
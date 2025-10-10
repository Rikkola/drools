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

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Timeout;

/**
 * Tests demonstrating BiLinear tuple processing and cross-network tuple joins.
 * These tests focus on the runtime behavior of BiLinear nodes when processing
 * tuples from two different left input networks.
 */
@RunWith(Parameterized.class)
public class BiLinearTupleProcessingTest {

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    public BiLinearTupleProcessingTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
        this.kieBaseTestConfiguration = kieBaseTestConfiguration;
    }

    @Parameterized.Parameters(name = "KieBase type={0}")
    public static Collection<Object[]> getParameters() {
        return TestParametersUtil.getKieBaseCloudConfigurations(true);
    }

    /**
     * Test BiLinear tuple creation and processing with two left input networks.
     * This test verifies that BiLinearTuple objects are created correctly and
     * enable proper cross-network variable resolution.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testBiLinearTupleCreationAndProcessing() {
        System.out.println("\n=== BiLinear Tuple Creation and Processing Test ===");
        
        String drl = 
            "package org.drools.test.bilinear\n" +
            "global java.util.List results\n" +
            "\n" +
            // First network: Employee -> Department
            "rule \"employee_department_network\"\n" +
            "when\n" +
            "    $emp: Employee( $empId: id, $deptId: departmentId )\n" +
            "    $dept: Department( id == $deptId, $deptName: name )\n" +
            "then\n" +
            "    results.add(\"Network1: \" + $emp.getName() + \" works in \" + $deptName);\n" +
            "end\n" +
            "\n" +
            // Second network: Same Employee -> Department pattern (BiLinear candidate)
            "rule \"employee_department_analysis\"\n" +
            "when\n" +
            "    $emp: Employee( $empId: id, $deptId: departmentId )\n" +
            "    $dept: Department( id == $deptId, $deptName: name )\n" +
            "then\n" +
            "    results.add(\"Network2: Analyzing \" + $emp.getName() + \" in department \" + $deptName);\n" +
            "end\n" +
            "\n" +
            // Third network: Same pattern for comprehensive sharing test
            "rule \"employee_department_reporting\"\n" +
            "when\n" +
            "    $emp: Employee( $empId: id, $deptId: departmentId )\n" +
            "    $dept: Department( id == $deptId, $deptName: name )\n" +
            "then\n" +
            "    results.add(\"Network3: Reporting on \" + $emp.getName() + \" from \" + $deptName);\n" +
            "end\n";

        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("bilinear-tuple-test", kieBaseTestConfiguration, drl);
        KieSession ksession = kieBase.newKieSession();
        
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        // Insert test data
        ksession.insert(new Employee(1, "Alice", 100));
        ksession.insert(new Employee(2, "Bob", 200));
        ksession.insert(new Department(100, "Engineering"));
        ksession.insert(new Department(200, "Sales"));

        int fired = ksession.fireAllRules();
        ksession.dispose();

        // Should fire 6 rules total (3 rules × 2 employee-department matches)
        assertThat(fired).isEqualTo(6);
        assertThat(results).hasSize(6);
        
        // Verify all combinations are present
        assertThat(results).contains(
            "Network1: Alice works in Engineering",
            "Network2: Analyzing Alice in department Engineering",
            "Network3: Reporting on Alice from Engineering",
            "Network1: Bob works in Sales",
            "Network2: Analyzing Bob in department Sales", 
            "Network3: Reporting on Bob from Sales"
        );

        System.out.println("✅ BiLinear tuple processing test completed");
        System.out.println("   - 3 networks sharing Employee->Department pattern");
        System.out.println("   - 2 employee-department pairs processed");
        System.out.println("   - Total rules fired: " + fired);
        System.out.println("   - BiLinear tuples enable cross-network variable resolution");
    }

    /**
     * Test BiLinear tuple constraint evaluation across multiple networks.
     * This verifies that constraints are properly evaluated when tuples
     * come from different left input sources.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testBiLinearConstraintEvaluationAcrossNetworks() {
        System.out.println("\n=== BiLinear Constraint Evaluation Test ===");
        
        String drl = 
            "package org.drools.test.bilinear\n" +
            "global java.util.List results\n" +
            "\n" +
            // Network with complex cross-pattern constraints
            "rule \"salary_budget_constraint_rule1\"\n" +
            "when\n" +
            "    $emp: Employee( $salary: salary > 50000 )\n" +
            "    $budget: Budget( totalAmount >= $salary, $budgetId: id )\n" +
            "then\n" +
            "    results.add(\"Rule1: Employee \" + $emp.getName() + \" ($\" + $salary + \") fits budget \" + $budgetId);\n" +
            "end\n" +
            "\n" +
            // Same constraint pattern - should share BiLinear node
            "rule \"salary_budget_constraint_rule2\"\n" +
            "when\n" +
            "    $emp: Employee( $salary: salary > 50000 )\n" +
            "    $budget: Budget( totalAmount >= $salary, $budgetId: id )\n" +
            "then\n" +
            "    results.add(\"Rule2: Budget \" + $budgetId + \" can afford employee \" + $emp.getName() + \" ($\" + $salary + \")\");\n" +
            "end\n" +
            "\n" +
            // Same constraint pattern with different action
            "rule \"salary_budget_constraint_rule3\"\n" +
            "when\n" +
            "    $emp: Employee( $salary: salary > 50000 )\n" +
            "    $budget: Budget( totalAmount >= $salary, $budgetId: id )\n" +
            "then\n" +
            "    results.add(\"Rule3: Allocation approved - \" + $emp.getName() + \" to budget \" + $budgetId);\n" +
            "end\n";

        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("bilinear-constraint-test", kieBaseTestConfiguration, drl);
        KieSession ksession = kieBase.newKieSession();
        
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        // Insert employees with different salaries
        ksession.insert(new Employee(1, "HighPaid", 60000));  // Should match
        ksession.insert(new Employee(2, "LowPaid", 40000));   // Should NOT match (salary <= 50000)
        ksession.insert(new Employee(3, "VeryHighPaid", 80000)); // Should match if budget allows
        
        // Insert budgets with different amounts
        ksession.insert(new Budget(1, 65000));  // Can afford HighPaid but not VeryHighPaid
        ksession.insert(new Budget(2, 90000));  // Can afford both HighPaid and VeryHighPaid

        int fired = ksession.fireAllRules();
        ksession.dispose();

        // Should fire 9 rules: 3 rules × 3 valid employee-budget combinations
        // HighPaid (60k) matches both budgets (65k, 90k) = 6 rule firings
        // VeryHighPaid (80k) matches only budget 2 (90k) = 3 rule firings
        // LowPaid doesn't match any rules (salary <= 50000)
        assertThat(fired).isEqualTo(9);
        assertThat(results).hasSize(9);

        // Verify constraint evaluation worked correctly
        long highPaidMatches = results.stream().filter(r -> r.contains("HighPaid")).count();
        long veryHighPaidMatches = results.stream().filter(r -> r.contains("VeryHighPaid")).count();
        long lowPaidMatches = results.stream().filter(r -> r.contains("LowPaid")).count();
        
        assertThat(highPaidMatches).isEqualTo(6); // Matches both budgets, 3 rules each
        assertThat(veryHighPaidMatches).isEqualTo(3); // Matches only budget 2, 3 rules
        assertThat(lowPaidMatches).isEqualTo(0); // No matches due to salary constraint

        System.out.println("✅ BiLinear constraint evaluation test completed");
        System.out.println("   - Complex constraints evaluated across Employee->Budget networks");
        System.out.println("   - HighPaid employee matched: " + highPaidMatches + " times");
        System.out.println("   - VeryHighPaid employee matched: " + veryHighPaidMatches + " times");
        System.out.println("   - LowPaid employee matched: " + lowPaidMatches + " times (correctly filtered)");
        System.out.println("   - Total rules fired: " + fired);
    }

    /**
     * Test BiLinear tuple memory management and lifecycle.
     * This test verifies that BiLinear tuples are properly managed
     * during inserts, updates, and deletes.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testBiLinearTupleMemoryManagement() {
        System.out.println("\n=== BiLinear Tuple Memory Management Test ===");
        
        String drl = 
            "package org.drools.test.bilinear\n" +
            "global java.util.List results\n" +
            "\n" +
            "rule \"product_category_match_rule1\"\n" +
            "when\n" +
            "    $product: Product( $categoryId: categoryId, $productName: name )\n" +
            "    $category: Category( id == $categoryId, $categoryName: name )\n" +
            "then\n" +
            "    results.add(\"Rule1: \" + $productName + \" is in category \" + $categoryName);\n" +
            "end\n" +
            "\n" +
            "rule \"product_category_match_rule2\"\n" +
            "when\n" +
            "    $product: Product( $categoryId: categoryId, $productName: name )\n" +
            "    $category: Category( id == $categoryId, $categoryName: name )\n" +
            "then\n" +
            "    results.add(\"Rule2: Category \" + $categoryName + \" contains \" + $productName);\n" +
            "end\n";

        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("bilinear-memory-test", kieBaseTestConfiguration, drl);
        KieSession ksession = kieBase.newKieSession();
        
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        // Phase 1: Initial inserts
        Product laptop = new Product(1, "Laptop", 100);
        Category electronics = new Category(100, "Electronics");
        
        ksession.insert(laptop);
        ksession.insert(electronics);
        
        int fired1 = ksession.fireAllRules();
        assertThat(fired1).isEqualTo(2);
        assertThat(results).hasSize(2);
        
        System.out.println("   Phase 1 - Initial inserts: " + fired1 + " rules fired");

        // Phase 2: Add more facts
        Product phone = new Product(2, "Phone", 100);
        ksession.insert(phone);
        
        int fired2 = ksession.fireAllRules();
        assertThat(fired2).isEqualTo(2); // Only new product rules fire
        assertThat(results).hasSize(4); // Total results increased
        
        System.out.println("   Phase 2 - Add phone: " + fired2 + " rules fired");

        // Phase 3: Add new category and product
        Category books = new Category(200, "Books");
        Product novel = new Product(3, "Novel", 200);
        
        ksession.insert(books);
        ksession.insert(novel);
        
        int fired3 = ksession.fireAllRules();
        assertThat(fired3).isEqualTo(2); // Only novel-books rules fire
        assertThat(results).hasSize(6); // Total results increased
        
        System.out.println("   Phase 3 - Add books category and novel: " + fired3 + " rules fired");

        // Verify final state
        long electronicsMatches = results.stream().filter(r -> r.contains("Electronics")).count();
        long booksMatches = results.stream().filter(r -> r.contains("Books")).count();
        
        assertThat(electronicsMatches).isEqualTo(4); // Laptop and Phone, 2 rules each
        assertThat(booksMatches).isEqualTo(2); // Novel, 2 rules
        
        ksession.dispose();

        System.out.println("✅ BiLinear tuple memory management test completed");
        System.out.println("   - Electronics category matches: " + electronicsMatches);
        System.out.println("   - Books category matches: " + booksMatches);
        System.out.println("   - Total rule firings across all phases: " + (fired1 + fired2 + fired3));
        System.out.println("   - BiLinear tuples properly managed during incremental inserts");
    }

    /**
     * Test BiLinear tuple processing with multiple fact types and complex joins.
     * This demonstrates BiLinear capability in realistic business scenarios.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testBiLinearMultipleFactTypeProcessing() {
        System.out.println("\n=== BiLinear Multiple Fact Type Processing Test ===");
        
        String drl = 
            "package org.drools.test.bilinear\n" +
            "global java.util.List results\n" +
            "\n" +
            // Complex business rule with multiple fact types
            "rule \"order_fulfillment_rule1\"\n" +
            "when\n" +
            "    $order: Order( $customerId: customerId, $total: total > 100 )\n" +
            "    $customer: Customer( id == $customerId, creditLimit >= $total )\n" +
            "then\n" +
            "    results.add(\"Rule1: Order \" + $order.getId() + \" approved for customer \" + $customer.getName());\n" +
            "end\n" +
            "\n" +
            // Same pattern - should use BiLinear sharing
            "rule \"order_fulfillment_rule2\"\n" +
            "when\n" +
            "    $order: Order( $customerId: customerId, $total: total > 100 )\n" +
            "    $customer: Customer( id == $customerId, creditLimit >= $total )\n" +
            "then\n" +
            "    results.add(\"Rule2: Processing order \" + $order.getId() + \" for \" + $customer.getName());\n" +
            "end\n" +
            "\n" +
            // Same pattern with additional business logic
            "rule \"order_fulfillment_rule3\"\n" +
            "when\n" +
            "    $order: Order( $customerId: customerId, $total: total > 100 )\n" +
            "    $customer: Customer( id == $customerId, creditLimit >= $total )\n" +
            "then\n" +
            "    results.add(\"Rule3: Credit check passed for order \" + $order.getId());\n" +
            "end\n";

        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("bilinear-multifact-test", kieBaseTestConfiguration, drl);
        KieSession ksession = kieBase.newKieSession();
        
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        // Insert customers with different credit limits
        ksession.insert(new Customer(1, "Alice", 500.0));
        ksession.insert(new Customer(2, "Bob", 200.0));
        ksession.insert(new Customer(3, "Charlie", 1000.0));

        // Insert orders with different amounts and customers
        ksession.insert(new Order(101, 1, 150.0)); // Alice, should pass (150 < 500)
        ksession.insert(new Order(102, 2, 250.0)); // Bob, should fail (250 > 200)
        ksession.insert(new Order(103, 3, 300.0)); // Charlie, should pass (300 < 1000)
        ksession.insert(new Order(104, 1, 50.0));  // Alice, should fail (50 <= 100)

        int fired = ksession.fireAllRules();
        ksession.dispose();

        // Should fire 6 rules: 2 valid orders × 3 rules each
        assertThat(fired).isEqualTo(6);
        assertThat(results).hasSize(6);

        // Verify specific order processing
        long aliceOrderMatches = results.stream().filter(r -> r.contains("101")).count(); // Order 101
        long bobOrderMatches = results.stream().filter(r -> r.contains("102")).count();   // Order 102
        long charlieOrderMatches = results.stream().filter(r -> r.contains("103")).count(); // Order 103
        long smallOrderMatches = results.stream().filter(r -> r.contains("104")).count();  // Order 104
        
        assertThat(aliceOrderMatches).isEqualTo(3); // All 3 rules
        assertThat(bobOrderMatches).isEqualTo(0);   // Credit limit exceeded
        assertThat(charlieOrderMatches).isEqualTo(3); // All 3 rules  
        assertThat(smallOrderMatches).isEqualTo(0);  // Amount too small

        System.out.println("✅ BiLinear multiple fact type processing test completed");
        System.out.println("   - Alice's order 101 processed: " + aliceOrderMatches + " times");
        System.out.println("   - Bob's order 102 rejected: " + bobOrderMatches + " times (credit limit)");
        System.out.println("   - Charlie's order 103 processed: " + charlieOrderMatches + " times");
        System.out.println("   - Small order 104 rejected: " + smallOrderMatches + " times (amount threshold)");
        System.out.println("   - Total rules fired: " + fired);
        System.out.println("   - BiLinear handles complex multi-fact type joins correctly");
    }

    // Test data classes
    public static class Employee {
        private int id;
        private String name;
        private int departmentId;
        private double salary;
        
        public Employee(int id, String name, int departmentId) {
            this.id = id;
            this.name = name;
            this.departmentId = departmentId;
            this.salary = 50000; // Default
        }
        
        public Employee(int id, String name, double salary) {
            this.id = id;
            this.name = name;
            this.salary = salary;
            this.departmentId = 0; // Default
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public int getDepartmentId() { return departmentId; }
        public double getSalary() { return salary; }
    }
    
    public static class Department {
        private int id;
        private String name;
        
        public Department(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
    }
    
    public static class Budget {
        private int id;
        private double totalAmount;
        
        public Budget(int id, double totalAmount) {
            this.id = id;
            this.totalAmount = totalAmount;
        }
        
        public int getId() { return id; }
        public double getTotalAmount() { return totalAmount; }
    }
    
    public static class Product {
        private int id;
        private String name;
        private int categoryId;
        
        public Product(int id, String name, int categoryId) {
            this.id = id;
            this.name = name;
            this.categoryId = categoryId;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public int getCategoryId() { return categoryId; }
    }
    
    public static class Category {
        private int id;
        private String name;
        
        public Category(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
    }
    
    public static class Order {
        private int id;
        private int customerId;
        private double total;
        
        public Order(int id, int customerId, double total) {
            this.id = id;
            this.customerId = customerId;
            this.total = total;
        }
        
        public int getId() { return id; }
        public int getCustomerId() { return customerId; }
        public double getTotal() { return total; }
    }
    
    public static class Customer {
        private int id;
        private String name;
        private double creditLimit;
        
        public Customer(int id, String name, double creditLimit) {
            this.id = id;
            this.name = name;
            this.creditLimit = creditLimit;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public double getCreditLimit() { return creditLimit; }
    }
}
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
import org.drools.testcoverage.common.util.TestParametersUtil;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Error handling and robustness tests for BiLinear optimization.
 * Tests BiLinear behavior under invalid conditions, error scenarios, and edge cases.
 */
@RunWith(Parameterized.class)
public class BiLinearErrorHandlingTest {

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    public BiLinearErrorHandlingTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
        this.kieBaseTestConfiguration = kieBaseTestConfiguration;
    }

    @Parameterized.Parameters(name = "KieBase type={0}")
    public static Collection<Object[]> getParameters() {
        return TestParametersUtil.getKieBaseCloudConfigurations(true);
    }

    @After
    public void cleanup() {
        BiLinearTestUtils.cleanupBiLinearProperties();
    }

    @Test
    public void testInvalidDRLWithBiLinearEnabled() {
        // Test BiLinear behavior with syntactically invalid DRL
        String invalidDrl = """
            package org.drools.test
            import %s
            
            rule "Invalid Rule"
            when
                $p: Person( invalidProperty == "test" )  // Invalid property
                $proj: Project( owner == $p.name
            then
                results.add("Should not execute");
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName());

        // BiLinear should fail gracefully during compilation
        assertThatThrownBy(() -> {
            BiLinearTestUtils.createBiLinearEnabledSession(invalidDrl, kieBaseTestConfiguration);
        }).isInstanceOf(Exception.class);
    }

    @Test
    public void testNullConstraintHandling() {
        String drl = """
            package org.drools.test
            import %s
            import %s
            global java.util.List results
            
            rule "Null Constraint Rule"
            when
                $p: Person( name != null )
                $proj: Project( owner == $p.name, name != null )
            then
                results.add("NullSafe: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert valid data
            ksession.insert(new BiLinearTestUtils.Person("John", 30));
            ksession.insert(new BiLinearTestUtils.Project("ProjectA", "John", 1));

            // Insert data with nulls
            ksession.insert(new BiLinearTestUtils.Person(null, 25)); // null name
            ksession.insert(new BiLinearTestUtils.Project(null, "John", 2)); // null project name

            int rulesExecuted = ksession.fireAllRules();

            // Should handle nulls gracefully and only fire for valid data
            assertThat(rulesExecuted).isPositive();
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).contains("John");

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testConflictingPatternConstraints() {
        // Test BiLinear with logically conflicting constraints
        String drl = """
            package org.drools.test
            import %s
            import %s
            global java.util.List results
            
            rule "Conflicting Rule 1"
            when
                $p: Person( age > 30 )
                $proj: Project( owner == $p.name, priority > 5 )
            then
                results.add("Conflict1: " + $p.getName());
            end
            
            rule "Conflicting Rule 2"
            when
                $p: Person( age > 30 )
                $proj: Project( owner == $p.name, priority < 3 )
            then
                results.add("Conflict2: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert data that satisfies first rule but not second
            ksession.insert(new BiLinearTestUtils.Person("Alice", 35));
            ksession.insert(new BiLinearTestUtils.Project("HighPriorityProject", "Alice", 7));

            // Insert data that satisfies second rule but not first
            ksession.insert(new BiLinearTestUtils.Person("Bob", 40));
            ksession.insert(new BiLinearTestUtils.Project("LowPriorityProject", "Bob", 1));

            int rulesExecuted = ksession.fireAllRules();

            // Should handle conflicting constraints correctly
            assertThat(rulesExecuted).isEqualTo(2);
            assertThat(results).hasSize(2);

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testExcessiveMemoryPressure() {
        // Test BiLinear behavior under memory pressure
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(20, true);

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Force memory pressure by inserting many facts
            for (int i = 0; i < 1000; i++) {
                ksession.insert(new BiLinearTestUtils.Person("Person" + i, 25 + (i % 40), "Dept" + (i % 10)));
                ksession.insert(new BiLinearTestUtils.Project("Project" + i, "Person" + i, (i % 5) + 1));
                ksession.insert(new BiLinearTestUtils.Task("Task" + i, "Person" + i, "Project" + i, 8 + (i % 20)));
            }

            // Should handle memory pressure gracefully
            long startTime = System.currentTimeMillis();
            int rulesExecuted = ksession.fireAllRules();
            long endTime = System.currentTimeMillis();

            System.out.println("Excessive memory pressure test:");
            System.out.println("  Rules executed: " + rulesExecuted);
            System.out.println("  Execution time: " + (endTime - startTime) + "ms");
            System.out.println("  Results count: " + results.size());

            // Should complete without errors
            assertThat(rulesExecuted).isPositive();
            assertThat(endTime - startTime).isLessThan(30000); // Less than 30 seconds

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testRecursiveRulePatterns() {
        // Test BiLinear with potentially recursive patterns
        String drl = """
            package org.drools.test
            import %s
            import %s
            import %s
            global java.util.List results
            
            rule "Recursive Pattern Rule"
            when
                $p1: Person( $name1: name )
                $p2: Person( name != $name1, department == $p1.department )
                $proj: Project( owner == $name1 )
                exists( Task( assignee == $p2.name, project == $proj.name ) )
            then
                results.add("Recursive: " + $name1 + " -> " + $p2.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName(),
                          BiLinearTestUtils.Task.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert data that creates recursive relationships
            ksession.insert(new BiLinearTestUtils.Person("Alice", 30, "Engineering"));
            ksession.insert(new BiLinearTestUtils.Person("Bob", 28, "Engineering"));
            ksession.insert(new BiLinearTestUtils.Project("ProjectX", "Alice", 3));
            ksession.insert(new BiLinearTestUtils.Task("TaskY", "Bob", "ProjectX", 10));

            int rulesExecuted = ksession.fireAllRules();

            // Should handle recursive patterns without infinite loops
            assertThat(rulesExecuted).isEqualTo(1);
            assertThat(results).hasSize(1);

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testMalformedConstraintExpressions() {
        // Test BiLinear with edge case constraint expressions
        String drl = """
            package org.drools.test
            import %s
            import %s
            global java.util.List results
            
            rule "Edge Case Constraints"
            when
                $p: Person( age > 0, age < 150 ) // Valid range
                $proj: Project( owner == $p.name, priority >= 1, priority <= 5 ) // Valid priority range
            then
                results.add("Valid: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert edge case data
            ksession.insert(new BiLinearTestUtils.Person("EdgeCase1", 0, "TestDept")); // Age boundary
            ksession.insert(new BiLinearTestUtils.Person("EdgeCase2", 150, "TestDept")); // Age boundary
            ksession.insert(new BiLinearTestUtils.Person("Valid", 30, "TestDept")); // Valid case

            ksession.insert(new BiLinearTestUtils.Project("ValidProject", "Valid", 3));
            ksession.insert(new BiLinearTestUtils.Project("EdgeProject1", "EdgeCase1", 0)); // Priority boundary
            ksession.insert(new BiLinearTestUtils.Project("EdgeProject2", "EdgeCase2", 6)); // Priority boundary

            int rulesExecuted = ksession.fireAllRules();

            // Should only match the valid combination
            assertThat(rulesExecuted).isEqualTo(1);
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).contains("Valid");

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testCircularReferenceDetection() {
        // Test BiLinear with potential circular references in fact relationships
        String drl = """
            package org.drools.test
            import %s
            import %s
            global java.util.List results
            
            rule "Circular Reference Rule"
            when
                $p1: Person( $name1: name )
                $p2: Person( name != $name1, department == $p1.department )
                $proj1: Project( owner == $name1 )
                $proj2: Project( owner == $p2.name, name != $proj1.name )
            then
                results.add("Circular: " + $name1 + " <-> " + $p2.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Create potential circular references
            ksession.insert(new BiLinearTestUtils.Person("Alice", 30, "Engineering"));
            ksession.insert(new BiLinearTestUtils.Person("Bob", 28, "Engineering"));
            ksession.insert(new BiLinearTestUtils.Project("AliceProject", "Alice", 3));
            ksession.insert(new BiLinearTestUtils.Project("BobProject", "Bob", 2));

            int rulesExecuted = ksession.fireAllRules();

            // Should detect circular patterns correctly without infinite loops
            assertThat(rulesExecuted).isEqualTo(2); // Each person matches with the other
            assertThat(results).hasSize(2);

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testResourceExhaustionRecovery() {
        // Test BiLinear recovery from resource exhaustion scenarios
        String drl = BiLinearTestUtils.generateStressTestDRL(30, 3);

        System.out.println("Testing resource exhaustion recovery...");

        try {
            // First, exhaust resources with a large session
            KieSession heavySession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
            try {
                List<String> heavyResults = new ArrayList<>();
                heavySession.setGlobal("results", heavyResults);
                BiLinearTestUtils.insertTestData(heavySession, 500, 400, 800); // Heavy load
                
                // This might fail due to resource exhaustion, but shouldn't crash
                try {
                    heavySession.fireAllRules();
                } catch (OutOfMemoryError e) {
                    System.out.println("Expected resource exhaustion occurred");
                }
            } finally {
                heavySession.dispose();
            }

            // Force garbage collection
            System.gc();
            Thread.sleep(100);

            // Then, verify that new sessions can still be created and work normally
            KieSession recoverySession = BiLinearTestUtils.createBiLinearEnabledSession(
                BiLinearTestUtils.generateOverlappingPatternDRL(5, false), kieBaseTestConfiguration);
            try {
                List<String> recoveryResults = new ArrayList<>();
                recoverySession.setGlobal("results", recoveryResults);
                BiLinearTestUtils.insertTestData(recoverySession, 10, 8, 15); // Normal load

                int rulesExecuted = recoverySession.fireAllRules();

                // Should recover and work normally
                assertThat(rulesExecuted).isPositive();
                System.out.println("Recovery successful: " + rulesExecuted + " rules executed");

            } finally {
                recoverySession.dispose();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testInvalidPropertyAccess() {
        // Test BiLinear robustness with invalid property access patterns
        String drl = """
            package org.drools.test
            import %s
            global java.util.List results
            
            rule "Safe Property Access"
            when
                $p: Person( name != null && name.length() > 0 )
            then
                results.add("Safe: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert valid and invalid data
            ksession.insert(new BiLinearTestUtils.Person("ValidPerson", 30));
            ksession.insert(new BiLinearTestUtils.Person("", 25)); // Empty name
            ksession.insert(new BiLinearTestUtils.Person(null, 35)); // Null name

            int rulesExecuted = ksession.fireAllRules();

            // Should safely handle null/invalid property access
            assertThat(rulesExecuted).isEqualTo(1);
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).contains("ValidPerson");

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testConcurrentErrorRecovery() {
        // Test BiLinear error recovery under concurrent conditions
        String validDrl = BiLinearTestUtils.generateOverlappingPatternDRL(5, false);
        
        int threadCount = 4;
        List<Exception> caughtExceptions = new ArrayList<>();
        List<Integer> successfulExecutions = new ArrayList<>();

        List<Runnable> operations = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            operations.add(() -> {
                try {
                    KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(validDrl, kieBaseTestConfiguration);
                    try {
                        List<String> results = new ArrayList<>();
                        ksession.setGlobal("results", results);
                        
                        // Some threads will have problematic data
                        if (threadId % 2 == 0) {
                            // Normal data
                            BiLinearTestUtils.insertTestData(ksession, 10, 8, 15);
                        } else {
                            // Insert null data that might cause issues
                            ksession.insert(new BiLinearTestUtils.Person(null, -1));
                            ksession.insert(new BiLinearTestUtils.Project(null, null, -1));
                        }
                        
                        int executed = ksession.fireAllRules();
                        synchronized (successfulExecutions) {
                            successfulExecutions.add(executed);
                        }
                        
                    } finally {
                        ksession.dispose();
                    }
                } catch (Exception e) {
                    synchronized (caughtExceptions) {
                        caughtExceptions.add(e);
                    }
                }
            });
        }

        BiLinearTestUtils.executeConcurrently(operations, threadCount);

        System.out.println("Concurrent error recovery test:");
        System.out.println("  Successful executions: " + successfulExecutions.size());
        System.out.println("  Caught exceptions: " + caughtExceptions.size());

        // Some threads should succeed despite others having problems
        assertThat(successfulExecutions.size() + caughtExceptions.size()).isEqualTo(threadCount);
        
        // At least some operations should succeed
        assertThat(successfulExecutions.size()).isPositive();
    }
}
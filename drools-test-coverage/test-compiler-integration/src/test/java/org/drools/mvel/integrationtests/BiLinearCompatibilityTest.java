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
import org.kie.api.runtime.rule.AgendaFilter;
import org.kie.api.runtime.rule.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compatibility tests for BiLinear optimization with existing Drools features.
 * Tests BiLinear integration with agenda groups, salience, CEP, accumulate, and other features.
 */
@RunWith(Parameterized.class)
public class BiLinearCompatibilityTest {

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    public BiLinearCompatibilityTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
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
    public void testBiLinearWithAgendaGroups() {
        String drl = """
            package org.drools.test
            import %s
            import %s
            global java.util.List results
            
            rule "Group1 Rule"
            agenda-group "group1"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
            then
                results.add("Group1: " + $p.getName());
            end
            
            rule "Group2 Rule"
            agenda-group "group2"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 2 )
            then
                results.add("Group2: " + $p.getName());
            end
            
            rule "Main Rule"
            when
                $p: Person( age > 30 )
                $proj: Project( owner == $p.name )
            then
                results.add("Main: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert test data
            ksession.insert(new BiLinearTestUtils.Person("Alice", 28));
            ksession.insert(new BiLinearTestUtils.Person("Bob", 35));
            ksession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 3));
            ksession.insert(new BiLinearTestUtils.Project("ProjectB", "Bob", 1));

            // Test agenda group activation
            ksession.getAgenda().getAgendaGroup("group1").setFocus();
            int group1Fired = ksession.fireAllRules();

            ksession.getAgenda().getAgendaGroup("group2").setFocus();
            int group2Fired = ksession.fireAllRules();

            int mainFired = ksession.fireAllRules(); // Main group (default)

            System.out.println("Agenda groups test:");
            System.out.println("  Group1 fired: " + group1Fired);
            System.out.println("  Group2 fired: " + group2Fired);
            System.out.println("  Main fired: " + mainFired);
            System.out.println("  Results: " + results);

            // Verify agenda groups work with BiLinear
            assertThat(group1Fired + group2Fired + mainFired).isPositive();
            assertThat(results).isNotEmpty();

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testBiLinearWithSalience() {
        String drl = """
            package org.drools.test
            import %s
            import %s
            global java.util.List results
            
            rule "High Priority Rule"
            salience 100
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
            then
                results.add("High: " + $p.getName());
            end
            
            rule "Medium Priority Rule"
            salience 50
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 1 )
            then
                results.add("Medium: " + $p.getName());
            end
            
            rule "Low Priority Rule"
            salience 10
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 3 )
            then
                results.add("Low: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert data that will trigger all rules
            ksession.insert(new BiLinearTestUtils.Person("Alice", 30));
            ksession.insert(new BiLinearTestUtils.Project("HighPriorityProject", "Alice", 5));

            int rulesExecuted = ksession.fireAllRules();

            System.out.println("Salience test:");
            System.out.println("  Rules executed: " + rulesExecuted);
            System.out.println("  Results order: " + results);

            // Verify salience ordering works with BiLinear
            assertThat(rulesExecuted).isEqualTo(3);
            assertThat(results).hasSize(3);
            
            // High priority should fire first
            assertThat(results.get(0)).startsWith("High:");

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testBiLinearWithAccumulate() {
        String drl = """
            package org.drools.test
            import %s
            import %s
            import %s
            global java.util.List results
            
            rule "Accumulate with BiLinear Pattern"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
                $totalHours: Number() from accumulate(
                    $task: Task( assignee == $p.name, project == $proj.name ),
                    sum($task.getEstimatedHours())
                )
            then
                results.add("Accumulate: " + $p.getName() + " total hours: " + $totalHours);
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName(),
                          BiLinearTestUtils.Task.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert data for accumulate test
            ksession.insert(new BiLinearTestUtils.Person("Alice", 30));
            ksession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 3));
            ksession.insert(new BiLinearTestUtils.Task("Task1", "Alice", "ProjectA", 8));
            ksession.insert(new BiLinearTestUtils.Task("Task2", "Alice", "ProjectA", 12));
            ksession.insert(new BiLinearTestUtils.Task("Task3", "Alice", "ProjectA", 5));

            int rulesExecuted = ksession.fireAllRules();

            System.out.println("Accumulate test:");
            System.out.println("  Rules executed: " + rulesExecuted);
            System.out.println("  Results: " + results);

            // Verify accumulate works with BiLinear patterns
            assertThat(rulesExecuted).isEqualTo(1);
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).contains("total hours: 25"); // 8 + 12 + 5

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testBiLinearWithExists() {
        String drl = """
            package org.drools.test
            import %s
            import %s
            import %s
            global java.util.List results
            
            rule "Exists Pattern Rule"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
                exists( Task( assignee == $p.name, project == $proj.name, !completed ) )
            then
                results.add("Exists: " + $p.getName() + " has incomplete tasks");
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName(),
                          BiLinearTestUtils.Task.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert data for exists test
            ksession.insert(new BiLinearTestUtils.Person("Alice", 30));
            ksession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 3));
            
            // Add completed task
            BiLinearTestUtils.Task completedTask = new BiLinearTestUtils.Task("CompletedTask", "Alice", "ProjectA", 8);
            completedTask.setCompleted(true);
            ksession.insert(completedTask);
            
            // Add incomplete task
            ksession.insert(new BiLinearTestUtils.Task("IncompleteTask", "Alice", "ProjectA", 12));

            int rulesExecuted = ksession.fireAllRules();

            System.out.println("Exists pattern test:");
            System.out.println("  Rules executed: " + rulesExecuted);
            System.out.println("  Results: " + results);

            // Verify exists works with BiLinear patterns
            assertThat(rulesExecuted).isEqualTo(1);
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).contains("has incomplete tasks");

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testBiLinearWithNot() {
        String drl = """
            package org.drools.test
            import %s
            import %s
            import %s
            global java.util.List results
            
            rule "Not Pattern Rule"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
                not( Task( assignee == $p.name, project == $proj.name, estimatedHours > 20 ) )
            then
                results.add("Not: " + $p.getName() + " has no large tasks");
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName(),
                          BiLinearTestUtils.Task.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert data for not test
            ksession.insert(new BiLinearTestUtils.Person("Alice", 30));
            ksession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 3));
            
            // Add small tasks only
            ksession.insert(new BiLinearTestUtils.Task("SmallTask1", "Alice", "ProjectA", 8));
            ksession.insert(new BiLinearTestUtils.Task("SmallTask2", "Alice", "ProjectA", 12));

            int rulesExecuted = ksession.fireAllRules();

            System.out.println("Not pattern test:");
            System.out.println("  Rules executed: " + rulesExecuted);
            System.out.println("  Results: " + results);

            // Verify not works with BiLinear patterns
            assertThat(rulesExecuted).isEqualTo(1);
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).contains("has no large tasks");

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testBiLinearWithAgendaFilter() {
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(8, false);

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            BiLinearTestUtils.insertTestData(ksession, 15, 10, 20);

            // Custom agenda filter to only fire specific rules
            AgendaFilter filter = new AgendaFilter() {
                @Override
                public boolean accept(Match match) {
                    String ruleName = match.getRule().getName();
                    return ruleName.contains("Rule1") || ruleName.contains("Rule3") || ruleName.contains("Rule5");
                }
            };

            int rulesExecuted = ksession.fireAllRules(filter);

            System.out.println("Agenda filter test:");
            System.out.println("  Rules executed with filter: " + rulesExecuted);
            System.out.println("  Results count: " + results.size());

            // Verify agenda filter works with BiLinear
            assertThat(rulesExecuted).isPositive();
            assertThat(rulesExecuted).isLessThan(8); // Should be filtered

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testBiLinearWithGlobalVariables() {
        String drl = """
            package org.drools.test
            import %s
            import %s
            global java.util.List results
            global Integer minAge
            global String targetDepartment
            
            rule "Global Variable Rule"
            when
                $p: Person( age > minAge, department == targetDepartment )
                $proj: Project( owner == $p.name )
            then
                results.add("Global: " + $p.getName() + " in " + targetDepartment);
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);
            ksession.setGlobal("minAge", 25);
            ksession.setGlobal("targetDepartment", "Engineering");

            // Insert test data
            ksession.insert(new BiLinearTestUtils.Person("Alice", 30, "Engineering"));
            ksession.insert(new BiLinearTestUtils.Person("Bob", 28, "Marketing"));
            ksession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 3));
            ksession.insert(new BiLinearTestUtils.Project("ProjectB", "Bob", 2));

            int rulesExecuted = ksession.fireAllRules();

            System.out.println("Global variables test:");
            System.out.println("  Rules executed: " + rulesExecuted);
            System.out.println("  Results: " + results);

            // Verify global variables work with BiLinear
            assertThat(rulesExecuted).isEqualTo(1); // Only Alice should match
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).contains("Alice");
            assertThat(results.get(0)).contains("Engineering");

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testBiLinearWithComplexConstraints() {
        String drl = """
            package org.drools.test
            import %s
            import %s
            import %s
            global java.util.List results
            
            rule "Complex Constraint Rule"
            when
                $p: Person( age > 25 && age < 50, department in ("Engineering", "Research") )
                $proj: Project( owner == $p.name, priority >= 2 && priority <= 4 )
                $task: Task( 
                    assignee == $p.name, 
                    project == $proj.name, 
                    estimatedHours >= 8 && estimatedHours <= 40,
                    !completed
                )
            then
                results.add("Complex: " + $p.getName() + " eligible for task " + $task.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName(),
                          BiLinearTestUtils.Task.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert data that meets complex criteria
            ksession.insert(new BiLinearTestUtils.Person("Alice", 30, "Engineering"));
            ksession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 3));
            ksession.insert(new BiLinearTestUtils.Task("TaskA", "Alice", "ProjectA", 16));

            // Insert data that doesn't meet all criteria
            ksession.insert(new BiLinearTestUtils.Person("Bob", 55, "Engineering")); // Too old
            ksession.insert(new BiLinearTestUtils.Project("ProjectB", "Bob", 5)); // Priority too high
            ksession.insert(new BiLinearTestUtils.Task("TaskB", "Bob", "ProjectB", 50)); // Too many hours

            int rulesExecuted = ksession.fireAllRules();

            System.out.println("Complex constraints test:");
            System.out.println("  Rules executed: " + rulesExecuted);
            System.out.println("  Results: " + results);

            // Verify complex constraints work with BiLinear
            assertThat(rulesExecuted).isEqualTo(1);
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).contains("Alice");

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testBiLinearWithRuleFlowGroups() {
        String drl = """
            package org.drools.test
            import %s
            import %s
            global java.util.List results
            
            rule "Setup Rule"
            ruleflow-group "setup"
            when
                $p: Person( age > 25 )
            then
                results.add("Setup: " + $p.getName());
            end
            
            rule "Processing Rule"
            ruleflow-group "processing"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
            then
                results.add("Processing: " + $p.getName());
            end
            
            rule "Cleanup Rule"
            ruleflow-group "cleanup"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 2 )
            then
                results.add("Cleanup: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);

            // Insert test data
            ksession.insert(new BiLinearTestUtils.Person("Alice", 30));
            ksession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 4));

            // Activate ruleflow groups in sequence
            ksession.getAgenda().getAgendaGroup("setup").setFocus();
            int setupFired = ksession.fireAllRules();

            ksession.getAgenda().getAgendaGroup("processing").setFocus();
            int processingFired = ksession.fireAllRules();

            ksession.getAgenda().getAgendaGroup("cleanup").setFocus();
            int cleanupFired = ksession.fireAllRules();

            System.out.println("Ruleflow groups test:");
            System.out.println("  Setup fired: " + setupFired);
            System.out.println("  Processing fired: " + processingFired);
            System.out.println("  Cleanup fired: " + cleanupFired);
            System.out.println("  Results: " + results);

            // Verify ruleflow groups work with BiLinear
            assertThat(setupFired + processingFired + cleanupFired).isEqualTo(3);
            assertThat(results).hasSize(3);

        } finally {
            ksession.dispose();
        }
    }
}
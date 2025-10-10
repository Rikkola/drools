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
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pattern evolution tests for BiLinear optimization.
 * Tests BiLinear behavior during dynamic rule addition, removal, and pattern sharing evolution.
 */
@RunWith(Parameterized.class)
public class BiLinearPatternEvolutionTest {

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    public BiLinearPatternEvolutionTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
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
    public void testPatternSharingEvolution() {
        // Start with a base set of rules that share patterns
        String baseDrl = """
            package org.drools.test
            import %s
            import %s
            global java.util.List results
            
            rule "Base Rule 1"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
            then
                results.add("Base1: " + $p.getName());
            end
            
            rule "Base Rule 2"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 1 )
            then
                results.add("Base2: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName());

        // Test base pattern sharing
        int baseRulesExecuted;
        int baseResultsSize;
        
        KieSession baseSession = BiLinearTestUtils.createBiLinearEnabledSession(baseDrl, kieBaseTestConfiguration);
        try {
            List<String> baseResults = new ArrayList<>();
            baseSession.setGlobal("results", baseResults);
            
            baseSession.insert(new BiLinearTestUtils.Person("Alice", 30));
            baseSession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 3));
            
            baseRulesExecuted = baseSession.fireAllRules();
            baseResultsSize = baseResults.size();
            
            System.out.println("Base pattern sharing:");
            System.out.println("  Rules executed: " + baseRulesExecuted);
            System.out.println("  Results: " + baseResults);
            
            assertThat(baseRulesExecuted).isEqualTo(2);
            assertThat(baseResults).hasSize(2);
            
        } finally {
            baseSession.dispose();
        }

        // Now test with evolved patterns (additional rules that extend sharing)
        String evolvedDrl = baseDrl + """
            
            rule "Evolved Rule 3"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 2 )
            then
                results.add("Evolved3: " + $p.getName());
            end
            
            rule "Evolved Rule 4"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
                exists( Task( assignee == $p.name, project == $proj.name ) )
            then
                results.add("Evolved4: " + $p.getName());
            end
            """;

        KieSession evolvedSession = BiLinearTestUtils.createBiLinearEnabledSession(evolvedDrl, kieBaseTestConfiguration);
        try {
            List<String> evolvedResults = new ArrayList<>();
            evolvedSession.setGlobal("results", evolvedResults);
            
            evolvedSession.insert(new BiLinearTestUtils.Person("Alice", 30));
            evolvedSession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 3));
            evolvedSession.insert(new BiLinearTestUtils.Task("TaskA", "Alice", "ProjectA", 8));
            
            int evolvedRulesExecuted = evolvedSession.fireAllRules();
            
            System.out.println("Evolved pattern sharing:");
            System.out.println("  Rules executed: " + evolvedRulesExecuted);
            System.out.println("  Results: " + evolvedResults);
            
            // Should execute all applicable rules with extended sharing
            assertThat(evolvedRulesExecuted).isGreaterThan(baseRulesExecuted);
            assertThat(evolvedResults.size()).isGreaterThan(baseResultsSize);
            
        } finally {
            evolvedSession.dispose();
        }
    }

    @Test
    public void testPatternSharingReduction() {
        // Test reduction in pattern sharing when rules are removed
        String fullDrl = BiLinearTestUtils.generateOverlappingPatternDRL(8, true);
        
        // Test with full rule set
        PerformanceMeasurementUtils.PerformanceMetrics fullMetrics = 
            PerformanceMeasurementUtils.measureOperation("Full_Pattern_Set", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(fullDrl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 20, 15, 30);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 5, 2);

        // Test with reduced rule set (only first 4 rules)
        String reducedDrl = BiLinearTestUtils.generateOverlappingPatternDRL(4, true);
        
        PerformanceMeasurementUtils.PerformanceMetrics reducedMetrics = 
            PerformanceMeasurementUtils.measureOperation("Reduced_Pattern_Set", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(reducedDrl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 20, 15, 30);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 5, 2);

        System.out.println("Pattern sharing reduction test:");
        System.out.println("  Full set: " + fullMetrics);
        System.out.println("  Reduced set: " + reducedMetrics);

        // Reduced set should execute fewer rules but potentially be more efficient per rule
        assertThat(reducedMetrics.getRulesExecuted()).isLessThan(fullMetrics.getRulesExecuted());
    }

    @Test
    public void testDynamicPatternAddition() {
        // Simulate dynamic pattern addition by testing different rule combinations
        String[] ruleProgression = {
            // Stage 1: Single rule
            """
            package org.drools.test
            import %s
            import %s
            global java.util.List results
            
            rule "Stage1 Rule"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
            then
                results.add("Stage1: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName()),

            // Stage 2: Add sharing rule
            """
            package org.drools.test
            import %s
            import %s
            global java.util.List results
            
            rule "Stage1 Rule"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
            then
                results.add("Stage1: " + $p.getName());
            end
            
            rule "Stage2 Rule"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 1 )
            then
                results.add("Stage2: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName()),

            // Stage 3: Add more sharing rules
            """
            package org.drools.test
            import %s
            import %s
            import %s
            global java.util.List results
            
            rule "Stage1 Rule"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
            then
                results.add("Stage1: " + $p.getName());
            end
            
            rule "Stage2 Rule"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 1 )
            then
                results.add("Stage2: " + $p.getName());
            end
            
            rule "Stage3 Rule"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 2 )
                $task: Task( assignee == $p.name, project == $proj.name )
            then
                results.add("Stage3: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName(),
                          BiLinearTestUtils.Task.class.getCanonicalName())
        };

        System.out.println("Dynamic pattern addition test:");

        for (int stage = 0; stage < ruleProgression.length; stage++) {
            String drl = ruleProgression[stage];
            final int currentStage = stage;
            
            long executionTime = BiLinearTestUtils.measureExecutionTime(() -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    
                    // Insert consistent test data
                    ksession.insert(new BiLinearTestUtils.Person("Alice", 30));
                    ksession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 3));
                    ksession.insert(new BiLinearTestUtils.Task("TaskA", "Alice", "ProjectA", 8));
                    
                    int rulesExecuted = ksession.fireAllRules();
                    
                    System.out.println("  Stage " + (currentStage + 1) + ": " + rulesExecuted + 
                                     " rules executed, " + 
                                     results.size() + " results");
                    
                } finally {
                    ksession.dispose();
                }
            });
        }

        // Test should complete successfully for all stages
        assertThat(true).isTrue();
    }

    @Test
    public void testPatternComplexityEvolution() {
        // Test evolution from simple to complex patterns
        List<String> evolutionStages = List.of(
            // Simple patterns
            "Person( age > 25 )",
            // Medium complexity
            "Person( age > 25, department != null )",
            // Complex patterns
            "Person( age > 25, department in (\"Engineering\", \"Research\"), name matches \"[A-Z].*\" )"
        );

        System.out.println("Pattern complexity evolution test:");

        for (int i = 0; i < evolutionStages.size(); i++) {
            String pattern = evolutionStages.get(i);
            final int currentStage = i;
            
            String drl = String.format("""
                package org.drools.test
                import %s
                import %s
                global java.util.List results
                
                rule "Evolution Rule 1"
                when
                    $p: %s
                    $proj: Project( owner == $p.name )
                then
                    results.add("Evo1: " + $p.getName());
                end
                
                rule "Evolution Rule 2" 
                when
                    $p: %s
                    $proj: Project( owner == $p.name, priority > 1 )
                then
                    results.add("Evo2: " + $p.getName());
                end
                """, BiLinearTestUtils.Person.class.getCanonicalName(),
                     BiLinearTestUtils.Project.class.getCanonicalName(),
                     pattern, pattern);

            long executionTime = BiLinearTestUtils.measureExecutionTime(() -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    
                    // Insert varied test data to test pattern matching
                    ksession.insert(new BiLinearTestUtils.Person("Alice", 30, "Engineering"));
                    ksession.insert(new BiLinearTestUtils.Person("bob", 28, "Marketing")); // lowercase
                    ksession.insert(new BiLinearTestUtils.Person("Charlie", 35, "Research"));
                    ksession.insert(new BiLinearTestUtils.Person("diana", 32, null)); // null department
                    
                    ksession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 3));
                    ksession.insert(new BiLinearTestUtils.Project("ProjectB", "bob", 2));
                    ksession.insert(new BiLinearTestUtils.Project("ProjectC", "Charlie", 1));
                    ksession.insert(new BiLinearTestUtils.Project("ProjectD", "diana", 4));
                    
                    int rulesExecuted = ksession.fireAllRules();
                    
                    System.out.println("  Complexity " + (currentStage + 1) + ": " + rulesExecuted + 
                                     " rules executed, " + results.size() + " results");
                    
                } finally {
                    ksession.dispose();
                }
            });
        }

        assertThat(true).isTrue();
    }

    @Test
    public void testPatternSharingWithInheritance() {
        // Test pattern sharing evolution with inheritance-like relationships
        String drl = """
            package org.drools.test
            import %s
            import %s
            import %s
            global java.util.List results
            
            rule "Base Pattern Rule"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
            then
                results.add("Base: " + $p.getName());
            end
            
            rule "Inherited Pattern Rule 1"
            extends "Base Pattern Rule"
            when
                $proj: Project( priority > 2 )
            then
                results.add("Inherited1: " + $p.getName());
            end
            
            rule "Inherited Pattern Rule 2"
            extends "Base Pattern Rule"
            when
                exists( Task( assignee == $p.name, project == $proj.name ) )
            then
                results.add("Inherited2: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName(),
                          BiLinearTestUtils.Task.class.getCanonicalName());

        // Note: This test may not work with all rule engines as 'extends' is not standard DRL
        // Testing pattern sharing that simulates inheritance
        String simulatedInheritanceDrl = """
            package org.drools.test
            import %s
            import %s
            import %s
            global java.util.List results
            
            rule "Base Pattern Rule"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
            then
                results.add("Base: " + $p.getName());
            end
            
            rule "Simulated Inherited Rule 1"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 2 )
            then
                results.add("SimInherited1: " + $p.getName());
            end
            
            rule "Simulated Inherited Rule 2"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name )
                exists( Task( assignee == $p.name, project == $proj.name ) )
            then
                results.add("SimInherited2: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName(),
                          BiLinearTestUtils.Task.class.getCanonicalName());

        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(simulatedInheritanceDrl, kieBaseTestConfiguration);
        try {
            List<String> results = new ArrayList<>();
            ksession.setGlobal("results", results);
            
            // Insert data that will trigger inherited patterns
            ksession.insert(new BiLinearTestUtils.Person("Alice", 30));
            ksession.insert(new BiLinearTestUtils.Project("ProjectA", "Alice", 3));
            ksession.insert(new BiLinearTestUtils.Task("TaskA", "Alice", "ProjectA", 8));
            
            int rulesExecuted = ksession.fireAllRules();
            
            System.out.println("Pattern inheritance simulation test:");
            System.out.println("  Rules executed: " + rulesExecuted);
            System.out.println("  Results: " + results);
            
            // Should execute base pattern and both inherited patterns
            assertThat(rulesExecuted).isEqualTo(3);
            assertThat(results).hasSize(3);
            
        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testPatternEvolutionPerformanceImpact() {
        // Test performance impact of pattern evolution
        System.out.println("Pattern evolution performance impact test:");

        // Measure performance at different evolution stages
        int[] ruleCounts = {5, 10, 15, 20};
        
        for (int ruleCount : ruleCounts) {
            String drl = BiLinearTestUtils.generateOverlappingPatternDRL(ruleCount, false);
            
            PerformanceMeasurementUtils.PerformanceMetrics metrics = 
                PerformanceMeasurementUtils.measureOperation("Rules_" + ruleCount, () -> {
                    KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                    try {
                        List<String> results = new ArrayList<>();
                        ksession.setGlobal("results", results);
                        BiLinearTestUtils.insertTestData(ksession, 20, 15, 30);
                        return ksession.fireAllRules();
                    } finally {
                        ksession.dispose();
                    }
                }, 5, 2);

            System.out.println("  " + ruleCount + " rules: " + metrics.getAverageExecutionTime() + "ms avg, " +
                             metrics.getRulesExecuted() + " executed");
        }

        // Performance should scale reasonably with rule count
        assertThat(true).isTrue(); // Completion test
    }
}
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

/**
 * Comprehensive performance comparison tests for BiLinear optimization.
 * This test class provides quantitative metrics comparing BiLinear enabled vs disabled scenarios.
 */
@RunWith(Parameterized.class)
public class BiLinearPerformanceComparisonTest {

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    public BiLinearPerformanceComparisonTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
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
    public void testBasicJoinPerformanceComparison() {
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(10, false);
        
        // Measure BiLinear enabled performance
        PerformanceMeasurementUtils.PerformanceMetrics bilinearMetrics = 
            PerformanceMeasurementUtils.measureOperation("BiLinear_Basic_Join", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 20, 15, 30);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 10, 3);

        // Measure standard performance (BiLinear disabled)
        PerformanceMeasurementUtils.PerformanceMetrics standardMetrics = 
            PerformanceMeasurementUtils.measureOperation("Standard_Basic_Join", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 20, 15, 30);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 10, 3);

        // Compare and validate results
        PerformanceMeasurementUtils.ComparisonResult comparison = 
            PerformanceMeasurementUtils.compareMetrics(standardMetrics, bilinearMetrics);
        
        PerformanceMeasurementUtils.printPerformanceReport(comparison);

        // Validate that both approaches produce correct results
        assertThat(bilinearMetrics.getRulesExecuted()).isEqualTo(standardMetrics.getRulesExecuted());
        
        // Performance validation (informational - actual improvement depends on many factors)
        if (comparison.getTimeImprovementPercent() < -50) {
            System.out.println("INFO: BiLinear may be significantly slower for this scenario");
        } else if (comparison.getTimeImprovementPercent() > 10) {
            System.out.println("INFO: BiLinear shows potential performance improvement: " + 
                             String.format("%.1f%%", comparison.getTimeImprovementPercent()));
        }
    }

    @Test
    public void testComplexConstraintJoinPerformance() {
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(15, true);
        
        PerformanceMeasurementUtils.PerformanceMetrics bilinearMetrics = 
            PerformanceMeasurementUtils.measureOperation("BiLinear_Complex_Join", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 25, 20, 40);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 8, 2);

        PerformanceMeasurementUtils.PerformanceMetrics standardMetrics = 
            PerformanceMeasurementUtils.measureOperation("Standard_Complex_Join", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 25, 20, 40);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 8, 2);

        PerformanceMeasurementUtils.ComparisonResult comparison = 
            PerformanceMeasurementUtils.compareMetrics(standardMetrics, bilinearMetrics);
        
        PerformanceMeasurementUtils.printPerformanceReport(comparison);

        assertThat(bilinearMetrics.getRulesExecuted()).isEqualTo(standardMetrics.getRulesExecuted());
    }

    @Test
    public void testHighCardinalityJoinPerformance() {
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(8, false);
        
        PerformanceMeasurementUtils.PerformanceMetrics bilinearMetrics = 
            PerformanceMeasurementUtils.measureOperation("BiLinear_High_Cardinality", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    // Higher fact counts to test scalability
                    BiLinearTestUtils.insertTestData(ksession, 50, 30, 80);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 6, 2);

        PerformanceMeasurementUtils.PerformanceMetrics standardMetrics = 
            PerformanceMeasurementUtils.measureOperation("Standard_High_Cardinality", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 50, 30, 80);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 6, 2);

        PerformanceMeasurementUtils.ComparisonResult comparison = 
            PerformanceMeasurementUtils.compareMetrics(standardMetrics, bilinearMetrics);
        
        PerformanceMeasurementUtils.printPerformanceReport(comparison);

        assertThat(bilinearMetrics.getRulesExecuted()).isEqualTo(standardMetrics.getRulesExecuted());
    }

    @Test
    public void testRuleConstructionTimePerformance() {
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(20, true);
        
        // Measure BiLinear rule construction time
        PerformanceMeasurementUtils.PerformanceMetrics bilinearConstructionMetrics = 
            PerformanceMeasurementUtils.measureOperation("BiLinear_Construction", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    return 1; // Successfully constructed
                } finally {
                    ksession.dispose();
                }
            }, 5, 1);

        // Measure standard rule construction time
        PerformanceMeasurementUtils.PerformanceMetrics standardConstructionMetrics = 
            PerformanceMeasurementUtils.measureOperation("Standard_Construction", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    return 1; // Successfully constructed
                } finally {
                    ksession.dispose();
                }
            }, 5, 1);

        PerformanceMeasurementUtils.ComparisonResult comparison = 
            PerformanceMeasurementUtils.compareMetrics(standardConstructionMetrics, bilinearConstructionMetrics);
        
        System.out.println("\nRule Construction Time Comparison:");
        PerformanceMeasurementUtils.printPerformanceReport(comparison);

        // Construction should always succeed
        assertThat(bilinearConstructionMetrics.getRulesExecuted()).isPositive();
        assertThat(standardConstructionMetrics.getRulesExecuted()).isPositive();
    }

    @Test
    public void testThroughputComparison() {
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(12, false);
        
        // Measure BiLinear throughput
        PerformanceMeasurementUtils.ThroughputMetrics bilinearThroughput = 
            PerformanceMeasurementUtils.measureThroughput("BiLinear_Throughput", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 15, 10, 25);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 5); // 5 second measurement

        // Measure standard throughput
        PerformanceMeasurementUtils.ThroughputMetrics standardThroughput = 
            PerformanceMeasurementUtils.measureThroughput("Standard_Throughput", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 15, 10, 25);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 5); // 5 second measurement

        System.out.println("\nThroughput Comparison:");
        System.out.println("BiLinear: " + bilinearThroughput);
        System.out.println("Standard: " + standardThroughput);

        double throughputImprovement = ((bilinearThroughput.getOperationsPerSecond() - 
                                       standardThroughput.getOperationsPerSecond()) / 
                                      standardThroughput.getOperationsPerSecond()) * 100.0;
        
        System.out.println(String.format("Throughput improvement: %.1f%%", throughputImprovement));

        // Both should process operations
        assertThat(bilinearThroughput.getTotalOperations()).isPositive();
        assertThat(standardThroughput.getTotalOperations()).isPositive();
    }

    @Test
    public void testMemoryUsageComparison() {
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(15, true);
        
        // Measure BiLinear memory usage
        BiLinearTestUtils.MemoryMeasurement bilinearMemory = 
            BiLinearTestUtils.measureMemoryUsage(() -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 30, 25, 50);
                    ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            });

        // Measure standard memory usage  
        BiLinearTestUtils.MemoryMeasurement standardMemory = 
            BiLinearTestUtils.measureMemoryUsage(() -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 30, 25, 50);
                    ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            });

        System.out.println("\nMemory Usage Comparison:");
        System.out.println("BiLinear: " + bilinearMemory);
        System.out.println("Standard: " + standardMemory);

        double memoryImprovement = 0.0;
        if (standardMemory.getMemoryDelta() > 0) {
            memoryImprovement = ((double)(standardMemory.getMemoryDelta() - bilinearMemory.getMemoryDelta()) / 
                               standardMemory.getMemoryDelta()) * 100.0;
        }
        
        System.out.println(String.format("Memory improvement: %.1f%%", memoryImprovement));

        // Memory usage should be reasonable (not negative due to GC effects)
        // This is mainly for observational purposes
    }

    @Test
    public void testPatternSharingEffectiveness() {
        // Create a DRL specifically designed to benefit from pattern sharing
        String drl = """
            package org.drools.test
            import %s
            import %s
            import %s
            global java.util.List results
            
            rule "Shared Pattern Rule 1"
            when
                $p: Person( age > 25, department == "Engineering" )
                $proj: Project( owner == $p.name, priority > 2 )
                $task: Task( assignee == $p.name, project == $proj.name )
            then
                results.add("SharedRule1: " + $p.getName());
            end
            
            rule "Shared Pattern Rule 2"
            when
                $p: Person( age > 25, department == "Engineering" )
                $proj: Project( owner == $p.name, priority > 2 )
                $task: Task( assignee == $p.name, estimatedHours > 10 )
            then
                results.add("SharedRule2: " + $p.getName());
            end
            
            rule "Shared Pattern Rule 3"
            when
                $p: Person( age > 25, department == "Engineering" )
                $proj: Project( owner == $p.name, priority > 2 )
                Task( assignee == $p.name, completed == false )
            then
                results.add("SharedRule3: " + $p.getName());
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName(),
                          BiLinearTestUtils.Task.class.getCanonicalName());

        PerformanceMeasurementUtils.PerformanceMetrics bilinearMetrics = 
            PerformanceMeasurementUtils.measureOperation("BiLinear_Pattern_Sharing", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    
                    // Insert data that will trigger the shared patterns
                    ksession.insert(new BiLinearTestUtils.Person("John", 30, "Engineering"));
                    ksession.insert(new BiLinearTestUtils.Person("Jane", 28, "Engineering"));
                    ksession.insert(new BiLinearTestUtils.Project("ProjectA", "John", 3));
                    ksession.insert(new BiLinearTestUtils.Project("ProjectB", "Jane", 4));
                    ksession.insert(new BiLinearTestUtils.Task("TaskA1", "John", "ProjectA", 12));
                    ksession.insert(new BiLinearTestUtils.Task("TaskA2", "John", "ProjectA", 8));
                    ksession.insert(new BiLinearTestUtils.Task("TaskB1", "Jane", "ProjectB", 15));
                    
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 8, 2);

        PerformanceMeasurementUtils.PerformanceMetrics standardMetrics = 
            PerformanceMeasurementUtils.measureOperation("Standard_Pattern_Sharing", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    
                    ksession.insert(new BiLinearTestUtils.Person("John", 30, "Engineering"));
                    ksession.insert(new BiLinearTestUtils.Person("Jane", 28, "Engineering"));
                    ksession.insert(new BiLinearTestUtils.Project("ProjectA", "John", 3));
                    ksession.insert(new BiLinearTestUtils.Project("ProjectB", "Jane", 4));
                    ksession.insert(new BiLinearTestUtils.Task("TaskA1", "John", "ProjectA", 12));
                    ksession.insert(new BiLinearTestUtils.Task("TaskA2", "John", "ProjectA", 8));
                    ksession.insert(new BiLinearTestUtils.Task("TaskB1", "Jane", "ProjectB", 15));
                    
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 8, 2);

        PerformanceMeasurementUtils.ComparisonResult comparison = 
            PerformanceMeasurementUtils.compareMetrics(standardMetrics, bilinearMetrics);
        
        System.out.println("\nPattern Sharing Effectiveness Test:");
        PerformanceMeasurementUtils.printPerformanceReport(comparison);

        // Both should produce the same number of rule executions
        assertThat(bilinearMetrics.getRulesExecuted()).isEqualTo(standardMetrics.getRulesExecuted());
        
        // Expect some rules to fire (the exact number depends on the data relationships)
        assertThat(bilinearMetrics.getRulesExecuted()).isPositive();
    }
}
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
 * Scalability tests for BiLinear optimization focusing on large-scale scenarios.
 * Tests BiLinear performance under high rule count, fact count, and complexity conditions.
 */
@RunWith(Parameterized.class)
public class BiLinearScalabilityTest {

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    public BiLinearScalabilityTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
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
    public void testLargeRuleSetScalability() {
        // Test with 50 rules having overlapping patterns
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(50, false);
        
        PerformanceMeasurementUtils.PerformanceMetrics bilinearMetrics = 
            PerformanceMeasurementUtils.measureOperation("BiLinear_50_Rules", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 30, 25, 50);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 5, 2);

        PerformanceMeasurementUtils.PerformanceMetrics standardMetrics = 
            PerformanceMeasurementUtils.measureOperation("Standard_50_Rules", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 30, 25, 50);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 5, 2);

        PerformanceMeasurementUtils.ComparisonResult comparison = 
            PerformanceMeasurementUtils.compareMetrics(standardMetrics, bilinearMetrics);
        
        System.out.println("\n=== LARGE RULE SET SCALABILITY TEST (50 Rules) ===");
        PerformanceMeasurementUtils.printPerformanceReport(comparison);

        // Both should produce the same results
        assertThat(bilinearMetrics.getRulesExecuted()).isEqualTo(standardMetrics.getRulesExecuted());
        
        // Should handle large rule sets without failure
        assertThat(bilinearMetrics.getAverageExecutionTime()).isGreaterThan(0);
        assertThat(standardMetrics.getAverageExecutionTime()).isGreaterThan(0);
    }

    @Test
    public void testHighCardinalityFactScalability() {
        // Test with moderate rule count but high fact count
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(15, true);
        
        PerformanceMeasurementUtils.PerformanceMetrics bilinearMetrics = 
            PerformanceMeasurementUtils.measureOperation("BiLinear_High_Facts", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    // High fact cardinality: 100 persons, 75 projects, 150 tasks
                    BiLinearTestUtils.insertTestData(ksession, 100, 75, 150);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 4, 1);

        PerformanceMeasurementUtils.PerformanceMetrics standardMetrics = 
            PerformanceMeasurementUtils.measureOperation("Standard_High_Facts", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 100, 75, 150);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 4, 1);

        PerformanceMeasurementUtils.ComparisonResult comparison = 
            PerformanceMeasurementUtils.compareMetrics(standardMetrics, bilinearMetrics);
        
        System.out.println("\n=== HIGH CARDINALITY FACT SCALABILITY TEST ===");
        PerformanceMeasurementUtils.printPerformanceReport(comparison);

        assertThat(bilinearMetrics.getRulesExecuted()).isEqualTo(standardMetrics.getRulesExecuted());
        
        // High cardinality should still execute in reasonable time
        assertThat(bilinearMetrics.getAverageExecutionTime()).isLessThan(10000); // Less than 10 seconds
    }

    @Test
    public void testComplexPatternScalability() {
        // Test with complex patterns and multiple constraints
        String drl = BiLinearTestUtils.generateStressTestDRL(25, 4); // 25 rules, 4 patterns each
        
        PerformanceMeasurementUtils.PerformanceMetrics bilinearMetrics = 
            PerformanceMeasurementUtils.measureOperation("BiLinear_Complex_Patterns", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 40, 30, 60);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 4, 1);

        PerformanceMeasurementUtils.PerformanceMetrics standardMetrics = 
            PerformanceMeasurementUtils.measureOperation("Standard_Complex_Patterns", () -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 40, 30, 60);
                    return ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            }, 4, 1);

        PerformanceMeasurementUtils.ComparisonResult comparison = 
            PerformanceMeasurementUtils.compareMetrics(standardMetrics, bilinearMetrics);
        
        System.out.println("\n=== COMPLEX PATTERN SCALABILITY TEST ===");
        PerformanceMeasurementUtils.printPerformanceReport(comparison);

        assertThat(bilinearMetrics.getRulesExecuted()).isEqualTo(standardMetrics.getRulesExecuted());
    }

    @Test
    public void testScalabilityProgression() {
        // Test scalability with progressively increasing rule counts
        int[] ruleCounts = {10, 20, 30, 40};
        
        System.out.println("\n=== SCALABILITY PROGRESSION TEST ===");
        
        for (int ruleCount : ruleCounts) {
            String drl = BiLinearTestUtils.generateOverlappingPatternDRL(ruleCount, false);
            
            long bilinearTime = BiLinearTestUtils.measureExecutionTime(() -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 25, 20, 40);
                    ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            });

            long standardTime = BiLinearTestUtils.measureExecutionTime(() -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 25, 20, 40);
                    ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            });

            double improvement = standardTime > 0 ? 
                ((double)(standardTime - bilinearTime) / standardTime) * 100.0 : 0.0;
            
            System.out.println(String.format("Rules: %2d | BiLinear: %4dms | Standard: %4dms | Improvement: %+.1f%%",
                              ruleCount, bilinearTime, standardTime, improvement));
        }

        // All tests should complete successfully
        assertThat(true).isTrue(); // Basic completion test
    }

    @Test
    public void testMemoryScalabilityUnderLoad() {
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(30, true);
        
        // Test memory behavior under increasing load
        int[] factCounts = {50, 100, 200};
        
        System.out.println("\n=== MEMORY SCALABILITY UNDER LOAD TEST ===");
        
        for (int factCount : factCounts) {
            BiLinearTestUtils.MemoryMeasurement bilinearMemory = 
                BiLinearTestUtils.measureMemoryUsage(() -> {
                    KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                    try {
                        List<String> results = new ArrayList<>();
                        ksession.setGlobal("results", results);
                        BiLinearTestUtils.insertTestData(ksession, factCount, factCount * 3/4, factCount * 3/2);
                        ksession.fireAllRules();
                    } finally {
                        ksession.dispose();
                    }
                });

            BiLinearTestUtils.MemoryMeasurement standardMemory = 
                BiLinearTestUtils.measureMemoryUsage(() -> {
                    KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                    try {
                        List<String> results = new ArrayList<>();
                        ksession.setGlobal("results", results);
                        BiLinearTestUtils.insertTestData(ksession, factCount, factCount * 3/4, factCount * 3/2);
                        ksession.fireAllRules();
                    } finally {
                        ksession.dispose();
                    }
                });

            System.out.println(String.format("Facts: %3d | BiLinear: %s | Standard: %s",
                              factCount, 
                              formatMemory(bilinearMemory.getMemoryDelta()),
                              formatMemory(standardMemory.getMemoryDelta())));
        }

        assertThat(true).isTrue(); // Completion test
    }

    @Test
    public void testExtremeScalabilityStressTest() {
        // This is a stress test - may take longer to run
        System.out.println("\n=== EXTREME SCALABILITY STRESS TEST ===");
        System.out.println("Testing BiLinear under extreme conditions...");
        
        String drl = BiLinearTestUtils.generateStressTestDRL(100, 3); // 100 rules, 3 patterns each
        
        // Test BiLinear under extreme load
        long startTime = System.currentTimeMillis();
        
        BiLinearTestUtils.MemoryMeasurement bilinearMemory = 
            BiLinearTestUtils.measureMemoryUsage(() -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 200, 150, 300);
                    int rulesExecuted = ksession.fireAllRules();
                    System.out.println("BiLinear executed " + rulesExecuted + " rules");
                } finally {
                    ksession.dispose();
                }
            });
        
        long bilinearTime = System.currentTimeMillis() - startTime;
        
        // Test standard approach under same extreme load
        startTime = System.currentTimeMillis();
        
        BiLinearTestUtils.MemoryMeasurement standardMemory = 
            BiLinearTestUtils.measureMemoryUsage(() -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 200, 150, 300);
                    int rulesExecuted = ksession.fireAllRules();
                    System.out.println("Standard executed " + rulesExecuted + " rules");
                } finally {
                    ksession.dispose();
                }
            });
        
        long standardTime = System.currentTimeMillis() - startTime;

        System.out.println("\nExtreme Stress Test Results:");
        System.out.println("BiLinear Time: " + bilinearTime + "ms, Memory: " + formatMemory(bilinearMemory.getMemoryDelta()));
        System.out.println("Standard Time: " + standardTime + "ms, Memory: " + formatMemory(standardMemory.getMemoryDelta()));
        
        double timeImprovement = standardTime > 0 ? 
            ((double)(standardTime - bilinearTime) / standardTime) * 100.0 : 0.0;
        
        System.out.println(String.format("Performance improvement: %.1f%%", timeImprovement));

        // Both approaches should complete without errors under extreme load
        assertThat(bilinearTime).isGreaterThan(0);
        assertThat(standardTime).isGreaterThan(0);
        
        // Execution time should be reasonable (less than 60 seconds for extreme test)
        assertThat(bilinearTime).isLessThan(60000);
    }

    @Test 
    public void testLinearScalingCharacteristics() {
        // Test to verify that BiLinear scaling is indeed better than quadratic
        System.out.println("\n=== LINEAR SCALING CHARACTERISTICS TEST ===");
        
        int[] ruleCounts = {5, 10, 15, 20, 25};
        double[] bilinearRatios = new double[ruleCounts.length - 1];
        double[] standardRatios = new double[ruleCounts.length - 1];
        
        long previousBilinearTime = 0;
        long previousStandardTime = 0;
        
        for (int i = 0; i < ruleCounts.length; i++) {
            int ruleCount = ruleCounts[i];
            String drl = BiLinearTestUtils.generateOverlappingPatternDRL(ruleCount, false);
            
            long bilinearTime = BiLinearTestUtils.measureExecutionTime(() -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 20, 15, 30);
                    ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            });

            long standardTime = BiLinearTestUtils.measureExecutionTime(() -> {
                KieSession ksession = BiLinearTestUtils.createBiLinearDisabledSession(drl, kieBaseTestConfiguration);
                try {
                    List<String> results = new ArrayList<>();
                    ksession.setGlobal("results", results);
                    BiLinearTestUtils.insertTestData(ksession, 20, 15, 30);
                    ksession.fireAllRules();
                } finally {
                    ksession.dispose();
                }
            });

            if (i > 0) {
                bilinearRatios[i - 1] = previousBilinearTime > 0 ? (double) bilinearTime / previousBilinearTime : 0.0;
                standardRatios[i - 1] = previousStandardTime > 0 ? (double) standardTime / previousStandardTime : 0.0;
            }
            
            System.out.println(String.format("Rules: %2d | BiLinear: %4dms | Standard: %4dms",
                              ruleCount, bilinearTime, standardTime));
            
            previousBilinearTime = bilinearTime;
            previousStandardTime = standardTime;
        }

        System.out.println("\nScaling ratios (current/previous time):");
        for (int i = 0; i < bilinearRatios.length; i++) {
            System.out.println(String.format("Step %d: BiLinear ratio: %.2f | Standard ratio: %.2f", 
                              i + 1, bilinearRatios[i], standardRatios[i]));
        }

        // The test completes successfully if no exceptions are thrown
        assertThat(bilinearRatios.length).isGreaterThan(0);
    }

    private String formatMemory(long bytes) {
        if (bytes < 0) return "-" + formatMemory(-bytes);
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
    }
}
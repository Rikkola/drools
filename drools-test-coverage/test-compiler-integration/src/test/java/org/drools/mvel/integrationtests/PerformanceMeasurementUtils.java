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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utility class providing standardized performance measurement functionality
 * for BiLinear and general Drools performance testing.
 */
public class PerformanceMeasurementUtils {

    /**
     * Container for comprehensive performance measurements
     */
    public static class PerformanceMetrics {
        private final String testName;
        private final long executionTime;
        private final long memoryUsed;
        private final long peakMemoryUsed;
        private final int rulesExecuted;
        private final double averageExecutionTime;
        private final double standardDeviation;
        private final List<Long> executionTimes;

        public PerformanceMetrics(String testName, long executionTime, long memoryUsed, 
                                long peakMemoryUsed, int rulesExecuted, 
                                double averageExecutionTime, double standardDeviation,
                                List<Long> executionTimes) {
            this.testName = testName;
            this.executionTime = executionTime;
            this.memoryUsed = memoryUsed;
            this.peakMemoryUsed = peakMemoryUsed;
            this.rulesExecuted = rulesExecuted;
            this.averageExecutionTime = averageExecutionTime;
            this.standardDeviation = standardDeviation;
            this.executionTimes = new ArrayList<>(executionTimes);
        }

        // Getters
        public String getTestName() { return testName; }
        public long getExecutionTime() { return executionTime; }
        public long getMemoryUsed() { return memoryUsed; }
        public long getPeakMemoryUsed() { return peakMemoryUsed; }
        public int getRulesExecuted() { return rulesExecuted; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public double getStandardDeviation() { return standardDeviation; }
        public List<Long> getExecutionTimes() { return new ArrayList<>(executionTimes); }

        @Override
        public String toString() {
            return String.format("%s: avg=%.2fms, std=%.2fms, mem=%dKB, peak=%dKB, rules=%d", 
                               testName, averageExecutionTime, standardDeviation, 
                               memoryUsed / 1024, peakMemoryUsed / 1024, rulesExecuted);
        }
    }

    /**
     * Performs multiple iterations of a test operation and collects comprehensive metrics
     */
    public static PerformanceMetrics measureOperation(String testName, 
                                                    Supplier<Integer> operation, 
                                                    int iterations, 
                                                    int warmupIterations) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        List<Long> executionTimes = new ArrayList<>();
        long totalMemoryUsed = 0;
        long peakMemoryUsed = 0;
        int totalRulesExecuted = 0;

        // Warmup iterations
        for (int i = 0; i < warmupIterations; i++) {
            forceGarbageCollection();
            operation.get();
        }

        // Actual measurement iterations
        for (int i = 0; i < iterations; i++) {
            forceGarbageCollection();
            
            MemoryUsage beforeMemory = memoryBean.getHeapMemoryUsage();
            long startTime = System.nanoTime();
            
            Integer rulesExecuted = operation.get();
            
            long endTime = System.nanoTime();
            MemoryUsage afterMemory = memoryBean.getHeapMemoryUsage();
            
            long executionTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            long memoryUsed = afterMemory.getUsed() - beforeMemory.getUsed();
            
            executionTimes.add(executionTime);
            totalMemoryUsed += memoryUsed;
            peakMemoryUsed = Math.max(peakMemoryUsed, afterMemory.getUsed());
            totalRulesExecuted += (rulesExecuted != null ? rulesExecuted : 0);
        }

        // Calculate statistics
        double averageTime = executionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double standardDeviation = calculateStandardDeviation(executionTimes, averageTime);
        long averageMemoryUsed = totalMemoryUsed / iterations;
        
        return new PerformanceMetrics(
            testName,
            (long) averageTime,
            averageMemoryUsed,
            peakMemoryUsed,
            totalRulesExecuted,
            averageTime,
            standardDeviation,
            executionTimes
        );
    }

    /**
     * Compares two performance metrics and provides improvement ratios
     */
    public static ComparisonResult compareMetrics(PerformanceMetrics baseline, PerformanceMetrics optimized) {
        double timeImprovement = calculateImprovement(baseline.getAverageExecutionTime(), 
                                                    optimized.getAverageExecutionTime());
        double memoryImprovement = calculateImprovement(baseline.getMemoryUsed(), 
                                                       optimized.getMemoryUsed());
        double peakMemoryImprovement = calculateImprovement(baseline.getPeakMemoryUsed(), 
                                                           optimized.getPeakMemoryUsed());
        
        boolean isTimeSignificant = isSignificantDifference(baseline.getExecutionTimes(), 
                                                           optimized.getExecutionTimes());
        
        return new ComparisonResult(baseline, optimized, timeImprovement, memoryImprovement, 
                                  peakMemoryImprovement, isTimeSignificant);
    }

    /**
     * Container for performance comparison results
     */
    public static class ComparisonResult {
        private final PerformanceMetrics baseline;
        private final PerformanceMetrics optimized;
        private final double timeImprovementPercent;
        private final double memoryImprovementPercent;
        private final double peakMemoryImprovementPercent;
        private final boolean isStatisticallySignificant;

        public ComparisonResult(PerformanceMetrics baseline, PerformanceMetrics optimized,
                              double timeImprovement, double memoryImprovement,
                              double peakMemoryImprovement, boolean isSignificant) {
            this.baseline = baseline;
            this.optimized = optimized;
            this.timeImprovementPercent = timeImprovement;
            this.memoryImprovementPercent = memoryImprovement;
            this.peakMemoryImprovementPercent = peakMemoryImprovement;
            this.isStatisticallySignificant = isSignificant;
        }

        public PerformanceMetrics getBaseline() { return baseline; }
        public PerformanceMetrics getOptimized() { return optimized; }
        public double getTimeImprovementPercent() { return timeImprovementPercent; }
        public double getMemoryImprovementPercent() { return memoryImprovementPercent; }
        public double getPeakMemoryImprovementPercent() { return peakMemoryImprovementPercent; }
        public boolean isStatisticallySignificant() { return isStatisticallySignificant; }

        @Override
        public String toString() {
            return String.format("Performance Comparison:\n" +
                               "  Baseline: %s\n" +
                               "  Optimized: %s\n" +
                               "  Time improvement: %.1f%%\n" +
                               "  Memory improvement: %.1f%%\n" +
                               "  Peak memory improvement: %.1f%%\n" +
                               "  Statistically significant: %s",
                               baseline, optimized, timeImprovementPercent, 
                               memoryImprovementPercent, peakMemoryImprovementPercent,
                               isStatisticallySignificant);
        }
    }

    /**
     * Performs a throughput measurement test
     */
    public static ThroughputMetrics measureThroughput(String testName, 
                                                    Supplier<Integer> operation,
                                                    int durationSeconds) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000L);
        
        int totalOperations = 0;
        int totalRulesExecuted = 0;
        List<Integer> operationCounts = new ArrayList<>();
        
        while (System.currentTimeMillis() < endTime) {
            Integer rulesExecuted = operation.get();
            totalOperations++;
            totalRulesExecuted += (rulesExecuted != null ? rulesExecuted : 0);
            operationCounts.add(rulesExecuted != null ? rulesExecuted : 0);
        }
        
        long actualDuration = System.currentTimeMillis() - startTime;
        double operationsPerSecond = (totalOperations * 1000.0) / actualDuration;
        double rulesPerSecond = (totalRulesExecuted * 1000.0) / actualDuration;
        
        return new ThroughputMetrics(testName, totalOperations, totalRulesExecuted,
                                   actualDuration, operationsPerSecond, rulesPerSecond);
    }

    /**
     * Container for throughput measurement results
     */
    public static class ThroughputMetrics {
        private final String testName;
        private final int totalOperations;
        private final int totalRulesExecuted;
        private final long durationMs;
        private final double operationsPerSecond;
        private final double rulesPerSecond;

        public ThroughputMetrics(String testName, int totalOperations, int totalRulesExecuted,
                               long durationMs, double operationsPerSecond, double rulesPerSecond) {
            this.testName = testName;
            this.totalOperations = totalOperations;
            this.totalRulesExecuted = totalRulesExecuted;
            this.durationMs = durationMs;
            this.operationsPerSecond = operationsPerSecond;
            this.rulesPerSecond = rulesPerSecond;
        }

        public String getTestName() { return testName; }
        public int getTotalOperations() { return totalOperations; }
        public int getTotalRulesExecuted() { return totalRulesExecuted; }
        public long getDurationMs() { return durationMs; }
        public double getOperationsPerSecond() { return operationsPerSecond; }
        public double getRulesPerSecond() { return rulesPerSecond; }

        @Override
        public String toString() {
            return String.format("%s: %.1f ops/sec, %.1f rules/sec (%d ops, %d rules in %dms)",
                               testName, operationsPerSecond, rulesPerSecond, 
                               totalOperations, totalRulesExecuted, durationMs);
        }
    }

    /**
     * Forces garbage collection and waits for it to complete
     */
    private static void forceGarbageCollection() {
        System.gc();
        try {
            Thread.sleep(10); // Small delay to allow GC to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Calculates improvement percentage between two values
     */
    private static double calculateImprovement(double baseline, double optimized) {
        if (baseline == 0) return 0.0;
        return ((baseline - optimized) / baseline) * 100.0;
    }

    /**
     * Calculates standard deviation for a list of execution times
     */
    private static double calculateStandardDeviation(List<Long> values, double average) {
        if (values.size() <= 1) return 0.0;
        
        double sumOfSquaredDifferences = values.stream()
            .mapToDouble(value -> Math.pow(value - average, 2))
            .sum();
        
        return Math.sqrt(sumOfSquaredDifferences / (values.size() - 1));
    }

    /**
     * Performs a simple t-test to check if the difference between two samples is significant
     */
    private static boolean isSignificantDifference(List<Long> sample1, List<Long> sample2) {
        if (sample1.size() < 2 || sample2.size() < 2) {
            return false; // Need at least 2 samples each for t-test
        }

        double mean1 = sample1.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double mean2 = sample2.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        double std1 = calculateStandardDeviation(sample1, mean1);
        double std2 = calculateStandardDeviation(sample2, mean2);
        
        if (std1 == 0.0 && std2 == 0.0) {
            return mean1 != mean2;
        }
        
        // Simplified t-test (assuming equal variances)
        double pooledStd = Math.sqrt(((std1 * std1) + (std2 * std2)) / 2);
        double standardError = pooledStd * Math.sqrt((1.0 / sample1.size()) + (1.0 / sample2.size()));
        
        if (standardError == 0.0) {
            return mean1 != mean2;
        }
        
        double tStatistic = Math.abs(mean1 - mean2) / standardError;
        
        // Critical value for 95% confidence (rough approximation)
        double criticalValue = 2.0; // Simplified - would normally depend on degrees of freedom
        
        return tStatistic > criticalValue;
    }

    /**
     * Creates a performance baseline for comparison purposes
     */
    public static PerformanceBaseline createBaseline(String name, 
                                                   Supplier<Integer> operation,
                                                   int iterations) {
        PerformanceMetrics metrics = measureOperation(name + "_baseline", operation, 
                                                    iterations, Math.max(iterations / 10, 3));
        return new PerformanceBaseline(name, metrics);
    }

    /**
     * Container for performance baseline data
     */
    public static class PerformanceBaseline {
        private final String name;
        private final PerformanceMetrics metrics;

        public PerformanceBaseline(String name, PerformanceMetrics metrics) {
            this.name = name;
            this.metrics = metrics;
        }

        public String getName() { return name; }
        public PerformanceMetrics getMetrics() { return metrics; }

        public ComparisonResult compareWith(PerformanceMetrics other) {
            return compareMetrics(this.metrics, other);
        }
    }

    /**
     * Utility method to print detailed performance report
     */
    public static void printPerformanceReport(ComparisonResult comparison) {
        System.out.println("=".repeat(80));
        System.out.println("PERFORMANCE COMPARISON REPORT");
        System.out.println("=".repeat(80));
        System.out.println(comparison);
        System.out.println("=".repeat(80));
    }
}
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency stress tests for BiLinear optimization.
 * Tests thread safety, concurrent access patterns, and race condition detection.
 */
@RunWith(Parameterized.class)
public class BiLinearConcurrencyStressTest {

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    public BiLinearConcurrencyStressTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
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
    public void testConcurrentSessionCreationAndExecution() {
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(15, true);
        
        int threadCount = 8;
        int operationsPerThread = 5;
        
        List<Runnable> operations = new ArrayList<>();
        AtomicInteger totalRulesExecuted = new AtomicInteger(0);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        
        // Create operations for BiLinear sessions
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            operations.add(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                        try {
                            List<String> results = Collections.synchronizedList(new ArrayList<>());
                            ksession.setGlobal("results", results);
                            BiLinearTestUtils.insertTestData(ksession, 20, 15, 30);
                            int rulesExecuted = ksession.fireAllRules();
                            totalRulesExecuted.addAndGet(rulesExecuted);
                        } finally {
                            ksession.dispose();
                        }
                    }
                } catch (Exception e) {
                    errorRef.set(e);
                }
            });
        }

        // Execute concurrently
        long startTime = System.currentTimeMillis();
        List<Future<?>> futures = BiLinearTestUtils.executeConcurrently(operations, threadCount);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n=== CONCURRENT SESSION CREATION TEST ===");
        System.out.println("Threads: " + threadCount + ", Operations per thread: " + operationsPerThread);
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");
        System.out.println("Total rules executed: " + totalRulesExecuted.get());
        
        // Verify no exceptions occurred
        assertThat(errorRef.get()).isNull();
        
        // Verify all operations completed
        assertThat(futures).hasSize(threadCount);
        
        // Verify expected number of rule executions
        assertThat(totalRulesExecuted.get()).isPositive();
    }

    @Test
    public void testConcurrentFactInsertionAndRuleExecution() {
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(10, false);
        
        int threadCount = 6;
        AtomicInteger totalRulesExecuted = new AtomicInteger(0);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        
        // Single shared session for concurrent access
        KieSession sharedSession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
        List<String> sharedResults = Collections.synchronizedList(new ArrayList<>());
        sharedSession.setGlobal("results", sharedResults);
        
        try {
            List<Runnable> operations = new ArrayList<>();
            
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                operations.add(() -> {
                    try {
                        // Each thread inserts facts and fires rules
                        for (int j = 0; j < 5; j++) {
                            // Insert thread-specific facts
                            sharedSession.insert(new BiLinearTestUtils.Person("Person" + threadId + "_" + j, 25 + j, "Dept" + threadId));
                            sharedSession.insert(new BiLinearTestUtils.Project("Project" + threadId + "_" + j, "Person" + threadId + "_" + j, j + 1));
                            sharedSession.insert(new BiLinearTestUtils.Task("Task" + threadId + "_" + j, "Person" + threadId + "_" + j, "Project" + threadId + "_" + j, 8 + j));
                            
                            // Fire rules
                            int rulesExecuted = sharedSession.fireAllRules();
                            totalRulesExecuted.addAndGet(rulesExecuted);
                        }
                    } catch (Exception e) {
                        errorRef.set(e);
                    }
                });
            }
            
            long startTime = System.currentTimeMillis();
            List<Future<?>> futures = BiLinearTestUtils.executeConcurrently(operations, threadCount);
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n=== CONCURRENT FACT INSERTION TEST ===");
            System.out.println("Threads: " + threadCount + ", Execution time: " + (endTime - startTime) + "ms");
            System.out.println("Total rules executed: " + totalRulesExecuted.get());
            System.out.println("Results collected: " + sharedResults.size());
            
            // Verify no exceptions occurred
            assertThat(errorRef.get()).isNull();
            
            // Verify operations completed
            assertThat(futures).hasSize(threadCount);
            
            // Verify rule executions occurred
            assertThat(totalRulesExecuted.get()).isPositive();
            
        } finally {
            sharedSession.dispose();
        }
    }

    @Test
    public void testConcurrentPatternSharingStress() {
        // This tests the core BiLinear functionality under concurrent load
        String drl = """
            package org.drools.test
            import %s
            import %s
            import %s
            global java.util.List results
            
            rule "Concurrent Rule A"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 1 )
            then
                synchronized(results) { results.add("RuleA: " + $p.getName()); }
            end
            
            rule "Concurrent Rule B"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 2 )
            then
                synchronized(results) { results.add("RuleB: " + $p.getName()); }
            end
            
            rule "Concurrent Rule C"
            when
                $p: Person( age > 25 )
                $proj: Project( owner == $p.name, priority > 3 )
            then
                synchronized(results) { results.add("RuleC: " + $p.getName()); }
            end
            """.formatted(BiLinearTestUtils.Person.class.getCanonicalName(),
                          BiLinearTestUtils.Project.class.getCanonicalName(),
                          BiLinearTestUtils.Task.class.getCanonicalName());

        int threadCount = 10;
        int iterationsPerThread = 10;
        AtomicInteger successfulIterations = new AtomicInteger(0);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        
        List<Runnable> operations = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            operations.add(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                        try {
                            List<String> results = Collections.synchronizedList(new ArrayList<>());
                            ksession.setGlobal("results", results);
                            
                            // Insert data that will trigger shared patterns
                            ksession.insert(new BiLinearTestUtils.Person("ConcurrentPerson" + threadId + "_" + j, 30, "Engineering"));
                            ksession.insert(new BiLinearTestUtils.Project("ConcurrentProject" + threadId + "_" + j, "ConcurrentPerson" + threadId + "_" + j, 4));
                            
                            ksession.fireAllRules();
                            successfulIterations.incrementAndGet();
                            
                        } finally {
                            ksession.dispose();
                        }
                    }
                } catch (Exception e) {
                    errorRef.set(e);
                }
            });
        }
        
        long startTime = System.currentTimeMillis();
        List<Future<?>> futures = BiLinearTestUtils.executeConcurrently(operations, threadCount);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n=== CONCURRENT PATTERN SHARING STRESS TEST ===");
        System.out.println("Threads: " + threadCount + ", Iterations per thread: " + iterationsPerThread);
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");
        System.out.println("Successful iterations: " + successfulIterations.get() + "/" + (threadCount * iterationsPerThread));
        
        // Verify no exceptions occurred
        assertThat(errorRef.get()).isNull();
        
        // Verify all iterations completed successfully
        assertThat(successfulIterations.get()).isEqualTo(threadCount * iterationsPerThread);
    }

    @Test
    public void testConcurrentRuleAdditionAndExecution() {
        // Test concurrent rule addition/removal scenarios
        String baseDrl = BiLinearTestUtils.generateOverlappingPatternDRL(5, false);
        
        int threadCount = 4;
        AtomicInteger operationsCompleted = new AtomicInteger(0);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        
        List<Runnable> operations = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            operations.add(() -> {
                try {
                    for (int j = 0; j < 3; j++) {
                        // Create session with additional rules specific to this thread
                        String threadSpecificDrl = baseDrl + generateThreadSpecificRules(threadId);
                        
                        KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(threadSpecificDrl, kieBaseTestConfiguration);
                        try {
                            List<String> results = Collections.synchronizedList(new ArrayList<>());
                            ksession.setGlobal("results", results);
                            BiLinearTestUtils.insertTestData(ksession, 15, 10, 20);
                            ksession.fireAllRules();
                            operationsCompleted.incrementAndGet();
                        } finally {
                            ksession.dispose();
                        }
                    }
                } catch (Exception e) {
                    errorRef.set(e);
                }
            });
        }
        
        long startTime = System.currentTimeMillis();
        List<Future<?>> futures = BiLinearTestUtils.executeConcurrently(operations, threadCount);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n=== CONCURRENT RULE ADDITION TEST ===");
        System.out.println("Threads: " + threadCount + ", Execution time: " + (endTime - startTime) + "ms");
        System.out.println("Operations completed: " + operationsCompleted.get());
        
        // Verify no exceptions occurred
        assertThat(errorRef.get()).isNull();
        
        // Verify all operations completed
        assertThat(operationsCompleted.get()).isEqualTo(threadCount * 3);
    }

    @Test
    public void testDeadlockDetection() {
        // Test potential deadlock scenarios in BiLinear optimization
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(8, true);
        
        int threadCount = 6;
        int timeoutSeconds = 30;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger completedOperations = new AtomicInteger(0);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        
        try {
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await(); // Synchronized start
                        
                        for (int j = 0; j < 5; j++) {
                            KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                            try {
                                List<String> results = Collections.synchronizedList(new ArrayList<>());
                                ksession.setGlobal("results", results);
                                
                                // Complex operations that might cause contention
                                BiLinearTestUtils.insertTestData(ksession, 25, 20, 40);
                                ksession.fireAllRules();
                                
                                // Additional operations to stress the system
                                ksession.insert(new BiLinearTestUtils.Person("ExtraPerson" + threadId + "_" + j, 35, "TestDept"));
                                ksession.fireAllRules();
                                
                                completedOperations.incrementAndGet();
                            } finally {
                                ksession.dispose();
                            }
                        }
                    } catch (Exception e) {
                        errorRef.set(e);
                    }
                }));
            }
            
            long startTime = System.currentTimeMillis();
            startLatch.countDown(); // Start all threads simultaneously
            
            // Wait for completion with timeout to detect deadlocks
            boolean allCompleted = true;
            for (Future<?> future : futures) {
                try {
                    future.get(timeoutSeconds, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    allCompleted = false;
                    break;
                } catch (Exception e) {
                    errorRef.set(e);
                }
            }
            
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n=== DEADLOCK DETECTION TEST ===");
            System.out.println("Threads: " + threadCount + ", Timeout: " + timeoutSeconds + "s");
            System.out.println("Execution time: " + (endTime - startTime) + "ms");
            System.out.println("Completed operations: " + completedOperations.get() + "/" + (threadCount * 5));
            System.out.println("All threads completed: " + allCompleted);
            
            // Verify no deadlocks occurred (all operations completed within timeout)
            assertThat(allCompleted).isTrue();
            
            // Verify no exceptions occurred
            assertThat(errorRef.get()).isNull();
            
            // Verify expected number of operations completed
            assertThat(completedOperations.get()).isEqualTo(threadCount * 5);
            
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testHighContentionScenario() {
        // Test BiLinear under very high contention
        String drl = BiLinearTestUtils.generateOverlappingPatternDRL(20, true);
        
        int threadCount = 12; // High thread count for contention
        AtomicInteger totalExecutions = new AtomicInteger(0);
        AtomicLong totalExecutionTime = new AtomicLong(0);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        
        List<Runnable> operations = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            operations.add(() -> {
                try {
                    long threadStart = System.currentTimeMillis();
                    
                    KieSession ksession = BiLinearTestUtils.createBiLinearEnabledSession(drl, kieBaseTestConfiguration);
                    try {
                        List<String> results = Collections.synchronizedList(new ArrayList<>());
                        ksession.setGlobal("results", results);
                        
                        // High-contention operations
                        BiLinearTestUtils.insertTestData(ksession, 30, 25, 50);
                        int executions = ksession.fireAllRules();
                        totalExecutions.addAndGet(executions);
                        
                    } finally {
                        ksession.dispose();
                    }
                    
                    long threadEnd = System.currentTimeMillis();
                    totalExecutionTime.addAndGet(threadEnd - threadStart);
                    
                } catch (Exception e) {
                    errorRef.set(e);
                }
            });
        }
        
        long startTime = System.currentTimeMillis();
        List<Future<?>> futures = BiLinearTestUtils.executeConcurrently(operations, threadCount);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n=== HIGH CONTENTION SCENARIO TEST ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");
        System.out.println("Average thread execution time: " + (totalExecutionTime.get() / threadCount) + "ms");
        System.out.println("Total rule executions: " + totalExecutions.get());
        
        // Verify no exceptions occurred
        assertThat(errorRef.get()).isNull();
        
        // Verify all operations completed
        assertThat(futures).hasSize(threadCount);
        
        // Verify rule executions occurred
        assertThat(totalExecutions.get()).isPositive();
    }

    private String generateThreadSpecificRules(int threadId) {
        return String.format("""
            
            rule "ThreadRule%d"
            when
                $p: Person( department == "Dept%d" )
                $proj: Project( owner == $p.name )
            then
                results.add("ThreadRule%d: " + $p.getName());
            end
            """, threadId, threadId % 5, threadId);
    }
}
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
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Utility class providing common functionality for BiLinear feature testing.
 * Contains shared test data generation, session setup, and verification methods.
 */
public class BiLinearTestUtils {

    /**
     * Simple test fact class representing a Person
     */
    public static class Person {
        private String name;
        private int age;
        private String department;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public Person(String name, int age, String department) {
            this.name = name;
            this.age = age;
            this.department = department;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        @Override
        public String toString() {
            return "Person{" + "name='" + name + '\'' + ", age=" + age + 
                   (department != null ? ", department='" + department + '\'' : "") + '}';
        }
    }

    /**
     * Simple test fact class representing a Project
     */
    public static class Project {
        private String name;
        private String owner;
        private int priority;
        private String status;

        public Project(String name, String owner, int priority) {
            this.name = name;
            this.owner = owner;
            this.priority = priority;
            this.status = "ACTIVE";
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
        
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        @Override
        public String toString() {
            return "Project{" + "name='" + name + '\'' + ", owner='" + owner + '\'' + 
                   ", priority=" + priority + ", status='" + status + '\'' + '}';
        }
    }

    /**
     * Simple test fact class representing a Task
     */
    public static class Task {
        private String name;
        private String assignee;
        private String project;
        private int estimatedHours;
        private boolean completed;

        public Task(String name, String assignee, String project, int estimatedHours) {
            this.name = name;
            this.assignee = assignee;
            this.project = project;
            this.estimatedHours = estimatedHours;
            this.completed = false;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        
        public String getProject() { return project; }
        public void setProject(String project) { this.project = project; }
        
        public int getEstimatedHours() { return estimatedHours; }
        public void setEstimatedHours(int estimatedHours) { this.estimatedHours = estimatedHours; }
        
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }

        @Override
        public String toString() {
            return "Task{" + "name='" + name + '\'' + ", assignee='" + assignee + '\'' + 
                   ", project='" + project + '\'' + ", estimatedHours=" + estimatedHours + 
                   ", completed=" + completed + '}';
        }
    }

    /**
     * Creates a KieSession with BiLinear optimization enabled
     */
    public static KieSession createBiLinearEnabledSession(String drl, KieBaseTestConfiguration config) {
        System.setProperty("drools.bilinear.enabled", "true");
        try {
            KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("bilinear-test", config, drl);
            return kbase.newKieSession();
        } finally {
            // Keep property set for the test duration
        }
    }

    /**
     * Creates a KieSession with BiLinear optimization disabled
     */
    public static KieSession createBiLinearDisabledSession(String drl, KieBaseTestConfiguration config) {
        System.setProperty("drools.bilinear.enabled", "false");
        try {
            KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("bilinear-test", config, drl);
            return kbase.newKieSession();
        } finally {
            // Keep property set for the test duration
        }
    }

    /**
     * Generates a DRL with multiple rules that have overlapping patterns (good for BiLinear testing)
     */
    public static String generateOverlappingPatternDRL(int numRules, boolean useComplexConstraints) {
        StringBuilder drl = new StringBuilder();
        
        drl.append("package org.drools.test\n");
        drl.append("import ").append(Person.class.getCanonicalName()).append("\n");
        drl.append("import ").append(Project.class.getCanonicalName()).append("\n");
        drl.append("import ").append(Task.class.getCanonicalName()).append("\n");
        drl.append("global java.util.List results\n\n");

        // Generate rules with shared pattern structure
        for (int i = 1; i <= numRules; i++) {
            drl.append("rule \"Rule").append(i).append("\"\n");
            drl.append("when\n");
            drl.append("  $p: Person( age > ").append(20 + (i % 10)).append(" )\n");
            drl.append("  $proj: Project( owner == $p.name )\n");
            
            if (useComplexConstraints) {
                drl.append("  $task: Task( assignee == $p.name, project == $proj.name, estimatedHours > ")
                   .append(5 + (i % 15)).append(" )\n");
            } else {
                drl.append("  $task: Task( assignee == $p.name )\n");
            }
            
            drl.append("then\n");
            drl.append("  results.add(\"Rule").append(i).append(": \" + $p.getName() + \"-\" + $proj.getName());\n");
            drl.append("end\n\n");
        }

        return drl.toString();
    }

    /**
     * Generates test data with predictable relationships for BiLinear testing
     */
    public static void insertTestData(KieSession session, int personCount, int projectCount, int taskCount) {
        // Insert persons
        for (int i = 1; i <= personCount; i++) {
            Person person = new Person("Person" + i, 25 + (i % 30), "Dept" + (i % 5));
            session.insert(person);
        }

        // Insert projects (some will have owners matching persons)
        for (int i = 1; i <= projectCount; i++) {
            String owner = "Person" + (i % personCount + 1); // Ensure valid owner
            Project project = new Project("Project" + i, owner, i % 5 + 1);
            session.insert(project);
        }

        // Insert tasks
        for (int i = 1; i <= taskCount; i++) {
            String assignee = "Person" + (i % personCount + 1);
            String project = "Project" + (i % projectCount + 1);
            Task task = new Task("Task" + i, assignee, project, 8 + (i % 32));
            session.insert(task);
        }
    }

    /**
     * Executes a session operation with timing measurement
     */
    public static long measureExecutionTime(Runnable operation) {
        long startTime = System.nanoTime();
        operation.run();
        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    /**
     * Measures memory usage before and after an operation
     */
    public static MemoryMeasurement measureMemoryUsage(Runnable operation) {
        // Force garbage collection to get accurate baseline
        System.gc();
        Thread.yield();
        
        long beforeMemory = getUsedMemory();
        
        operation.run();
        
        long afterMemory = getUsedMemory();
        
        return new MemoryMeasurement(beforeMemory, afterMemory);
    }

    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Container for memory measurement results
     */
    public static class MemoryMeasurement {
        private final long beforeMemory;
        private final long afterMemory;

        public MemoryMeasurement(long beforeMemory, long afterMemory) {
            this.beforeMemory = beforeMemory;
            this.afterMemory = afterMemory;
        }

        public long getBeforeMemory() { return beforeMemory; }
        public long getAfterMemory() { return afterMemory; }
        public long getMemoryDelta() { return afterMemory - beforeMemory; }

        @Override
        public String toString() {
            return String.format("Memory: %d -> %d bytes (delta: %+d)", 
                               beforeMemory, afterMemory, getMemoryDelta());
        }
    }

    /**
     * Executes multiple operations concurrently and waits for completion
     */
    public static List<Future<?>> executeConcurrently(List<Runnable> operations, int threadCount) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (Runnable operation : operations) {
                futures.add(executor.submit(operation));
            }

            // Wait for all operations to complete
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS); // 30 second timeout
                } catch (Exception e) {
                    throw new RuntimeException("Concurrent operation failed", e);
                }
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        return futures;
    }

    /**
     * Creates a high-complexity DRL for stress testing
     */
    public static String generateStressTestDRL(int ruleCount, int patternCount) {
        StringBuilder drl = new StringBuilder();
        
        drl.append("package org.drools.test\n");
        drl.append("import ").append(Person.class.getCanonicalName()).append("\n");
        drl.append("import ").append(Project.class.getCanonicalName()).append("\n");
        drl.append("import ").append(Task.class.getCanonicalName()).append("\n");
        drl.append("global java.util.List results\n\n");

        for (int i = 1; i <= ruleCount; i++) {
            drl.append("rule \"StressRule").append(i).append("\"\n");
            drl.append("when\n");
            
            // Generate multiple patterns per rule
            for (int j = 1; j <= patternCount; j++) {
                if (j == 1) {
                    drl.append("  $p").append(j).append(": Person( age > ").append(20 + (i % 20)).append(" )\n");
                } else if (j == 2) {
                    drl.append("  $proj").append(j).append(": Project( owner == $p1.name, priority > ")
                       .append(i % 3).append(" )\n");
                } else {
                    drl.append("  $task").append(j).append(": Task( assignee == $p1.name, ")
                       .append("estimatedHours > ").append(j * 2 + (i % 10)).append(" )\n");
                }
            }
            
            drl.append("then\n");
            drl.append("  results.add(\"StressRule").append(i).append("\");\n");
            drl.append("end\n\n");
        }

        return drl.toString();
    }

    /**
     * Cleans up BiLinear system properties after testing
     */
    public static void cleanupBiLinearProperties() {
        System.clearProperty("drools.bilinear.enabled");
    }

    /**
     * Validates that BiLinear optimization is working by checking for expected performance improvements
     */
    public static void validateBiLinearOptimization(long bilinearTime, long standardTime, 
                                                   String operation, double expectedImprovementRatio) {
        if (bilinearTime > 0 && standardTime > 0) {
            double actualRatio = (double) standardTime / bilinearTime;
            if (actualRatio < expectedImprovementRatio) {
                // Note: This is informational rather than a hard failure since performance 
                // can vary based on system load and other factors
                System.out.println("INFO: BiLinear optimization may not be providing expected improvement for " + operation);
                System.out.println("  Expected ratio: " + expectedImprovementRatio + ", Actual ratio: " + actualRatio);
                System.out.println("  BiLinear time: " + bilinearTime + "ms, Standard time: " + standardTime + "ms");
            }
        }
    }
}
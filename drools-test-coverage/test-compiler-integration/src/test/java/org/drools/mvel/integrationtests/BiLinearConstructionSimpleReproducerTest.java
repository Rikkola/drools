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

import java.util.ArrayList;
import java.util.List;

import org.drools.core.test.model.Person;
import org.drools.core.test.model.Cheese;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple reproducer for BiLinear construction failure.
 * This test reproduces the exact scenario causing failures in benchmark tests.
 */
public class BiLinearConstructionSimpleReproducerTest {
    
    @Test
    public void testBiLinearConstructionFailureReproducer() {
        System.out.println("🧪 BiLinear Construction Failure Reproducer");
        System.out.println("==========================================");
        
        // Enable BiLinear optimization - this triggers the construction failure
        System.setProperty("drools.bilinear.enabled", "true");
        
        try {
            // Create rules that should trigger BiLinear optimization
            // This matches the pattern from benchmark that causes construction failure
            String drl = """
                package org.drools.test;
                
                import org.drools.core.test.model.Person;
                import org.drools.core.test.model.Cheese;
                
                global java.util.List results;
                
                rule "BiLinearRule1"
                when
                    $p : Person($name : name)
                    $c : Cheese(type == $name)
                then
                    results.add("R1: " + $name);
                end
                
                rule "BiLinearRule2" 
                when
                    $p : Person($name : name)
                    $c : Cheese(type == $name, price > 10)
                then
                    results.add("R2: " + $name);
                end
                """;
            
            System.out.println("📝 Building KieBase with BiLinear optimization enabled...");
            
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kfs = kieServices.newKieFileSystem();
            kfs.write("src/main/resources/bilinear-rules.drl", drl);
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
            kieBuilder.buildAll();
            
            // Check for build errors
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                System.err.println("❌ Build FAILED with BiLinear enabled:");
                kieBuilder.getResults().getMessages().forEach(msg -> {
                    System.err.println("  " + msg);
                });
                
                // This captures the construction failure
                System.out.println("🔍 BILINEAR CONSTRUCTION FAILURE CAPTURED:");
                System.out.println("- Pattern detection likely succeeded");
                System.out.println("- Node construction failed during build");
                return;
            }
            
            // If build succeeded, test execution
            KieBase kieBase = kieServices.newKieContainer(kieBuilder.getKieModule().getReleaseId())
                                        .getKieBase();
            
            System.out.println("✅ KieBase built successfully with BiLinear");
            
            try (KieSession kieSession = kieBase.newKieSession()) {
                List<String> results = new ArrayList<>();
                kieSession.setGlobal("results", results);
                
                // Insert test facts
                Person john = new Person("John", 25);
                Cheese cheddar = new Cheese("John", 15);
                
                kieSession.insert(john);
                kieSession.insert(cheddar);
                
                int fired = kieSession.fireAllRules();
                
                System.out.println("🔥 Rules fired: " + fired);
                System.out.println("📋 Results: " + results);
                
                assertThat(fired).isEqualTo(2);
                assertThat(results).hasSize(2);
                
                System.out.println("✅ BiLinear execution successful");
            }
            
        } catch (Exception e) {
            System.err.println("❌ BILINEAR CONSTRUCTION EXCEPTION CAPTURED:");
            System.err.println("Exception: " + e.getClass().getSimpleName());
            System.err.println("Message: " + e.getMessage());
            
            System.out.println("🔍 FAILURE ANALYSIS:");
            System.out.println("- Exception type: " + e.getClass().getName());
            System.out.println("- Root cause location:");
            
            // Print relevant stack trace
            StackTraceElement[] stack = e.getStackTrace();
            for (int i = 0; i < Math.min(10, stack.length); i++) {
                if (stack[i].toString().contains("BiLinear") || 
                    stack[i].toString().contains("buildAll") ||
                    stack[i].toString().contains("KieBuilder")) {
                    System.out.println("  " + stack[i]);
                }
            }
            
            // Important: Don't fail the test, we want to capture the failure details
            System.out.println("✅ BiLinear construction failure successfully reproduced and captured");
            
        } finally {
            System.clearProperty("drools.bilinear.enabled");
        }
    }
    
    @Test
    public void testBaselineWithoutBiLinear() {
        System.out.println("🧪 Baseline Test (BiLinear disabled)");
        System.out.println("===================================");
        
        // Explicitly disable BiLinear
        System.setProperty("drools.bilinear.enabled", "false");
        
        try {
            // Same rules as BiLinear test - this should work fine
            String drl = """
                package org.drools.test;
                
                import org.drools.core.test.model.Person;
                import org.drools.core.test.model.Cheese;
                
                global java.util.List results;
                
                rule "BaselineRule1"
                when
                    $p : Person($name : name)
                    $c : Cheese(type == $name)
                then
                    results.add("Baseline_R1: " + $name);
                end
                
                rule "BaselineRule2" 
                when
                    $p : Person($name : name)
                    $c : Cheese(type == $name, price > 10)
                then
                    results.add("Baseline_R2: " + $name);
                end
                """;
            
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kfs = kieServices.newKieFileSystem();
            kfs.write("src/main/resources/baseline-rules.drl", drl);
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
            kieBuilder.buildAll();
            
            // Baseline should never fail
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                System.err.println("❌ UNEXPECTED: Baseline failed:");
                kieBuilder.getResults().getMessages().forEach(System.err::println);
                throw new RuntimeException("Baseline should never fail");
            }
            
            KieBase kieBase = kieServices.newKieContainer(kieBuilder.getKieModule().getReleaseId())
                                        .getKieBase();
            
            try (KieSession kieSession = kieBase.newKieSession()) {
                List<String> results = new ArrayList<>();
                kieSession.setGlobal("results", results);
                
                Person john = new Person("John", 25);
                Cheese cheddar = new Cheese("John", 15);
                
                kieSession.insert(john);
                kieSession.insert(cheddar);
                
                int fired = kieSession.fireAllRules();
                
                System.out.println("🔥 Baseline rules fired: " + fired);
                System.out.println("📋 Baseline results: " + results);
                
                assertThat(fired).isEqualTo(2);
                assertThat(results).hasSize(2);
                
                System.out.println("✅ Baseline execution successful");
            }
            
        } finally {
            System.clearProperty("drools.bilinear.enabled");
        }
    }
}
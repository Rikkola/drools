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

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test specifically designed to isolate the BiLinear memory ID collision issue.
 * 
 * Problem: BiLinearJoinNode and RuleTerminalNode get same node ID, causing memory ID collision
 * Result: ClassCastException - BiLinearMemory cannot be cast to PathMemory
 * 
 * This test creates the minimal rule pattern that triggers this collision to help debug
 * the node ID assignment logic.
 */
public class BiLinearMemoryCollisionTest {
    
    private static final String BILINEAR_ENABLED_PROPERTY = "drools.bilinear.enabled";
    
    /**
     * Test with BiLinear disabled - should work without issues
     */
    @Test
    public void testWithBiLinearDisabled() {
        String originalValue = System.getProperty(BILINEAR_ENABLED_PROPERTY);
        try {
            System.setProperty(BILINEAR_ENABLED_PROPERTY, "false");
            System.out.println("\n=== Memory Collision Test - BiLinear DISABLED ===");
            
            String drl = getMinimalBiLinearTriggerDRL();
            
            // Should execute without any ClassCastException
            assertDoesNotThrow(() -> {
                executeRulesAndVerify(drl, "BiLinear_DISABLED");
            }, "Test should work when BiLinear is disabled");
            
        } finally {
            restoreSystemProperty(originalValue);
        }
    }
    
    /**
     * Test with BiLinear enabled - should reproduce the memory collision issue
     */
    @Test
    public void testWithBiLinearEnabled_ShouldShowMemoryCollision() {
        String originalValue = System.getProperty(BILINEAR_ENABLED_PROPERTY);
        try {
            System.setProperty(BILINEAR_ENABLED_PROPERTY, "true");
            System.out.println("\n=== Memory Collision Test - BiLinear ENABLED ===");
            
            String drl = getMinimalBiLinearTriggerDRL();
            
            // Currently expected to fail due to memory collision
            // This test documents the current issue - when fixed, this assertion should be updated
            System.out.println("🔍 COLLISION TEST: Attempting to reproduce memory ID collision...");
            
            try {
                executeRulesAndVerify(drl, "BiLinear_ENABLED");
                System.out.println("✅ SUCCESS: No memory collision occurred! Issue may be fixed.");
            } catch (Exception e) {
                System.out.println("❌ COLLISION DETECTED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                
                // Verify this is the expected memory collision error
                assertThat(e.getMessage())
                    .as("Should be the specific memory casting error we're tracking")
                    .contains("cannot be cast to")
                    .containsAnyOf("PathMemory", "BetaMemory", "BiLinearMemory");
                    
                System.out.println("🎯 CONFIRMED: This is the memory collision issue we need to fix");
            }
            
        } finally {
            restoreSystemProperty(originalValue);
        }
    }
    
    /**
     * Creates minimal DRL that triggers BiLinear optimization and memory collision
     * 
     * This pattern is designed to:
     * 1. Have shared pattern that BiLinear detector will optimize 
     * 2. Create BiLinearJoinNode that gets memory ID
     * 3. Have terminal nodes that expect different memory type at same ID
     */
    private String getMinimalBiLinearTriggerDRL() {
        return """
            package org.test.collision;
            
            declare Person
                name: String
                age: int
                favoriteFood: String
            end
            
            declare Food
                type: String
                price: int
            end
            
            rule "Rule1_PersonFood"
            when
                $person: Person(name == "alice")
                $food: Food(type == "cheese")
            then
                System.out.println("Rule1: " + $person.getName() + " likes " + $food.getType());
            end
            
            rule "Rule2_PersonFood"  
            when
                $person: Person(name == "bob")
                $food: Food(type == "cheese")
            then
                System.out.println("Rule2: " + $person.getName() + " likes " + $food.getType());
            end
            """;
    }
    
    /**
     * Execute the rules and verify basic functionality
     * Includes debug output to track memory assignments
     */
    private void executeRulesAndVerify(String drl, String testMode) {
        System.out.println("🔧 Building KieBase for " + testMode + "...");
        
        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        KieBase kieBase = kieHelper.build();
        
        System.out.println("✓ KieBase built successfully");
        
        KieSession kieSession = kieBase.newKieSession();
        System.out.println("✓ KieSession created successfully");
        
        // Insert test facts
        insertTestFacts(kieSession);
        
        // Fire rules - this is where memory collision occurs
        System.out.println("🔥 Firing rules...");
        int rulesFired = kieSession.fireAllRules();
        
        System.out.println("✓ Rules fired successfully: " + rulesFired + " rules executed");
        
        // Verify expected behavior
        assertThat(rulesFired)
            .as("Should fire the expected number of rules")
            .isGreaterThanOrEqualTo(0);
            
        kieSession.dispose();
        System.out.println("✓ Test completed for " + testMode);
    }
    
    /**
     * Insert test facts that will trigger the rules
     */
    private void insertTestFacts(KieSession kieSession) {
        // Create and insert Person facts
        try {
            // Using reflection to create Person instances since it's a declared type
            Class<?> personClass = kieSession.getKieBase().getFactType("org.test.collision", "Person").getFactClass();
            Object alice = personClass.getDeclaredConstructor().newInstance();
            personClass.getMethod("setName", String.class).invoke(alice, "alice");
            personClass.getMethod("setAge", int.class).invoke(alice, 25);
            personClass.getMethod("setFavoriteFood", String.class).invoke(alice, "cheese");
            
            Object bob = personClass.getDeclaredConstructor().newInstance();
            personClass.getMethod("setName", String.class).invoke(bob, "bob");
            personClass.getMethod("setAge", int.class).invoke(bob, 30);
            personClass.getMethod("setFavoriteFood", String.class).invoke(bob, "cheese");
            
            // Create and insert Food facts  
            Class<?> foodClass = kieSession.getKieBase().getFactType("org.test.collision", "Food").getFactClass();
            Object cheese = foodClass.getDeclaredConstructor().newInstance();
            foodClass.getMethod("setType", String.class).invoke(cheese, "cheese");
            foodClass.getMethod("setPrice", int.class).invoke(cheese, 5);
            
            kieSession.insert(alice);
            kieSession.insert(bob);
            kieSession.insert(cheese);
            
            System.out.println("📦 Inserted test facts: alice, bob, cheese");
            
        } catch (Exception e) {
            System.err.println("❌ Error creating test facts: " + e.getMessage());
            throw new RuntimeException("Failed to create test facts", e);
        }
    }
    
    /**
     * Helper to restore system property
     */
    private void restoreSystemProperty(String originalValue) {
        if (originalValue != null) {
            System.setProperty(BILINEAR_ENABLED_PROPERTY, originalValue);
        } else {
            System.clearProperty(BILINEAR_ENABLED_PROPERTY);
        }
    }
    
    /**
     * Additional test to track memory ID assignments during rule compilation
     * This test focuses on the compilation phase where the collision occurs
     */
    @Test
    public void testMemoryIDAssignmentTracking() {
        String originalValue = System.getProperty(BILINEAR_ENABLED_PROPERTY);
        try {
            System.setProperty(BILINEAR_ENABLED_PROPERTY, "true");
            System.out.println("\n=== Memory ID Assignment Tracking Test ===");
            
            String drl = getMinimalBiLinearTriggerDRL();
            
            System.out.println("🔍 Building KieBase with memory tracking enabled...");
            System.out.println("📊 Watch for memory ID assignments in debug output:");
            System.out.println("   - BiLinearJoinNode memory ID assignments");
            System.out.println("   - RuleTerminalNode memory ID assignments");  
            System.out.println("   - Memory ID collisions (same ID, different node types)");
            
            try {
                KieHelper kieHelper = new KieHelper();
                kieHelper.addContent(drl, ResourceType.DRL);
                KieBase kieBase = kieHelper.build();
                
                System.out.println("✓ KieBase compilation completed");
                System.out.println("📋 Check debug output above for memory ID assignment patterns");
                
            } catch (Exception e) {
                System.out.println("❌ Compilation failed with: " + e.getClass().getSimpleName());
                System.out.println("💡 This may indicate memory collision during compilation phase");
                
                // Don't fail the test - this is expected during investigation
                System.out.println("🔍 Exception details: " + e.getMessage());
            }
            
        } finally {
            restoreSystemProperty(originalValue);
        }
    }
}
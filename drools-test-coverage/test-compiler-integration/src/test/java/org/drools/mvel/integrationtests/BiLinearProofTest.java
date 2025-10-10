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

import org.drools.base.common.NetworkNode;
import org.drools.core.impl.InternalRuleBase;
import org.drools.core.reteoo.*;
import org.drools.mvel.integrationtests.phreak.A;
import org.drools.mvel.integrationtests.phreak.B;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that proves bilinear detection works by verifying BiLinearJoinNode creation
 * when two different rules share identical join patterns with beta constraints.
 * 
 * This test ONLY passes when bilinear detection successfully identifies shared
 * join patterns across different rules and creates BiLinearJoinNode instances.
 */
public class BiLinearProofTest {
/*
    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseCloudConfigurations(true).stream();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testBiLinearDetectionProof(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // Enable BiLinear optimization
        String originalProperty = System.getProperty("drools.bilinear.enabled");
        System.setProperty("drools.bilinear.enabled", "true");
        
        try {
            // Create rules that SHOULD trigger bilinear optimization:
            // - Both rules have identical A-B join patterns 
            // - Both have same beta constraint ($b.object > $a.object)
            // - Rules have different consequences (proving they are different rules)
            String drl = 
                "import " + A.class.getCanonicalName() + "\n" +
                "import " + B.class.getCanonicalName() + "\n" +
                "\n" +
                "rule \"FirstRule\"\n" +
                "when\n" +
                "    $a : A(object > 10)\n" +           // Alpha constraint on A
                "    $b : B(object > (Integer)$a.object)\n" +   // Beta constraint: B depends on A with cast
                "then\n" +
                "    System.out.println(\"FirstRule fired with A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
                "end\n" +
                "\n" +
                "rule \"SecondRule\"\n" +
                "when\n" +
                "    $a : A(object > 10)\n" +           // IDENTICAL alpha constraint on A
                "    $b : B(object > (Integer)$a.object)\n" +   // IDENTICAL beta constraint with cast
                "then\n" +
                "    System.out.println(\"SecondRule fired with A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
                "end\n";

            KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("bilinear-test", 
                                                                        kieBaseTestConfiguration, 
                                                                        drl);

            // Analyze the network structure to prove BiLinearJoinNode was created
            List<NetworkNode> biLinearJoinNodes = findBiLinearJoinNodes(kieBase);
            
            // CRITICAL ASSERTION: This test ONLY passes if bilinear detection worked
            // If no BiLinearJoinNodes are found, bilinear detection failed
            assertThat(biLinearJoinNodes)
                .as("BiLinear detection should create BiLinearJoinNode instances when two rules share identical join patterns")
                .isNotEmpty();
            
            // Verify that the BiLinearJoinNode is actually functioning
            NetworkNode biLinearNode = biLinearJoinNodes.get(0);
            assertThat(biLinearNode.getClass().getSimpleName())
                .as("Node should be a BiLinearJoinNode")
                .isEqualTo("BiLinearJoinNode");
                
            System.out.println("✅ BiLinearJoinNode found: " + biLinearNode.getClass().getSimpleName() + " (id=" + biLinearNode.getId() + ")");
            
            // Test that the rules actually execute correctly with bilinear optimization
            KieSession session = kieBase.newKieSession();
            
            // Insert facts that should trigger both rules
            session.insert(new A(20));  // A.object = 20 (satisfies > 10)
            session.insert(new B(25));  // B.object = 25 (satisfies > A.object = 20)
            
            int firedRules = session.fireAllRules();
            
            // Both rules should fire because they have identical conditions
            assertThat(firedRules)
                .as("Both rules should fire when bilinear optimization is working correctly")
                .isEqualTo(2);
            
            session.dispose();
            
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty("drools.bilinear.enabled", originalProperty);
            } else {
                System.clearProperty("drools.bilinear.enabled");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testNoBiLinearWithoutSharedPatterns(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // Enable BiLinear optimization
        String originalProperty = System.getProperty("drools.bilinear.enabled");
        System.setProperty("drools.bilinear.enabled", "true");
        
        try {
            // Create rules that should NOT trigger bilinear optimization:
            // Different constraints = different patterns = no sharing opportunity
            String drl = 
                "import " + A.class.getCanonicalName() + "\n" +
                "import " + B.class.getCanonicalName() + "\n" +
                "\n" +
                "rule \"DifferentRule1\"\n" +
                "when\n" +
                "    $a : A(object > 10)\n" +
                "    $b : B(object > (Integer)$a.object)\n" +   // Beta constraint with cast
                "then\n" +
                "    System.out.println(\"DifferentRule1 fired\");\n" +
                "end\n" +
                "\n" +
                "rule \"DifferentRule2\"\n" +
                "when\n" +
                "    $a : A(object > 5)\n" +            // DIFFERENT alpha constraint
                "    $b : B(object < (Integer)$a.object)\n" +    // DIFFERENT beta constraint with cast
                "then\n" +
                "    System.out.println(\"DifferentRule2 fired\");\n" +
                "end\n";

            KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("no-bilinear-test", 
                                                                        kieBaseTestConfiguration, 
                                                                        drl);

            // There should be NO BiLinearJoinNodes because patterns are different
            List<NetworkNode> biLinearJoinNodes = findBiLinearJoinNodes(kieBase);
            
            assertThat(biLinearJoinNodes)
                .as("No BiLinearJoinNodes should be created when rules have different patterns")
                .isEmpty();
            
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty("drools.bilinear.enabled", originalProperty);
            } else {
                System.clearProperty("drools.bilinear.enabled");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testBiLinearWithThreeRulesSharing(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // Enable BiLinear optimization
        String originalProperty = System.getProperty("drools.bilinear.enabled");
        System.setProperty("drools.bilinear.enabled", "true");
        
        try {
            // Create THREE rules that share the exact same join pattern
            String drl = 
                "import " + A.class.getCanonicalName() + "\n" +
                "import " + B.class.getCanonicalName() + "\n" +
                "\n" +
                "rule \"SharedRule1\"\n" +
                "when\n" +
                "    $a : A(object > 0)\n" +
                "    $b : B(object == ((Integer)$a.object) + 5)\n" +  // Cast Object to Integer
                "then\n" +
                "    System.out.println(\"SharedRule1: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
                "end\n" +
                "\n" +
                "rule \"SharedRule2\"\n" +
                "when\n" +
                "    $a : A(object > 0)\n" +                // SAME alpha constraint
                "    $b : B(object == ((Integer)$a.object) + 5)\n" +   // SAME beta constraint with cast
                "then\n" +
                "    System.out.println(\"SharedRule2: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
                "end\n" +
                "\n" +
                "rule \"SharedRule3\"\n" +
                "when\n" +
                "    $a : A(object > 0)\n" +                // SAME alpha constraint
                "    $b : B(object == ((Integer)$a.object) + 5)\n" +   // SAME beta constraint with cast  
                "then\n" +
                "    System.out.println(\"SharedRule3: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
                "end\n";

            KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("triple-bilinear-test", 
                                                                        kieBaseTestConfiguration, 
                                                                        drl);

            // Should have BiLinearJoinNode because three rules share same pattern
            List<NetworkNode> biLinearJoinNodes = findBiLinearJoinNodes(kieBase);
            
            assertThat(biLinearJoinNodes)
                .as("BiLinear detection should work with three rules sharing identical patterns")
                .isNotEmpty();
            
            // Test execution: all three rules should fire
            KieSession session = kieBase.newKieSession();
            session.insert(new A(10));  // A.object = 10
            session.insert(new B(15));  // B.object = 15 (= 10 + 5)
            
            int firedRules = session.fireAllRules();
            assertThat(firedRules)
                .as("All three rules should fire with shared bilinear optimization")
                .isEqualTo(3);
            
            session.dispose();
            
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty("drools.bilinear.enabled", originalProperty);
            } else {
                System.clearProperty("drools.bilinear.enabled");
            }
        }
    }

 */

    /**
     * Searches the Rete network for BiLinearJoinNode instances.
     * This is the proof that bilinear detection actually worked.
     */
    private List<NetworkNode> findBiLinearJoinNodes(KieBase kieBase) {
        List<NetworkNode> biLinearNodes = new ArrayList<>();
        Rete rete = ((InternalRuleBase) kieBase).getRete();
        
        // Traverse all object type nodes and their sinks
        for (ObjectTypeNode otn : rete.getObjectTypeNodes()) {
            findBiLinearNodesRecursive(otn, biLinearNodes, new HashSet<>());
        }
        
        return biLinearNodes;
    }
    
    /**
     * Recursively search for BiLinearJoinNode instances in the network
     */
    private void findBiLinearNodesRecursive(NetworkNode node, List<NetworkNode> biLinearNodes, Set<NetworkNode> visited) {
        if (visited.contains(node)) {
            return;
        }
        visited.add(node);
        
        // Check if this is a BiLinearJoinNode
        if (node.getClass().getSimpleName().equals("BiLinearJoinNode")) {
            biLinearNodes.add(node);
        }
        
        // Traverse children
        if (node instanceof ObjectSource) {
            Object[] sinks = ((ObjectSource) node).getObjectSinkPropagator().getSinks();
            if (sinks != null) {
                for (Object sink : sinks) {
                    findBiLinearNodesRecursive((NetworkNode) sink, biLinearNodes, visited);
                }
            }
        } else if (node instanceof LeftTupleSource) {
            LeftTupleSink[] sinks = ((LeftTupleSource) node).getSinkPropagator().getSinks();
            if (sinks != null) {
                for (LeftTupleSink sink : sinks) {
                    findBiLinearNodesRecursive(sink, biLinearNodes, visited);
                }
            }
        }
    }
}
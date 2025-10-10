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
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;

import java.util.*;

/**
 * Simple debug test to highlight the BiLinear pattern matching issue.
 * This shows exactly why detection works but construction fails.
 */
public class BiLinearDebugTest {

    @Test
    public void debugBiLinearPatternMismatch() {
        // Enable BiLinear optimization
        String originalProperty = System.getProperty("drools.bilinear.enabled");
        System.setProperty("drools.bilinear.enabled", "true");
        
        try {
            System.out.println("\n=== BILINEAR PATTERN MATCHING DEBUG ===");
            
            // Simple DRL with two identical rules
            String drl = 
                "import " + A.class.getCanonicalName() + "\n" +
                "import " + B.class.getCanonicalName() + "\n" +
                "\n" +
                "rule \"Rule1\"\n" +
                "when\n" +
                "    $a : A(object > 10)\n" +
                "    $b : B(object > (Integer)$a.object)\n" +
                "then\n" +
                "    System.out.println(\"Rule1 fired\");\n" +
                "end\n" +
                "\n" +
                "rule \"Rule2\"\n" +
                "when\n" +
                "    $a : A(object > 10)\n" +           // IDENTICAL pattern
                "    $b : B(object > (Integer)$a.object)\n" +   // IDENTICAL pattern
                "then\n" +
                "    System.out.println(\"Rule2 fired\");\n" +
                "end\n";

            // Create KieBase - this triggers BiLinear detection and construction
            KieBaseTestConfiguration config = TestParametersUtil2.getKieBaseCloudConfigurations(true).iterator().next();
            KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("debug-test", config, drl);

            // Check if BiLinearJoinNodes were created
            List<NetworkNode> biLinearJoinNodes = findBiLinearJoinNodes(kieBase);
            
            System.out.println("\n=== RESULTS ===");
            System.out.println("BiLinearJoinNodes found: " + biLinearJoinNodes.size());
            
            if (biLinearJoinNodes.isEmpty()) {
                System.out.println("❌ ISSUE: BiLinear detection worked but no BiLinearJoinNodes created");
                System.out.println("   This indicates a pattern matching problem between detection and construction phases");
            } else {
                System.out.println("✅ SUCCESS: BiLinearJoinNodes created successfully");
                for (NetworkNode node : biLinearJoinNodes) {
                    System.out.println("   - " + node.getClass().getSimpleName() + " (id=" + node.getId() + ")");
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty("drools.bilinear.enabled", originalProperty);
            } else {
                System.clearProperty("drools.bilinear.enabled");
            }
        }
    }

    /**
     * Find BiLinearJoinNode instances in the network
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
     * Recursively search for BiLinearJoinNode instances
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
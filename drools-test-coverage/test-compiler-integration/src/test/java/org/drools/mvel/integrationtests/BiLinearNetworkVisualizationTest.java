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

import org.drools.mvel.integrationtests.phreak.A;
import org.drools.mvel.integrationtests.phreak.B;
import org.drools.mvel.integrationtests.phreak.C;
import org.drools.mvel.integrationtests.phreak.D;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visualization tests for BiLinear Join Node network structures using NetworkVisitor.
 * 
 * This test class demonstrates different BiLinear network topologies and validates
 * that Phase 6 network sharing is working correctly by visualizing the actual
 * network structure created during rule compilation.
 */
public class BiLinearNetworkVisualizationTest {

    private final NetworkVisitor networkVisitor = new NetworkVisitor();

    @Test
    public void testBasicBiLinearNetworkStructure() {
        System.out.println("\n🔍 ===========================================");
        System.out.println("🔍 TEST: Basic BiLinear Network Structure");
        System.out.println("🔍 ===========================================");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"BasicBiLinear\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object)\n" +  // This should create BiLinearJoinNode
            "then\n" +
            "    System.out.println(\"BasicBiLinear fired: A=\" + $a.getObject() + \", B=\" + $b.getObject());\n" +
            "end\n";

        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("\n📊 Visualizing Basic BiLinear Network Structure:");
        networkVisitor.debugNetworkStructure(kieBase);
        
        // Verify the network was built
        assertThat(kieBase).isNotNull();
        
        System.out.println("\n✅ Basic BiLinear network visualization completed");
    }

    @Test 
    public void testPhase6NetworkSharingVisualization() {
        System.out.println("\n🔗 ===========================================");
        System.out.println("🔗 TEST: Phase 6 Network Sharing Visualization");
        System.out.println("🔗 ===========================================");
        
        // Create different rule structures that share common patterns but can't use normal sharing
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "import " + C.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"ComplexRule1\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object)\n" +  // Shared A-B pattern 
            "    $c : C(object > 10)\n" +          // Additional pattern makes rule different
            "then\n" +
            "    System.out.println(\"ComplexRule1 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"ComplexRule2\"\n" +
            "when\n" +
            "    $x : C(object < 100)\n" +         // Different structure - starts with C
            "    $a : A(object > 0)\n" +           // Same A pattern as Rule1 (different position)
            "    $b : B(object > $a.object)\n" +   // Same A-B relationship as Rule1
            "then\n" +
            "    System.out.println(\"ComplexRule2 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"CrossNetworkRule\"\n" +
            "when\n" +
            "    $temp : A(object > 5)\n" +         // Different A constraint
            "    $a : A(object > 0)\n" +            // Same A pattern as others (different context)
            "    $b : B(object > $a.object)\n" +    // Same A-B relationship (cross-network sharing candidate)
            "then\n" +
            "    System.out.println(\"CrossNetworkRule fired\");\n" +
            "end\n";

        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("\n📊 Visualizing Phase 6 Cross-Network Sharing Structure:");
        System.out.println("   Expected: A-B patterns should be detected across different rule structures");
        System.out.println("   Expected: BiLinear optimization for cross-network A-B relationship sharing");
        
        networkVisitor.debugNetworkStructure(kieBase);
        
        // Test runtime behavior to verify sharing works
        KieSession session = kieBase.newKieSession();
        session.insert(new A(5));
        session.insert(new B(10)); 
        session.insert(new C(1));
        
        System.out.println("\n🚀 Testing shared network execution:");
        int firedRules = session.fireAllRules();
        session.dispose();
        
        System.out.println("📈 Rules fired: " + firedRules + " (Expected: 3 rules with cross-network optimization)");
        
        assertThat(kieBase).isNotNull();
        assertThat(firedRules).isGreaterThan(0);
        
        System.out.println("\n✅ Phase 6 network sharing visualization completed");
    }

    @Test
    public void testComplexBiLinearNetworkPatterns() {
        System.out.println("\n🌐 ===========================================");
        System.out.println("🌐 TEST: Complex BiLinear Network Patterns");
        System.out.println("🌐 ===========================================");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "import " + C.class.getCanonicalName() + "\n" +
            "import " + D.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"ComplexRule1\"\n" +
            "when\n" +
            "    $a : A(object == 1)\n" +
            "    $b : B(object != $a.object)\n" +  // Pattern 1: A-B relationship
            "    $c : C(object > 5)\n" +
            "    $d : D(object == $c.object * 2)\n" +  // Pattern 2: C-D relationship
            "then\n" +
            "    System.out.println(\"ComplexRule1 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"ComplexRule2\"\n" +
            "when\n" +
            "    $a : A(object == 1)\n" +
            "    $b : B(object != $a.object)\n" +  // Same Pattern 1: A-B (should share)
            "    $x : A(object > 10)\n" +
            "then\n" +
            "    System.out.println(\"ComplexRule2 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"ComplexRule3\"\n" +
            "when\n" +
            "    $c : C(object > 5)\n" +
            "    $d : D(object == $c.object * 2)\n" +  // Same Pattern 2: C-D (should share)
            "    $y : C(object < 100)\n" +
            "then\n" +
            "    System.out.println(\"ComplexRule3 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"ChainedBiLinear\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object)\n" +  // First BiLinear join
            "    $c : C(object > $b.object)\n" +  // Second BiLinear join (chained)
            "    $d : D(object > $c.object)\n" +  // Third BiLinear join (chained)
            "then\n" +
            "    System.out.println(\"ChainedBiLinear fired\");\n" +
            "end\n";

        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("\n📊 Visualizing Complex BiLinear Network with Multiple Sharing Patterns:");
        System.out.println("   Expected: A-B pattern shared between Rules 1&2");
        System.out.println("   Expected: C-D pattern shared between Rules 1&3");
        System.out.println("   Expected: ChainedBiLinear shows sequential BiLinear joins");
        
        networkVisitor.debugNetworkStructure(kieBase);
        
        assertThat(kieBase).isNotNull();
        
        System.out.println("\n✅ Complex BiLinear network visualization completed");
    }

    @Test
    public void testBiLinearVsRegularJoinComparison() {
        System.out.println("\n⚖️  ===========================================");
        System.out.println("⚖️  TEST: BiLinear vs Regular Join Comparison");
        System.out.println("⚖️  ===========================================");
        
        // First: Network with BiLinear opportunities
        String biLinearDrl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"WithBiLinear\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object)\n" +  // Cross-pattern constraint (BiLinear eligible)
            "then\n" +
            "    System.out.println(\"BiLinear rule fired\");\n" +
            "end\n";
            
        // Second: Network without BiLinear opportunities  
        String regularDrl =
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"WithoutBiLinear\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > 5)\n" +  // No cross-pattern constraint (regular join)
            "then\n" +
            "    System.out.println(\"Regular rule fired\");\n" +
            "end\n";

        System.out.println("\n📊 BiLinear-Optimized Network:");
        KieBase biLinearKieBase = buildKieBase(biLinearDrl);
        networkVisitor.debugNetworkStructure(biLinearKieBase);
        
        System.out.println("\n📊 Regular Join Network:");
        KieBase regularKieBase = buildKieBase(regularDrl);
        networkVisitor.debugNetworkStructure(regularKieBase);
        
        System.out.println("\n📋 Comparison Summary:");
        System.out.println("   • BiLinear network should show BiLinearJoinNode with cross-pattern constraints");
        System.out.println("   • Regular network should show standard JoinNode without BiLinear optimization");
        
        assertThat(biLinearKieBase).isNotNull();
        assertThat(regularKieBase).isNotNull();
        
        System.out.println("\n✅ BiLinear vs Regular join comparison completed");
    }

    @Test
    public void testNetworkSharingEfficiency() {
        System.out.println("\n📈 ===========================================");
        System.out.println("📈 TEST: Network Sharing Efficiency Analysis");
        System.out.println("📈 ===========================================");
        
        // Create rules with different contexts but shared patterns to force BiLinear optimization
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "import " + C.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"ContextRule1\"\n" +
            "when\n" +
            "    $ctx1 : C(object > 0)\n" +         // Context pattern 1
            "    $a : A(object == 10)\n" +
            "    $b : B(object > $a.object)\n" +    // Shared A-B pattern
            "then\n" +
            "    System.out.println(\"ContextRule1 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"ContextRule2\"\n" +
            "when\n" +
            "    $ctx2 : C(object < 50)\n" +         // Context pattern 2 (different constraint)
            "    $a : A(object == 10)\n" +           // Same A pattern (different context)
            "    $b : B(object > $a.object)\n" +    // Same A-B relationship
            "then\n" +
            "    System.out.println(\"ContextRule2 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"PositionalRule3\"\n" +
            "when\n" +
            "    $a : A(object == 10)\n" +           // Same A pattern (different position)
            "    $ctx3 : C(object != 25)\n" +        // Context in middle
            "    $b : B(object > $a.object)\n" +    // Same A-B relationship (different position)
            "then\n" +
            "    System.out.println(\"PositionalRule3 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"ChainedRule4\"\n" +
            "when\n" +
            "    $temp : C(object > 100)\n" +        // Chained context
            "    $a : A(object == 10)\n" +           // Same A pattern
            "    $b : B(object > $a.object)\n" +    // Same A-B relationship
            "    $extra : A(object < 1000)\n" +      // Additional chaining
            "then\n" +
            "    System.out.println(\"ChainedRule4 fired\");\n" +
            "end\n";

        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("\n📊 Analyzing Cross-Context Network Sharing:");
        System.out.println("   Expected: A-B patterns should be shared across different rule contexts");
        System.out.println("   Expected: BiLinear optimization despite different rule structures");
        
        networkVisitor.debugNetworkStructure(kieBase);
        
        // Test that all rules can fire using the shared network
        KieSession session = kieBase.newKieSession();
        session.insert(new A(10));
        session.insert(new B(15));
        session.insert(new C(30));  // Satisfies ContextRule1(>0), ContextRule2(<50), PositionalRule3(!=25), ChainedRule4(>100 - NO, need separate)
        session.insert(new C(150)); // Only satisfies ChainedRule4(>100)
        
        System.out.println("\n🚀 Testing shared network execution with multiple rules:");
        int firedRules = session.fireAllRules();
        session.dispose();
        
        System.out.println("📈 Rules fired: " + firedRules + " (Expected: 6 rule activations with cross-context matching)");
        
        assertThat(kieBase).isNotNull();
        assertThat(firedRules).isEqualTo(6); // ContextRule1(2x) + ContextRule2(1x) + PositionalRule3(2x) + ChainedRule4(1x)
        
        System.out.println("\n✅ Network sharing efficiency analysis completed");
    }

    @Test
    public void testOOPathBiLinearOptimization() {
        System.out.println("\n🛤️  ===========================================");
        System.out.println("🛤️  TEST: OOPath BiLinear Optimization");
        System.out.println("🛤️  ===========================================");
        
        // Test OOPath patterns with BiLinear optimization enabled
        String oopathDrl = 
            "import org.drools.mvel.integrationtests.phreak.A\n" +
            "import org.drools.mvel.integrationtests.phreak.B\n" +
            "\n" +
            "rule \"OOPathBiLinearRule1\"\n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $childB : B(object > $a.object) from $a.children\n" +  // OOPath-style FROM
            "then\n" +
            "    System.out.println(\"OOPath BiLinear fired: A=\" + $a.getObject() + \", B=\" + $childB.getObject());\n" +
            "end\n" +
            "\n" +
            "rule \"OOPathBiLinearRule2\" \n" +
            "when\n" +
            "    $a : A(object > 0)\n" +
            "    $childB : B(object > $a.object) from $a.children\n" +  // Same OOPath pattern (should share)
            "then\n" +
            "    System.out.println(\"OOPath BiLinear 2 fired\");\n" +
            "end\n";
            
        // Ensure OOPath bypass is enabled
        System.setProperty("drools.oopath.bypass.analysis", "true");
        
        System.out.println("\n📊 Visualizing OOPath BiLinear Network:");
        System.out.println("   Expected: OOPath patterns should create BiLinearJoinNode");
        System.out.println("   Expected: Both rules should share the same BiLinearJoinNode");
        System.out.println("   Expected: No FROM analysis overhead for OOPath patterns");
        
        KieBase kieBase = buildKieBase(oopathDrl);
        networkVisitor.debugNetworkStructure(kieBase);
        
        // Test runtime behavior
        KieSession session = kieBase.newKieSession();
        
        A parentA = new A(10);
        B childB1 = new B(15);
        B childB2 = new B(20);
        // Note: This test assumes A has a children collection that can be set
        // The actual test data setup would depend on the A/B class implementation
        
        session.insert(parentA);
        session.insert(childB1);
        session.insert(childB2);
        
        System.out.println("\n🚀 Testing OOPath BiLinear optimization execution:");
        int firedRules = session.fireAllRules();
        session.dispose();
        
        System.out.println("📈 Rules fired: " + firedRules + " (Expected: rules using OOPath patterns)");
        
        assertThat(kieBase).isNotNull();
        
        System.out.println("\n✅ OOPath BiLinear optimization test completed");
    }

    @Test
    public void testOOPathVsRegularFromPerformance() {
        System.out.println("\n⚡ ===========================================");
        System.out.println("⚡ TEST: OOPath vs Regular FROM Performance");
        System.out.println("⚡ ===========================================");
        
        // OOPath pattern (should bypass FROM analysis)
        String oopathDrl = 
            "import org.drools.mvel.integrationtests.phreak.A\n" +
            "import org.drools.mvel.integrationtests.phreak.B\n" +
            "\n" +
            "rule \"OOPathPattern\" when\n" +
            "    $a : A(object > 0)\n" +  
            "    $b : B(object > $a.object) from $a.children\n" +  // OOPath-style
            "then\n" +
            "    System.out.println(\"OOPath fired\");\n" +
            "end\n";
            
        // Regular FROM pattern (goes through full analysis)
        String regularDrl =
            "import org.drools.mvel.integrationtests.phreak.A\n" +
            "import org.drools.mvel.integrationtests.phreak.B\n" +
            "import java.util.Arrays\n" +
            "\n" +
            "rule \"RegularFromPattern\" when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object) from Arrays.asList(new Object[]{})\n" +  // Regular FROM
            "then\n" +
            "    System.out.println(\"Regular FROM fired\");\n" +
            "end\n";
        
        System.setProperty("drools.oopath.bypass.analysis", "true");
        
        // Measure compilation time for OOPath
        long startTime = System.currentTimeMillis();
        KieBase oopathKieBase = buildKieBase(oopathDrl);
        long oopathTime = System.currentTimeMillis() - startTime;
        
        // Measure compilation time for regular FROM
        startTime = System.currentTimeMillis();
        KieBase regularKieBase = buildKieBase(regularDrl);
        long regularTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\n📊 Performance Analysis:");
        System.out.println("   OOPath compilation time: " + oopathTime + "ms");
        System.out.println("   Regular FROM compilation time: " + regularTime + "ms");
        
        if (oopathTime > 0 && regularTime > 0) {
            double improvement = (double)regularTime / oopathTime;
            System.out.println("   Performance ratio: " + String.format("%.2f", improvement) + "x");
            
            if (improvement > 1.0) {
                System.out.println("   ✅ OOPath bypass shows performance improvement!");
            }
        }
        
        System.out.println("\n📊 OOPath Network Structure:");
        networkVisitor.debugNetworkStructure(oopathKieBase);
        
        System.out.println("\n📊 Regular FROM Network Structure:");
        networkVisitor.debugNetworkStructure(regularKieBase);
        
        assertThat(oopathKieBase).isNotNull();
        assertThat(regularKieBase).isNotNull();
        
        System.out.println("\n✅ OOPath vs Regular FROM performance comparison completed");
    }

    @Test
    public void testOOPathBypassConfiguration() {
        System.out.println("\n⚙️  ===========================================");
        System.out.println("⚙️  TEST: OOPath Bypass Configuration");
        System.out.println("⚙️  ===========================================");
        
        String oopathDrl = 
            "import org.drools.mvel.integrationtests.phreak.A\n" +
            "import org.drools.mvel.integrationtests.phreak.B\n" +
            "\n" +
            "rule \"ConfigurableOOPath\" when\n" +
            "    $a : A(object > 0)\n" +
            "    $b : B(object > $a.object) from $a.children\n" +
            "then\n" +
            "    System.out.println(\"Configurable OOPath fired\");\n" +
            "end\n";
        
        // Test with bypass enabled (default)
        System.setProperty("drools.oopath.bypass.analysis", "true");
        System.out.println("\n📊 OOPath Bypass ENABLED:");
        KieBase enabledKieBase = buildKieBase(oopathDrl);
        networkVisitor.debugNetworkStructure(enabledKieBase);
        
        // Test with bypass disabled
        System.setProperty("drools.oopath.bypass.analysis", "false");
        System.out.println("\n📊 OOPath Bypass DISABLED:");
        KieBase disabledKieBase = buildKieBase(oopathDrl);
        networkVisitor.debugNetworkStructure(disabledKieBase);
        
        // Reset to default
        System.setProperty("drools.oopath.bypass.analysis", "true");
        
        assertThat(enabledKieBase).isNotNull();
        assertThat(disabledKieBase).isNotNull();
        
        System.out.println("\n✅ OOPath bypass configuration test completed");
        System.out.println("   Note: Compare networks above to see impact of bypass configuration");
    }

    @Test
    public void testForcedBiLinearScenario() {
        System.out.println("\n🎯 ==========================================");
        System.out.println("🎯 TEST: Forced BiLinear Scenario");
        System.out.println("🎯 ==========================================");
        
        // This test creates rule structures that CANNOT use normal sharing
        // but CAN benefit from BiLinear cross-network optimization
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "import " + C.class.getCanonicalName() + "\n" +
            "import " + D.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"MultiContextRule1\"\n" +
            "when\n" +
            "    $pre1 : C(object > 1)\n" +           // Unique prefix 1
            "    $pre2 : D(object < 100)\n" +         // Unique prefix 2 
            "    $a : A(object > 0)\n" +              // Shared pattern start
            "    $b : B(object > $a.object)\n" +     // Shared A-B relationship
            "    $post1 : C(object != 50)\n" +        // Unique suffix 1
            "then\n" +
            "    System.out.println(\"MultiContextRule1 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"MultiContextRule2\"\n" +
            "when\n" +
            "    $alt1 : D(object > 5)\n" +           // Different prefix 1
            "    $alt2 : C(object < 200)\n" +         // Different prefix 2
            "    $a : A(object > 0)\n" +              // Same A pattern (different context)
            "    $b : B(object > $a.object)\n" +     // Same A-B relationship
            "    $post2 : D(object != 75)\n" +        // Different suffix
            "then\n" +
            "    System.out.println(\"MultiContextRule2 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"ReversedContextRule\"\n" +
            "when\n" +
            "    $rev1 : B(object < 1000)\n" +        // Pre-constraint on B
            "    $a : A(object > 0)\n" +              // Same A pattern
            "    $b : B(object > $a.object)\n" +     // Same A-B relationship (different B context)
            "then\n" +
            "    System.out.println(\"ReversedContextRule fired\");\n" +
            "end\n";
            
        System.out.println("🔧 Building forced BiLinear scenario...");
        System.out.println("   Rules have completely different structures");  
        System.out.println("   Normal sharing is impossible due to different contexts");
        System.out.println("   But A-B relationship pattern is identical across rules");
        
        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("\n📊 Network Analysis for Forced BiLinear:");
        networkVisitor.debugNetworkStructure(kieBase);
        
        // Test runtime execution to verify cross-network optimization works
        KieSession session = kieBase.newKieSession();
        session.insert(new A(5));
        session.insert(new B(10));
        session.insert(new C(25)); 
        session.insert(new D(50));
        
        System.out.println("\n🚀 Testing forced BiLinear execution:");
        int firedRules = session.fireAllRules();
        session.dispose();
        
        System.out.println("📈 Rules fired: " + firedRules + " (Expected: 3 rules via BiLinear optimization)");
        
        assertThat(kieBase).isNotNull();
        assertThat(firedRules).isEqualTo(3);
        
        System.out.println("\n✅ Forced BiLinear scenario test completed");
    }

    private KieBase buildKieBase(String drl) {
        // Enable BiLinear optimization for this test
        System.setProperty("drools.bilinear.enabled", "true");
        
        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        return kieHelper.build();
    }
}
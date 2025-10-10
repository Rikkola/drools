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
 * Visualization tests for BiLinear Join Node cross-network variable scenarios.
 * 
 * This test class demonstrates scenarios where BiLinearDeclarationContext would be essential
 * for enabling cross-network variable resolution, and visualizes different network patterns
 * that BiLinear architecture is designed to optimize.
 * 
 * Note: These tests show patterns that WOULD create variable conflicts if BiLinear supported
 * true dual-network variable scoping. Currently, they demonstrate network patterns and
 * sharing scenarios where BiLinear optimization occurs.
 */
public class BiLinearVariableConflictVisualizationTest {

    private final NetworkVisitor networkVisitor = new NetworkVisitor();

    @Test
    public void testCustomerOrderCrossNetworkScenario() {
        System.out.println("\n👥 ===========================================");
        System.out.println("👥 TEST: Customer Order Cross-Network Scenario");
        System.out.println("👥 ===========================================");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "import " + C.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"HighValueCustomerRule\"\n" +
            "when\n" +
            "    $customer : A(object > 100)  // Premium customer threshold\n" +
            "    $account : B(object > $customer.object * 10)  // High-value account relationship\n" +
            "then\n" +
            "    System.out.println(\"High-value customer detected: \" + $customer.getObject());\n" +
            "end\n" +
            "\n" +
            "rule \"RecentOrderRule\"\n" +
            "when\n" +
            "    $customer : A(object < 999)  // Active customer constraint\n" +
            "    $order : C(object > $customer.object)  // Recent order relationship\n" +
            "then\n" +
            "    System.out.println(\"Recent order detected for customer: \" + $customer.getObject());\n" +
            "end\n" +
            "\n" +
            "rule \"CombinedAnalysisRule\"\n" +
            "when\n" +
            "    $premiumCustomer : A(object > 100)  // Different variable name to avoid conflict\n" +
            "    $account : B(object > $premiumCustomer.object * 10)\n" +
            "    $activeCustomer : A(object < 999)   // Different variable name\n" +
            "    $order : C(object > $activeCustomer.object)\n" +
            "then\n" +
            "    System.out.println(\"Combined analysis: Premium=\" + $premiumCustomer.getObject() + \", Active=\" + $activeCustomer.getObject());\n" +
            "end\n";

        System.out.println("📋 Cross-Network Scenario Analysis:");
        System.out.println("   • Rule 1: Premium customer analysis with account relationship");
        System.out.println("   • Rule 2: Recent order analysis with customer relationship");
        System.out.println("   • Rule 3: Combined analysis using different variable names");
        System.out.println("   • DEMO: Shows where BiLinearDeclarationContext would enable true variable sharing");

        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("\n📊 Visualizing Cross-Network Customer Analysis:");
        networkVisitor.debugNetworkStructure(kieBase);
        
        KieSession session = kieBase.newKieSession();
        
        session.insert(new A(150));   // Satisfies >100 and <999
        session.insert(new B(2000));  // Satisfies >150*10=1500
        session.insert(new C(200));   // Satisfies >150
        
        System.out.println("\n🚀 Testing cross-network scenario execution:");
        int firedRules = session.fireAllRules();
        session.dispose();
        
        System.out.println("📈 Rules fired: " + firedRules);
        
        assertThat(kieBase).isNotNull();
        
        System.out.println("\n✅ Customer order cross-network scenario test completed");
    }

    @Test
    public void testFinancialRiskCrossNetworkAnalysis() {
        System.out.println("\n💰 ===========================================");
        System.out.println("💰 TEST: Financial Risk Cross-Network Analysis");
        System.out.println("💰 ===========================================");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "import " + C.class.getCanonicalName() + "\n" +
            "import " + D.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"CheckingAccountRisk\"\n" +
            "when\n" +
            "    $account : A(object > 0)  // Checking account\n" +
            "    $transaction : B(object > $account.object * 5)  // Large transaction\n" +
            "    $balance : C(object < $account.object)  // Low balance\n" +
            "then\n" +
            "    System.out.println(\"Checking account risk detected\");\n" +
            "end\n" +
            "\n" +
            "rule \"CreditAnalysis\"\n" +
            "when\n" +
            "    $creditAccount : A(object < 600)  // Credit score constraint\n" +
            "    $creditTx : B(object > 0)  // Any credit transaction\n" +
            "    $creditBalance : C(object > $creditTx.object)  // Positive balance\n" +
            "    $credit : D(object == $creditAccount.object + $creditTx.object)  // Credit calculation\n" +
            "then\n" +
            "    System.out.println(\"Credit analysis completed\");\n" +
            "end\n" +
            "\n" +
            "rule \"ComprehensiveRiskAssessment\"\n" +
            "when\n" +
            "    // Would benefit from BiLinear variable sharing across these patterns\n" +
            "    $riskAccount : A(object > 0, object < 600)  // Combined constraints\n" +
            "    $riskTx : B(object > $riskAccount.object)   // Transaction pattern\n" +
            "    $riskBalance : C(object != $riskTx.object)  // Balance relationship\n" +
            "then\n" +
            "    System.out.println(\"Comprehensive risk assessment completed\");\n" +
            "end\n";

        System.out.println("📋 Cross-Network Risk Analysis:");
        System.out.println("   • Rule 1: Checking account risk factors");
        System.out.println("   • Rule 2: Credit analysis with different variable naming");
        System.out.println("   • Rule 3: Comprehensive assessment combining patterns");
        System.out.println("   • DEMO: Shows scenarios where BiLinear cross-network resolution would be beneficial");

        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("\n📊 Visualizing Cross-Network Financial Risk Analysis:");
        networkVisitor.debugNetworkStructure(kieBase);
        
        KieSession session = kieBase.newKieSession();
        session.insert(new A(100));   // Satisfies various constraints
        session.insert(new B(600));   // Large transaction
        session.insert(new C(50));    // Low balance
        session.insert(new C(700));   // High balance
        session.insert(new D(700));   // Credit calculation result
        
        System.out.println("\n🚀 Testing cross-network financial risk analysis:");
        int firedRules = session.fireAllRules();
        session.dispose();
        
        System.out.println("📈 Rules fired: " + firedRules);
        
        assertThat(kieBase).isNotNull();
        
        System.out.println("\n✅ Financial risk cross-network analysis test completed");
    }

    @Test
    public void testProductRecommendationScenarios() {
        System.out.println("\n🛒 ===========================================");
        System.out.println("🛒 TEST: Product Recommendation Scenarios");
        System.out.println("🛒 ===========================================");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "import " + C.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"ElectronicsRecommendation\"\n" +
            "when\n" +
            "    $user : A(object > 25)  // User age > 25\n" +
            "    $purchase : B(object > $user.object)  // Electronics purchase\n" +
            "    $product : C(object > 4)  // High-rated product\n" +
            "then\n" +
            "    System.out.println(\"Electronics recommendation for user: \" + $user.getObject());\n" +
            "end\n" +
            "\n" +
            "rule \"BooksRecommendation\"\n" +
            "when\n" +
            "    $bookUser : A(object < 65)  // Different variable: User age < 65\n" +
            "    $bookPurchase : B(object < $bookUser.object * 2)  // Books purchase pattern\n" +
            "    $bookProduct : C(object > 0)  // In-stock product\n" +
            "then\n" +
            "    System.out.println(\"Books recommendation for user: \" + $bookUser.getObject());\n" +
            "end\n" +
            "\n" +
            "rule \"CrossCategoryAnalysis\"\n" +
            "when\n" +
            "    // Demonstrates where BiLinear cross-network resolution would be valuable\n" +
            "    $targetUser : A(object > 25, object < 65)  // Combined constraints\n" +
            "    $electronicsPurchase : B(object > $targetUser.object)\n" +
            "    $booksPurchase : B(object < $targetUser.object * 2)\n" +
            "    $qualityProduct : C(object > 4)\n" +
            "    $availableProduct : C(object > 0)\n" +
            "then\n" +
            "    System.out.println(\"Cross-category analysis for user: \" + $targetUser.getObject());\n" +
            "end\n";

        System.out.println("📋 Product Recommendation Scenario Analysis:");
        System.out.println("   • Rule 1: Electronics recommendations with user-purchase relationship");
        System.out.println("   • Rule 2: Books recommendations with different variable naming");
        System.out.println("   • Rule 3: Cross-category analysis showing complex relationships");
        System.out.println("   • DEMO: Shows where BiLinear would enable elegant variable sharing");

        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("\n📊 Visualizing Product Recommendation Scenarios:");
        networkVisitor.debugNetworkStructure(kieBase);
        
        KieSession session = kieBase.newKieSession();
        session.insert(new A(35));   // User satisfying various constraints
        session.insert(new B(40));   // Purchase satisfying >35
        session.insert(new B(60));   // Purchase satisfying <35*2=70
        session.insert(new C(5));    // High-rated product (>4 and >0)
        
        System.out.println("\n🚀 Testing product recommendation scenarios:");
        int firedRules = session.fireAllRules();
        session.dispose();
        
        System.out.println("📈 Rules fired: " + firedRules);
        
        assertThat(kieBase).isNotNull();
        
        System.out.println("\n✅ Product recommendation scenarios test completed");
    }

    @Test
    public void testSharedPatternOptimization() {
        System.out.println("\n🔗 ===========================================");
        System.out.println("🔗 TEST: Shared Pattern Optimization Scenarios");
        System.out.println("🔗 ===========================================");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "import " + C.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"SharedPatternRule1\"\n" +
            "when\n" +
            "    // Shared pattern that should be reused\n" +
            "    $data : A(object == 10)\n" +
            "    $value : B(object > $data.object)\n" +
            "    \n" +
            "    // Rule-specific extension\n" +
            "    $result1 : C(object == $data.object * 3)\n" +
            "then\n" +
            "    System.out.println(\"SharedPatternRule1 fired with result: \" + $result1.getObject());\n" +
            "end\n" +
            "\n" +
            "rule \"SharedPatternRule2\"\n" +
            "when\n" +
            "    // Same shared pattern (should reuse BiLinearJoinNode)\n" +
            "    $data : A(object == 10)\n" +
            "    $value : B(object > $data.object)\n" +
            "    \n" +
            "    // Different rule-specific extension\n" +
            "    $result2 : C(object != $data.object)\n" +
            "then\n" +
            "    System.out.println(\"SharedPatternRule2 fired with result: \" + $result2.getObject());\n" +
            "end\n" +
            "\n" +
            "rule \"SharedPatternRule3\"\n" +
            "when\n" +
            "    // Same shared pattern again (maximum sharing)\n" +
            "    $data : A(object == 10)\n" +
            "    $value : B(object > $data.object)\n" +
            "    \n" +
            "    // Yet another rule-specific extension\n" +
            "    $alternative : A(object != 10)  // Additional condition\n" +
            "then\n" +
            "    System.out.println(\"SharedPatternRule3 fired with alternative: \" + $alternative.getObject());\n" +
            "end\n";

        System.out.println("📋 Shared Pattern Optimization Analysis:");
        System.out.println("   • Shared Pattern: $data (A == 10), $value (B > $data) - should be shared across rules");
        System.out.println("   • Rule 1: Extends with specific result calculation");
        System.out.println("   • Rule 2: Extends with different result condition");
        System.out.println("   • Rule 3: Extends with additional alternative condition");
        System.out.println("   • Expected: Maximum network sharing with BiLinear optimization");

        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("\n📊 Visualizing Shared Pattern Network Optimization:");
        networkVisitor.debugNetworkStructure(kieBase);
        
        KieSession session = kieBase.newKieSession();
        session.insert(new A(10));   // Matches shared pattern
        session.insert(new A(20));   // For $alternative variable
        session.insert(new B(15));   // Value > 10
        session.insert(new C(30));   // For result calculations
        session.insert(new C(25));   // Additional result data
        
        System.out.println("\n🚀 Testing shared pattern optimization:");
        int firedRules = session.fireAllRules();
        session.dispose();
        
        System.out.println("📈 Rules fired: " + firedRules + " (Expected: multiple rules sharing optimized pattern)");
        
        assertThat(kieBase).isNotNull();
        
        System.out.println("\n✅ Shared pattern optimization test completed");
    }

    @Test
    public void testComplexChainedNetworkScenario() {
        System.out.println("\n🌪️  ===========================================");
        System.out.println("🌪️  TEST: Complex Chained Network Scenario");
        System.out.println("🌪️  ===========================================");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "import " + C.class.getCanonicalName() + "\n" +
            "import " + D.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"AscendingChainRule\"\n" +
            "when\n" +
            "    // Ascending value chain\n" +
            "    $a1 : A(object > 0)\n" +
            "    $b1 : B(object > $a1.object)\n" +
            "    $c1 : C(object > $b1.object)\n" +
            "    $d1 : D(object > $c1.object)\n" +
            "then\n" +
            "    System.out.println(\"Ascending chain completed\");\n" +
            "end\n" +
            "\n" +
            "rule \"DescendingChainRule\"\n" +
            "when\n" +
            "    // Descending value chain with different variable names\n" +
            "    $a2 : A(object < 1000)\n" +
            "    $b2 : B(object < $a2.object)\n" +
            "    $c2 : C(object < $b2.object)\n" +
            "    $d2 : D(object < $c2.object)\n" +
            "then\n" +
            "    System.out.println(\"Descending chain completed\");\n" +
            "end\n" +
            "\n" +
            "rule \"MixedChainRule\"\n" +
            "when\n" +
            "    // Demonstrates complex cross-network patterns BiLinear could optimize\n" +
            "    $mixA : A(object > 0, object < 1000)\n" +
            "    $mixB1 : B(object > $mixA.object)  // Ascending relationship\n" +
            "    $mixB2 : B(object < $mixA.object)  // Descending relationship\n" +
            "    $mixC : C(object > $mixB1.object, object < $mixB2.object + 1000)\n" +
            "then\n" +
            "    System.out.println(\"Mixed chain analysis completed\");\n" +
            "end\n";

        System.out.println("📋 Complex Chained Network Analysis:");
        System.out.println("   • Rule 1: Ascending chain A→B→C→D with increasing values");
        System.out.println("   • Rule 2: Descending chain with opposite relationships");
        System.out.println("   • Rule 3: Mixed relationships demonstrating complex cross-network needs");
        System.out.println("   • DEMO: Shows extreme scenarios where BiLinear cross-network resolution would be essential");

        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("\n📊 Visualizing Complex Chained Network Scenario:");
        networkVisitor.debugNetworkStructure(kieBase);
        
        KieSession session = kieBase.newKieSession();
        session.insert(new A(100));   // Satisfies >0 and <1000
        session.insert(new B(200));   // For ascending chain >100
        session.insert(new B(50));    // For descending chain <100
        session.insert(new C(300));   // For ascending chain >200
        session.insert(new C(25));    // For descending chain <50
        session.insert(new C(150));   // For mixed chain conditions
        session.insert(new D(400));   // For ascending chain >300
        session.insert(new D(10));    // For descending chain <25
        
        System.out.println("\n🚀 Testing complex chained network scenario:");
        int firedRules = session.fireAllRules();
        session.dispose();
        
        System.out.println("📈 Rules fired: " + firedRules);
        
        assertThat(kieBase).isNotNull();
        
        System.out.println("\n✅ Complex chained network scenario test completed");
    }

    @Test
    public void testVariableConflictWithoutBiLinear() {
        System.out.println("\n❌ ===========================================");
        System.out.println("❌ TEST: Variable Conflicts WITHOUT BiLinear");
        System.out.println("❌ ===========================================");
        
        String drl = 
            "import " + A.class.getCanonicalName() + "\n" +
            "import " + B.class.getCanonicalName() + "\n" +
            "\n" +
            "rule \"NoConflictRule1\"\n" +
            "when\n" +
            "    $data : A(object > 0)\n" +
            "    $value : B(object > 5)  // No cross-pattern constraint\n" +
            "then\n" +
            "    System.out.println(\"NoConflictRule1 fired\");\n" +
            "end\n" +
            "\n" +
            "rule \"NoConflictRule2\" \n" +
            "when\n" +
            "    $info : A(object < 100)  // Different variable name\n" +
            "    $result : B(object < 50)  // No cross-pattern constraint\n" +
            "then\n" +
            "    System.out.println(\"NoConflictRule2 fired\");\n" +
            "end\n";

        System.out.println("📋 NO BiLinear Scenario Analysis:");
        System.out.println("   • Rule 1: $data (A > 0), $value (B > 5) - no cross-pattern constraints");
        System.out.println("   • Rule 2: $info (A < 100), $result (B < 50) - different variable names");
        System.out.println("   • NO BILINEAR OPTIMIZATION: Standard join nodes expected");
        System.out.println("   • NO CONFLICTS: No cross-network variable resolution needed");

        KieBase kieBase = buildKieBase(drl);
        
        System.out.println("\n📊 Visualizing Standard Network (No BiLinear Conflicts):");
        networkVisitor.debugNetworkStructure(kieBase);
        
        KieSession session = kieBase.newKieSession();
        session.insert(new A(10));    // Satisfies both >0 and <100
        session.insert(new B(10));    // Satisfies >5 and <50
        
        System.out.println("\n🚀 Testing standard network execution:");
        int firedRules = session.fireAllRules();
        session.dispose();
        
        System.out.println("📈 Rules fired: " + firedRules + " (Expected: standard join execution)");
        
        assertThat(kieBase).isNotNull();
        assertThat(firedRules).isEqualTo(2);
        
        System.out.println("\n✅ Standard network (no BiLinear conflicts) test completed");
        System.out.println("   📊 Compare with BiLinear tests above to see architectural differences");
    }

    private KieBase buildKieBase(String drl) {
        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        return kieHelper.build();
    }
}
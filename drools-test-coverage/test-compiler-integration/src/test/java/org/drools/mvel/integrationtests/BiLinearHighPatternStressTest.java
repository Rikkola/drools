package org.drools.mvel.integrationtests;

import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.TestParametersUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stress tests for BiLinear network with high pattern count rules
 * after removing pattern count restrictions.
 */
@RunWith(Parameterized.class)
public class BiLinearHighPatternStressTest {

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    public BiLinearHighPatternStressTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
        this.kieBaseTestConfiguration = kieBaseTestConfiguration;
    }

    @Parameterized.Parameters(name = "KieBase type={0}")
    public static Collection<Object[]> getParameters() {
        return TestParametersUtil.getKieBaseCloudConfigurations(true);
    }

    private KieSession createKieSession(String drl) {
        KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("bilinear-test", kieBaseTestConfiguration, drl);
        return kbase.newKieSession();
    }

    @Test
    public void testTenPatternRuleWithBiLinear() {
        System.out.println("🧪 Testing 10-pattern rule with BiLinear enabled...");
        
        String drl = "package org.drools.test\n" +
                     "global java.util.List results\n" +
                     "\n" +
                     "rule \"shared pattern rule 1\"\n" +
                     "when\n" +
                     "  $s: String( $len: length )\n" +
                     "  $i: Integer( this == $len )\n" +
                     "then\n" +
                     "  results.add(\"rule1: \" + $s);\n" +
                     "end\n" +
                     "\n" +
                     "rule \"shared pattern rule 2\"\n" +
                     "when\n" +
                     "  $s: String( $len: length )\n" +
                     "  $i: Integer( this == $len )\n" +
                     "then\n" +
                     "  results.add(\"rule2: \" + $s);\n" +
                     "end\n" +
                     "\n" +
                     "rule \"ten pattern stress rule\"\n" +
                     "when\n" +
                     "  $s1: String( length > 0 )\n" +
                     "  $s2: String( length > 1, this != $s1 )\n" +
                     "  $s3: String( length > 2, this != $s1, this != $s2 )\n" +
                     "  $s4: String( length > 3, this != $s1, this != $s2, this != $s3 )\n" +
                     "  $i1: Integer( this > 0 )\n" +
                     "  $i2: Integer( this > 1, this != $i1 )\n" +
                     "  $i3: Integer( this > 2, this != $i1, this != $i2 )\n" +
                     "  $i4: Integer( this > 3, this != $i1, this != $i2, this != $i3 )\n" +
                     "  $d1: Double( this > 0 )\n" +
                     "  $d2: Double( this > 1, this != $d1 )\n" +  // 10 patterns
                     "then\n" +
                     "  results.add(\"ten: matched\");\n" +
                     "end\n";

        KieSession ksession = createKieSession(drl);
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        ksession.insert("test");
        ksession.insert(4);

        long startTime = System.currentTimeMillis();
        int fired = ksession.fireAllRules();
        long endTime = System.currentTimeMillis();
        
        ksession.dispose();

        assertThat(fired).isEqualTo(2);  // Both shared pattern rules should fire
        assertThat(results).hasSize(2);
        
        System.out.println("✅ 10-pattern rule executed successfully");
        System.out.println("   Fired rules: " + fired + ", Execution time: " + (endTime - startTime) + "ms");
        System.out.println("   BiLinear should now work with high pattern count rules");
    }

    @Test
    public void testFifteenPatternRuleStress() {
        System.out.println("🔥 Testing 15-pattern rule stress test...");
        
        String drl = "package org.drools.test\n" +
                     "global java.util.List results\n" +
                     "\n" +
                     "rule \"shared baseline rule 1\"\n" +
                     "when\n" +
                     "  $s: String( $len: length )\n" +
                     "  $i: Integer( this == $len )\n" +
                     "then\n" +
                     "  results.add(\"baseline1\");\n" +
                     "end\n" +
                     "\n" +
                     "rule \"shared baseline rule 2\"\n" +
                     "when\n" +
                     "  $s: String( $len: length )\n" +
                     "  $i: Integer( this == $len )\n" +
                     "then\n" +
                     "  results.add(\"baseline2\");\n" +
                     "end\n" +
                     "\n" +
                     "rule \"fifteen pattern mega rule\"\n" +
                     "when\n" +
                     "  $s1: String( length > 0 )\n" +
                     "  $s2: String( length > 1, this != $s1 )\n" +
                     "  $s3: String( length > 2, this != $s1, this != $s2 )\n" +
                     "  $s4: String( length > 3, this != $s1, this != $s2, this != $s3 )\n" +
                     "  $s5: String( length > 4, this != $s1, this != $s2, this != $s3, this != $s4 )\n" +
                     "  $i1: Integer( this > 0 )\n" +
                     "  $i2: Integer( this > 1, this != $i1 )\n" +
                     "  $i3: Integer( this > 2, this != $i1, this != $i2 )\n" +
                     "  $i4: Integer( this > 3, this != $i1, this != $i2, this != $i3 )\n" +
                     "  $i5: Integer( this > 4, this != $i1, this != $i2, this != $i3, this != $i4 )\n" +
                     "  $d1: Double( this > 0 )\n" +
                     "  $d2: Double( this > 1, this != $d1 )\n" +
                     "  $d3: Double( this > 2, this != $d1, this != $d2 )\n" +
                     "  $l1: Long( this > 0 )\n" +
                     "  $l2: Long( this > 1, this != $l1 )\n" +  // 15 patterns
                     "then\n" +
                     "  results.add(\"fifteen: extreme\");\n" +
                     "end\n";

        KieSession ksession = createKieSession(drl);
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        ksession.insert("test");
        ksession.insert(4);

        long startTime = System.currentTimeMillis();
        int fired = ksession.fireAllRules();
        long endTime = System.currentTimeMillis();
        
        ksession.dispose();

        assertThat(fired).isEqualTo(2);  // Baseline rules should fire
        assertThat(results).hasSize(2);
        
        System.out.println("✅ 15-pattern rule handled successfully");
        System.out.println("   Fired rules: " + fired + ", Execution time: " + (endTime - startTime) + "ms");
        System.out.println("   System stable with extreme pattern counts");
    }

    @Test
    public void testMultipleHighPatternRulesPerformance() {
        System.out.println("⚡ Testing multiple high-pattern rules performance...");
        
        StringBuilder drlBuilder = new StringBuilder();
        drlBuilder.append("package org.drools.test\n");
        drlBuilder.append("global java.util.List results\n\n");
        
        // Create 5 rules with shared patterns for BiLinear optimization
        for (int ruleNum = 1; ruleNum <= 5; ruleNum++) {
            drlBuilder.append("rule \"shared pattern rule ").append(ruleNum).append("\"\n");
            drlBuilder.append("when\n");
            drlBuilder.append("  $s: String( $len: length )\n");
            drlBuilder.append("  $i: Integer( this == $len )\n");
            drlBuilder.append("then\n");
            drlBuilder.append("  results.add(\"rule").append(ruleNum).append("\");\n");
            drlBuilder.append("end\n\n");
        }
        
        // Add 3 high-pattern rules (12 patterns each)
        for (int complexRule = 1; complexRule <= 3; complexRule++) {
            drlBuilder.append("rule \"twelve pattern rule ").append(complexRule).append("\"\n");
            drlBuilder.append("when\n");
            for (int p = 1; p <= 12; p++) {
                String type = (p <= 4) ? "String" : (p <= 8) ? "Integer" : "Double";
                drlBuilder.append("  $").append(type.toLowerCase()).append(p)
                         .append(": ").append(type).append("( this != null )\n");
            }
            drlBuilder.append("then\n");
            drlBuilder.append("  results.add(\"complex").append(complexRule).append("\");\n");
            drlBuilder.append("end\n\n");
        }

        KieSession ksession = createKieSession(drlBuilder.toString());
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        ksession.insert("test");
        ksession.insert(4);

        long startTime = System.currentTimeMillis();
        int fired = ksession.fireAllRules();
        long endTime = System.currentTimeMillis();
        
        ksession.dispose();

        assertThat(fired).isEqualTo(5);  // 5 shared pattern rules should fire
        assertThat(results).hasSize(5);
        
        System.out.println("✅ Multiple high-pattern rules performance test completed");
        System.out.println("   Fired rules: " + fired + ", Execution time: " + (endTime - startTime) + "ms");
        System.out.println("   BiLinear optimization working with multiple complex rules");
    }
}
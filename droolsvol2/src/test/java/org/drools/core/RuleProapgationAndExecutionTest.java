package org.drools.core;

import org.drools.api.data.DataStore;
import org.junit.jupiter.api.Test;

public class RuleProapgationAndExecutionTest {
    @Test
    public void test0() {
        RuleBase ruleBase = new RuleBase();

        record DS(DataStore<Person> persons) {};

        RuleBuilder<DS> builder = new RuleBuilder<>();


        RuleBaseModifier.with(ruleBase)
                        .apply(RuleBaseModifier.changeSet()
                                               .selectPackage("org.domain").selectUnit("Unit1")
                                               .add(builder.rule("r1").ifn(() -> {System.out.println("hello");})));
    }
}

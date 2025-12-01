package org.drools.core;

import org.drools.api.data.DataStore;
import org.drools.core.RuleBuilderTest.Person;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class RuleBaseTest {

    @Test
    public void test1() {
        RuleBase ruleBase = new RuleBase();

        record DS(DataStore<?> ds1) {};

        RuleBuilder<DS> builder = new RuleBuilder<>();

        ruleBase.modify().with("org.domain")
                .add(builder.rule("r1").ifn(() -> {}))
                .add(builder.rule("r2").ifn(() -> {}));
    }

    @Test
    public void test2() {
        RuleBase ruleBase = new RuleBase();

        record DS(DataStore<?> ds1) {};

        record P3(String p3_1, String p3_2, String p3_3) {
            public static final P3 V = new P3(null,null,null);
        };

        DataStore<Object> ds = new DefaultDataStore<>();

        DataStore<Person> persons = ds.as(Person.class);

        RuleBuilder<DS> builder = new RuleBuilder<>();

        ruleBase.modify().with("org.domain")
                .add(builder.rule("rule1").<P3>params()
                .join(builder.from(persons).filter((ctx, b) -> b.age() > 20))
                .filter((ctx, a, b) -> a.p3_1().length() > b.age()))
                .apply();
    }


}

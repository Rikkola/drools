package org.drools.core;

import org.drools.api.data.DataStore;
import org.drools.core.RuleBuilderTest.DS;
import org.drools.core.RuleBuilderTest.P3;

public class TestRules {
    record DS(DataStore<Person> persons,
              DataStore<Library> libraries,
              DataStore<Object> misc) {};

    record P3(String p3_1, String p3_2, String p3_3) {
        public static final RuleBuilderTest.P3 V = new RuleBuilderTest.P3(null, null, null);
    };

    record Path (Library library, Room room, Shelf shelf, Book book, Page page) {}

    public void test3JOINS() {
        RuleBase ruleBase = new RuleBase();

        RuleBuilder<DS> builder = new RuleBuilder<>();

        RuleBaseModifier.with(ruleBase)
                        .apply(RuleBaseModifier.changeSet()
                                               .selectPackage("org.domain").selectUnit("Unit1")
                                               .add(builder.rule("Rule1").from(DS::persons)
                                                           .ifn( (ctx, p) -> System.out.println(p))));
    }
}

package org.drools.core;

import org.drools.api.data.DataStore;
import org.junit.jupiter.api.Test;

import java.util.HashMap;


public class RuleBuilderTest {

    public void test1() {
        record DS(DataStore<?> ds1) {};

        RuleBuilder<DS> builder = new RuleBuilder<>();

        DataStore<Object> ds = new DefaultDataStore<>();

        DataStore<Person> persons = ds.as(Person.class);

        record P3(String p3_1, String p3_2, String p3_3) {
            public static final P3 V = new P3(null,null,null);
        };

        builder.rule("rule1").<P3>params()
               .fn( (ctx, p) -> System.out.println(p.p3_1));

        builder.rule("rule1").<P3>params()
               .join(builder.from(persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> a.p3_1().length() > b.age)
               .join(builder.from(persons).filter(((ctx, c) -> c.age() > 20)))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);

        builder.rule("rule1").param("name", String.class).param("age", int.class)
               .join(builder.from(persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> ((String)a.get(0)).length() > b.age)
               .join(builder.from(persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);

        builder.rule("rule1").map().param("name", String.class).param("age", int.class)
               .join(builder.from(persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> ((String)a.get("name")).length() > b.age)
               .join(builder.from(persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);

        builder.rule("rule1").from(persons).filter((ctx, a) -> a.age() > 20)
               .join(builder.from(persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> a.age() > b.age)
               .join(builder.from(persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);

        builder.rule("rule1")
               .<Object>param("p1")
               .<Object>param("p2")
               .join(builder.from(persons).filter((ctx, b) -> b.age() > 20))
               .join(builder.from(persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);
    }

    public void test2() {
        RuleBuilder builder = new RuleBuilder();

        DataStore<Object> ds = new DefaultDataStore<>();

        DataStore<Person> persons = ds.as(Person.class);

        record P3(String p3_1, String p3_2, String p3_3) {
            public static final P3 V = new P3(null,null,null);
        };
    }

    public record Person(String name, int age) {

    }

    public record Man(){

    }

    @Test
    public void testType() {
        HashMap<String, Integer> map = new X<Object>().step1().load("xxx", null);
        System.out.println(map);
    }

    public static class X<A> {
        public X<A> step1() {
            return new X();
        }

        public <T> T load(String str, Type<T> type) {
            try {
                return type.type.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public <T> T load(String str, Type<T> type) {
        try {
            return type.type.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static <T> Type<T> type(T... array) {
        return new Type(array.getClass().getComponentType());
    }

    public static class Type<T> {
        private Class<T> type;

        public Type(Class<T> type) {
            this.type = type;
        }
    }

}

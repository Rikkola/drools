package org.drools.core;

import org.drools.api.data.DataStore;
import org.drools.core.OOPathTest.Book;
import org.drools.core.OOPathTest.Page;
import org.drools.core.OOPathTest.Room;
import org.drools.core.OOPathTest.Shelf;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;


public class RuleBuilderTest {

    public void test1() {
        record DS(DataStore<?> ds1) {};

        RuleBuilder<DS> builder = new RuleBuilder<>();

        DataStore<Object> ds = new DefaultDataStore<>();

        DataStore<Person> persons = ds.as(Person.class);

        DataStore<Library> libraries = ds.as(Library.class);

        record P3(String p3_1, String p3_2, String p3_3) {
            public static final P3 V = new P3(null,null,null);
        };

        builder.rule("rule1")
               .fn( (ctx) -> System.out.println("hello"));

        builder.rule("rule1")
               .from(libraries).filter((ctx, b) -> b.name() != null)
               //.path()
               .fn( (ctx, b) -> System.out.println(b.name()));

        builder.rule("rule1").<P3>params()
               .join(builder.from(persons).filter((ctx, b) -> b.age() > 20))
               .fn( (ctx, b, c) -> System.out.println("hello"));

        builder.rule("rule1").<P3>params()
               .fn( (ctx, p) -> System.out.println(p.p3_1));

        builder.rule("rule1").<P3>params()
               .join(builder.from(persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> a.p3_1().length() > b.age)
               .join(builder.from(persons).filter(((ctx, c) -> c.age() > 20)))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0)
               .fn( (ctx, a, b, c) -> System.out.println(a.p3_1));;

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

    public void testPath() {
        record DS(DataStore<?> ds1) {};

        RuleBuilder<DS> builder = new RuleBuilder<>();

        DataStore<Object> ds = new DefaultDataStore<>();

        builder.rule("rule1").<Library>params()
               .<Room, Page>path((ctx,l) -> l.rooms(), (ctx, r) -> r.name() != null)
               .<Shelf>path((ctx, r) -> r.shelves(), (ctx, s) -> s.name() != null )
               .<Book>path((ctx, s) -> s.books(), (ctx, b) -> b.title() != null)
               .<Page>path((ctx, b) -> b.pages(), (ctx, p) -> p.content() != null).endPath()
        .filter( (ctx, b, c) -> c.root().name() != c.leaf().content());

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

    record Library(String name, List<OOPathTest.Room> rooms) {
        public String toString() {
            return "Library[name=" + name +"]";
        }
    }

    record Room(String name, List<OOPathTest.Shelf> shelves) {
        public String toString() {
            return "Room[name=" + name +"]";
        }
    }

    record Shelf(String name, List<OOPathTest.Book> books) {
        public String toString() {
            return "Shelf[name=" + name +"]";
        }
    }

    record Book(String title, List<OOPathTest.Page> pages) {
        public String toString() {
            return "Book[name=" + title +"]";
        }
    }

    record Page(int number, String content) {
        public String toString() {
            return "Page[number=" + number +"]";
        }
    }
}

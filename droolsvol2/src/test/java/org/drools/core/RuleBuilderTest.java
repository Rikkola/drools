package org.drools.core;

import org.drools.api.data.DataStore;
import org.drools.core.function.Function1;
import org.drools.core.function.Function2;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RuleBuilderTest {


    public void test3JOINS() {
        record DS(DataStore<Person> persons,
                  DataStore<OOPathTest.Library> libraries) {};

        RuleBuilder<DS> builder = new RuleBuilder<>();

        record P3(String p3_1, String p3_2, String p3_3) {
            public static final P3 V = new P3(null,null,null);
        };

        builder.rule("rule1").<P3>params()
               .join(builder.from(DS::persons))
               .join(builder.from(DS::persons))
               .join(builder.from(DS::persons));
               //.join(builder.from(DS::persons))
    }

    public void test1() {
        record DS(DataStore<Person> persons,
                  DataStore<OOPathTest.Library> libraries) {};

        RuleBuilder<DS> builder = new RuleBuilder<>();

        record P3(String p3_1, String p3_2, String p3_3) {
            public static final P3 V = new P3(null,null,null);
        };

        builder.rule("rule1")
               .fn( (ctx) -> System.out.println("hello"));

        builder.rule("rule1")
               .from(DS::libraries).filter((ctx, b) -> b.name() != null)
               //.path()
               .fn( (ctx, b) -> System.out.println(b.name()));

        builder.rule("rule1").<P3>params()
               .join(builder.from(DS::persons).filter((ctx, b) -> b.age() > 20))
               .fn( (ctx, b, c) -> System.out.println("hello"));

        builder.rule("rule1").<P3>params()
               .fn( (ctx, p) -> System.out.println(p.p3_1));

        builder.rule("rule1").<P3>params()
               .join(builder.from(DS::persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> a.p3_1().length() > b.age)
               .join(builder.from(DS::persons).filter(((ctx, c) -> c.age() > 20)))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0)
               .fn( (ctx, a, b, c) -> System.out.println(a.p3_1));;

        builder.rule("rule1").param("name", String.class).param("age", int.class)
               .join(builder.from(DS::persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> ((String)a.get(0)).length() > b.age)
               .join(builder.from(DS::persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);

        builder.rule("rule1").map().param("name", String.class).param("age", int.class)
               .join(builder.from(DS::persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> ((String)a.get("name")).length() > b.age)
               .join(builder.from(DS::persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);

        builder.rule("rule1").from(DS::persons).filter((ctx, a) -> a.age() > 20)
               .join(builder.from(DS::persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> a.age() > b.age)
               .join(builder.from(DS::persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);

        builder.rule("rule1")
               .<Object>param("p1")
               .<Object>param("p2")
               .join(builder.from(DS::persons).filter((ctx, b) -> b.age() > 20))
               .join(builder.from(DS::persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);
    }

    public void testPath() {
        record DS(DataStore<?> ds1) {};

        RuleBuilder<DS> builder = new RuleBuilder<>();

        DataStore<Object> ds = new PropagatingDataStore<>(0, new TypeIndexer<>());

        builder.rule("rule1").<Library>params()
               .<Room, Shelf, Book, Page>path5((ctx,l) -> l.rooms(), (ctx, r) -> r.name() != null)
               .path((ctx, r) -> r.shelves(), (ctx, s) -> s.name() != null )
               .path((ctx, s) -> s.books(), (ctx, b) -> b.title() != null)
               .path((ctx, b) -> b.pages(), (ctx, p) -> p.content() != null)
        .filter( (ctx, b, c) -> c.getA().name() != c.getE().content());

    }

    public void testPath2() {
        record DS(DataStore<Person> persons,
                  DataStore<OOPathTest.Library> libraries) {};

        RuleBuilder<DS> builder = new RuleBuilder<>();

        record Path (Library library, Room room, Shelf shelf, Book book, Page page) {}

        builder.rule("rule1").<Library>params()
               .<Room, Shelf>path3()
               .path( (ctx, l) -> l.rooms(), (ctx, r) -> r.name() != null)
               .path( (ctx, r) -> r.shelves(), (ctx, s) -> s.name() != null)
               .filter((ctx, a, t) -> a.name() != t.getC().name());

        builder.rule("rule1").<Library>params()
               .<Room>path2()
               .path( (ctx, a) -> a.rooms(), (ctx, b) -> b.name() != null)
               .filter((ctx, a, t) -> a.name() != t.getB().name());

        builder.rule("rule1").from(DS::persons)
               .join(builder.from(DS::libraries))
               .<Room, Shelf, Book, Page>path5((ctx,l) -> l.rooms(), (ctx, r) -> r.name() != null)
               .path((ctx, r) -> r.shelves(), (ctx, s) -> s.name() != null )
               .path((ctx, s) -> s.books(), (ctx, b) -> b.title() != null)
               .path((ctx, b) -> b.pages(), (ctx, p) -> p.content() != null)
               .filter( (ctx, p, c, d) -> p.age() <= d.getD().pages().size())
               .filter( (ctx, p, c, d) -> p.age() <= d.<Path>as().book().pages().size());

    }

    @Test
    public void testNot() {
        record DS(DataStore<Person> persons,
                  DataStore<OOPathTest.Library> libraries,
                  DataStore<Object> misc) {};

        RuleBuilder<DS> builder = new RuleBuilder<>();

        record P3(String p3_1, String p3_2, String p3_3) {
            public static final P3 V = new P3(null,null,null);
        };

        builder.rule("rule1").<P3>params()
                   .join(builder.from(DS::persons).filter((ctx, b) -> b.age() > 20))
                   .not()
                       .join(builder.from(DS::misc)
                                    .<Map<String, Person>>type()
                                    .filter((ctx, p) -> p.get("xxx").age() > 20))
                       .join(builder.from(DS::libraries))
                   .end()
                   .fn( (a, b, c) -> System.out.println(a.ds() + b.p3_1 + c.name()))
               .end();
    }

    record DS(DataStore<Person> ds1) {};

    @Test
    public void testDataSource() {
        DS ds = new DS(new PropagatingDataStore<>(0, new TypeIndexer<>()));

        ContextPojoDS pojoCtx = new ContextPojoDS(ds);

        ContextMapDS mapCtx = new ContextMapDS();

        Function1<DS, DataStore<Person>> x = DS::ds1;

        Function2<Map, String, DataStore<?>> y = Map<String, DataStore<?>>::get;
    }


    public record Person(String name, int age) {

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

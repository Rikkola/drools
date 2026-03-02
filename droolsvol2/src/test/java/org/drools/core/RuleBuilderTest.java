package org.drools.core;

import org.drools.api.data.DataStore;
import org.drools.core.function.Function1;
import org.drools.core.function.Function2;
import org.junit.jupiter.api.Test;

import java.util.Map;


public class RuleBuilderTest {
    record Ctx(DataStore<Person> persons,
               DataStore<Library> libraries,
               DataStore<Object> misc) {};

    record Params3(String p3_1, String p3_2, String p3_3) {
        public static final Params3 V = new Params3(null, null, null);
    };

    record Path (Library library, Room room, Shelf shelf, Book book, Page page) {}

    public void test3JoinsVariousUses() {
        RuleBuilder<Ctx> builder = new RuleBuilder<>();

        builder.rule("rule1")
               .from(builder.from(Ctx::persons))
               .join(builder.from(Ctx::persons))
               .join(builder.from(Ctx::persons));

        builder.rule("rule1")
               .from(Ctx::persons)
               .join(Ctx::persons)
               .join(Ctx::persons);

        builder.rule("rule1")
               .from(builder.from(Ctx::persons)).filter((ctx, p) -> p.age() > 50)
               .join(builder.from(Ctx::persons)).filter((ctx, p) -> p.age() > 50)
               .join(builder.from(Ctx::persons)).filter((ctx, p) -> p.age() > 50);

        builder.rule("rule1")
               .from(Ctx::persons).filter((ctx, p) -> p.age() > 50)
               .join(Ctx::persons).filter((ctx, p) -> p.age() > 50)
               .join(Ctx::persons).filter((ctx, p) -> p.age() > 50);

        builder.rule("rule1")
               .from(builder.from(Ctx::persons)).filter((ctx, p) -> p.age() > 50)
               .join(builder.from(Ctx::persons)).filter((ctx, p) -> p.age() > 50)
               .filter((ctx, p1, p2) -> p1.age() > p2.age())
               .join(builder.from(Ctx::persons)).filter((ctx, p) -> p.age() > 50)
               .filter((ctx, p1, p2, p3) -> p1.age() > p3.age());

        builder.rule("rule1")
               .from(Ctx::persons).filter((ctx, p) -> p.age() > 50)
               .join(Ctx::persons).filter((ctx, p) -> p.age() > 50)
               .filter((ctx, p1, p2) -> p1.age() > p2.age())
               .join(Ctx::persons).filter((ctx, p) -> p.age() > 50)
               .filter((ctx, p1, p2, p3) -> p1.age() > p3.age());

        builder.rule("rule1")
               .from(builder.from(Ctx::persons).filter((ctx, p) -> p.age() > 50))
               .join(builder.from(Ctx::persons).filter((ctx, p) -> p.age() > 50))
               .filter((ctx, p1, p2) -> p1.age() > p2.age())
               .join(builder.from(Ctx::persons).filter((ctx, p) -> p.age() > 50))
               .filter((ctx, p1, p2, p3) -> p1.age() > p3.age());
    }

    @Test
    public void testCompactFilter() {
        RuleBuilder<Ctx> builder = new RuleBuilder<>();

        Variable<Person> v1 = Variable.of("p1");
        Variable<Person> v2 = Variable.of("p2");
        Variable<Person> v3 = Variable.of("p3");
        Variable<Person> v4 = Variable.of("p4");
        builder.rule("rule1").<Params3>params()
               .join(builder.from(Ctx::persons)).var(v1)
               .join(builder.from(Ctx::persons)).var(v2)
               .join(builder.from(Ctx::persons)).var(v3)

               .join(builder.from(Ctx::persons)).var(v4)
                                                .filter( v1, v4, (ctx, a1, a2) -> a1.name() == a2.name());
    }

    public void test1() {
        RuleBuilder<Ctx> builder = new RuleBuilder<>();

        builder.rule("rule1")
               .fn( (ctx) -> System.out.println("hello"));

        builder.rule("rule1")
               .from(Ctx::libraries).filter((ctx, b) -> b.name() != null)
               //.path()
               .fn( (ctx, b) -> System.out.println(b.name()));

        builder.rule("rule1").<Params3>params()
               .join(builder.from(Ctx::persons).filter((ctx, b) -> b.age() > 20))
               .fn( (ctx, b, c) -> System.out.println("hello"));

        builder.rule("rule1").<Params3>params()
               .fn( (ctx, p) -> System.out.println(p.p3_1));

        builder.rule("rule1").<Params3>params()
               .join(builder.from(Ctx::persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> a.p3_1().length() > b.age())
               .join(builder.from(Ctx::persons).filter(((ctx, c) -> c.age() > 20)))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0)
               .fn( (ctx, a, b, c) -> System.out.println(a.p3_1));;

        builder.rule("rule1").param("name", String.class).param("age", int.class)
               .join(builder.from(Ctx::persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> ((String)a.get(0)).length() > b.age())
               .join(builder.from(Ctx::persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);

        builder.rule("rule1").map().param("name", String.class).param("age", int.class)
               .join(builder.from(Ctx::persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> ((String)a.get("name")).length() > b.age())
               .join(builder.from(Ctx::persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);

        builder.rule("rule1").from(Ctx::persons).filter((ctx, a) -> a.age() > 20)
               .join(builder.from(Ctx::persons).filter((ctx, b) -> b.age() > 20))
               .filter((ctx, a, b) -> a.age() > b.age())
               .join(builder.from(Ctx::persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);

        builder.rule("rule1")
               .<Object>param("p1")
               .<Object>param("p2")
               .join(builder.from(Ctx::persons).filter((ctx, b) -> b.age() > 20))
               .join(builder.from(Ctx::persons).filter((ctx, c) -> c.age() > 20))
               .filter((ctx, a, b, c) -> a != null && b.age() > 0 && c.age()> 0);
    }

    public void testPath() {
        RuleBuilder<Ctx> builder = new RuleBuilder<>();

        DataStore<Object> ds = new PropagatingDataStore<>(0, new TypeIndexer<>());

        builder.rule("rule1").<Library>params()
               .<Room, Shelf, Book, Page>path5((ctx,l) -> l.rooms(), (ctx, r) -> r.name() != null)
               .path((ctx, r) -> r.shelves(), (ctx, s) -> s.name() != null )
               .path((ctx, s) -> s.books(), (ctx, b) -> b.title() != null)
               .path((ctx, b) -> b.pages(), (ctx, p) -> p.content() != null)
        .filter( (ctx, b, c) -> c.getA().name() != c.getE().content());

    }

    public void testPath2() {
        RuleBuilder<Ctx> builder = new RuleBuilder<>();

        builder.rule("rule1").<Library>params()
               .<Room, Shelf>path3()
               .path( (ctx, l) -> l.rooms(), (ctx, r) -> r.name() != null)
               .path( (ctx, r) -> r.shelves(), (ctx, s) -> s.name() != null)
               .filter((ctx, a, t) -> a.name() != t.getC().name());

        builder.rule("rule1").<Library>params()
               .<Room>path2()
               .path( (ctx, a) -> a.rooms(), (ctx, b) -> b.name() != null)
               .filter((ctx, a, t) -> a.name() != t.getB().name());

        builder.rule("rule1").from(Ctx::persons)
               .join(builder.from(Ctx::libraries))
               .<Room, Shelf, Book, Page>path5((ctx,l) -> l.rooms(), (ctx, r) -> r.name() != null)
               .path((ctx, r) -> r.shelves(), (ctx, s) -> s.name() != null )
               .path((ctx, s) -> s.books(), (ctx, b) -> b.title() != null)
               .path((ctx, b) -> b.pages(), (ctx, p) -> p.content() != null)
               .filter( (ctx, p, c, d) -> p.age() <= d.getD().pages().size())
               .filter( (ctx, p, c, d) -> p.age() <= d.<Path>as().book().pages().size());

    }

    @Test
    public void testNot() {
        RuleBuilder<Ctx> builder = new RuleBuilder<>();

        builder.rule("rule1").<Params3>params()
               .join(Ctx::persons).filter((ctx, b) -> b.age() > 20)
               .not()
                   .join(builder.from(Ctx::misc)
                                .<Map<String, Person>>type()
                                .filter((ctx, p) -> p.get("xxx").age() > 20))
                   .join(Ctx::libraries)
               .end()
               .fn( (a, b, c) -> System.out.println(a.ds() + b.p3_1 + c.name()))
               .end();
    }

    @Test
    public void testDataSource() {
        Ctx ctx = new Ctx(new PropagatingDataStore<>(0, new TypeIndexer<>()),
                          (new PropagatingDataStore<>(0, new TypeIndexer<>())),
                          (new PropagatingDataStore<>(0, new TypeIndexer<>())));

        ContextPojoDS pojoCtx = new ContextPojoDS(ctx);

        ContextMapDS mapCtx = new ContextMapDS();

        Function1<Ctx, DataStore<Person>> x = Ctx::persons;

        Function2<Map, String, DataStore<?>> y = Map<String, DataStore<?>>::get;
    }


}

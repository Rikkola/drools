package org.drools.core;

import org.drools.api.data.DataHandle;
import org.drools.api.data.DataStore;
import org.drools.base.base.ClassObjectType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTypeDataStoreTest {
    @Test
    public void testNonIndexedPropagation() {
        PropagatingDataStore<Object> objects = new PropagatingDataStore(new TypeIndexer<>());

        record DS1(DataStore<Object> objects) {};

        Filter1Type<DataStore<Object>, Person> personOtn = new Filter1Type<>( new ClassObjectType(Person.class) );
        Filter1Type<DataStore<Object>, City>   cityOtn = new Filter1Type<>( new ClassObjectType(City.class) );

        objects.subscribe(personOtn);
        objects.subscribe(cityOtn);

        Router<DS1> router = new Router<>(2);

        ContextPojoDS<DS1> ctx         = new ContextPojoDS<>(new DS1(objects));
        router.addContext(ctx);
        ContextRouterAdapter<DataStore<Object>, DS1, Person> ctxAdapter0 = new ContextRouterAdapter(0, router);
        ContextRouterAdapter<DataStore<Object>, DS1, City>   ctxAdapter1 = new ContextRouterAdapter(1, router);

        personOtn.subscribe(ctxAdapter0);
        cityOtn.subscribe(ctxAdapter1);

        RecordingDataProcessor<DS1, Person> recorder0 = new RecordingDataProcessor<>(0);
        RecordingDataProcessor<DS1, City> recorder1 = new RecordingDataProcessor<>(1);
        router.subscribe(0, recorder0);
        router.subscribe(1, recorder1);

        List<LogEntry> list0 = recorder0.getLog();
        List<LogEntry> list1 = recorder1.getLog();

        DataHandle<Object> h1 = objects.add(new Person("Darth", 100, "London"));
        assertThat(list1).hasSize(0);
        assertThat(list0).hasSize(1);
        assertThat(list0.get(0).action()).isEqualTo("add");
        assertThat(list0.get(0).object()).isSameAs(h1.getObject());

        objects.update(h1, new Person("Darth", 210, "New York"));
        assertThat(list1).hasSize(0);
        assertThat(list0).hasSize(2);
        assertThat(list0.get(1).action()).isEqualTo("update");
        assertThat(list0.get(1).object()).isSameAs(h1.getObject());

        assertThat(list1).hasSize(0);
        objects.remove(h1);
        assertThat(list0).hasSize(3);
        assertThat(list0.get(2).action()).isEqualTo("remove");
        assertThat(list0.get(2).object()).isSameAs(h1.getObject());
        assertThat(list1).hasSize(0);

        DataHandle<Object> c1 = objects.add(new City("London"));
        assertThat(list1).hasSize(1);
        assertThat(list1.get(0).action()).isEqualTo("add");
        assertThat(list1.get(0).object()).isSameAs(c1.getObject());

        objects.update(c1, new City("New York"));
        assertThat(list1).hasSize(2);
        assertThat(list1.get(1).action()).isEqualTo("update");
        assertThat(list1.get(1).object()).isSameAs(c1.getObject());

        objects.remove(c1);
        assertThat(list1).hasSize(3);
        assertThat(list1.get(2).action()).isEqualTo("remove");
        assertThat(list1.get(2).object()).isSameAs(c1.getObject());

        assertThat(list0).hasSize(3);
    }

    @Test
    public void testIndexedPropagation() {
        TypeIndexer<DataStore<Object>> typeIndex =new TypeIndexer<>();

        PropagatingDataStore<Object> objects = new PropagatingDataStore(typeIndex);

        record DS1(DataStore<Object> objects) {};

        Router<DS1> router = new Router<>(3);

        ContextPojoDS<DS1> ctx         = new ContextPojoDS<>(new DS1(objects));
        router.addContext(ctx);
        ContextRouterAdapter<DataStore<Object>, DS1, A1> a1 = new ContextRouterAdapter(0, router);
        ContextRouterAdapter<DataStore<Object>, DS1, A2> a2 = new ContextRouterAdapter(1, router);
        ContextRouterAdapter<DataStore<Object>, DS1, A3> a3 = new ContextRouterAdapter(2, router);

        typeIndex.buildCache(Base123.class, List.of(a1, a2, a3));
        typeIndex.buildCache(Base1.class, List.of(a1));
        typeIndex.buildCache(Base2.class, List.of(a2));
        typeIndex.buildCache(Base3.class, List.of(a3));

        Filter1TypeIndex indexedTypeIndex = new Filter1TypeIndex();
        objects.subscribe(indexedTypeIndex);

        RecordingDataProcessor<DS1, Object> recorder0 = new RecordingDataProcessor<>(0);
        RecordingDataProcessor<DS1, Object> recorder1 = new RecordingDataProcessor<>(1);
        RecordingDataProcessor<DS1, Object> recorder2 = new RecordingDataProcessor<>(2);
        router.subscribe(0, recorder0);
        router.subscribe(1, recorder1);
        router.subscribe(2, recorder2);

        List<LogEntry> list0 = recorder0.getLog();
        List<LogEntry> list1 = recorder1.getLog();
        List<LogEntry> list2 = recorder2.getLog();

        DataHandle<Object> h1 = objects.add(new Base1());
        assertThat(list0).hasSize(1);
        assertThat(list1).hasSize(0);
        assertThat(list2).hasSize(0);
        assertThat(list0.get(0).action()).isEqualTo("add");
        assertThat(list0.get(0).object()).isSameAs(h1.getObject());

        DataHandle<Object> h2 = objects.add(new Base2());
        assertThat(list0).hasSize(1);
        assertThat(list1).hasSize(1);
        assertThat(list2).hasSize(0);
        assertThat(list1.get(0).action()).isEqualTo("add");
        assertThat(list1.get(0).object()).isSameAs(h2.getObject());

        DataHandle<Object> h3 = objects.add(new Base3());
        assertThat(list0).hasSize(1);
        assertThat(list1).hasSize(1);
        assertThat(list2).hasSize(1);
        assertThat(list2.get(0).action()).isEqualTo("add");
        assertThat(list2.get(0).object()).isSameAs(h3.getObject());

        DataHandle<Object> h123 = objects.add(new Base123());
        assertThat(list0).hasSize(2);
        assertThat(list1).hasSize(2);
        assertThat(list2).hasSize(2);
        assertThat(list0.get(1).action()).isEqualTo("add");
        assertThat(list0.get(1).object()).isSameAs(h123.getObject());
        assertThat(list1.get(1).action()).isEqualTo("add");
        assertThat(list1.get(1).object()).isSameAs(h123.getObject());
        assertThat(list2.get(1).action()).isEqualTo("add");
        assertThat(list2.get(1).object()).isSameAs(h123.getObject());
    }

    public class Base123 implements A1, A2, A3 {

    }

    public class Base1 implements A1 {

    }

    public class Base2 implements A2 {

    }

    public class Base3 implements A3 {

    }

    public interface A1 {
        default boolean a1() {
            return true;
        }
    }

    public interface A2 {
        default boolean a2() {
            return true;
        }
    }

    public interface A3 {
        default boolean a3() {
            return true;
        }
    }

}

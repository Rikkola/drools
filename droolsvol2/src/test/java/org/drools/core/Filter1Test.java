package org.drools.core;

import org.drools.api.data.ObjectHandle;
import org.drools.api.data.DataStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Filter1Test {

    @Test
    public void testPropagation() {
        PropagatingDataStore<Person> persons = new PropagatingDataStore(0, new TypeIndexer<>());

        Filter1<DataStore<Person>, Person> f1 = new Filter1<>((ctx, p) -> p.age() > 0) {};
        persons.subscribe(f1);

        RecordingDataProcessor<DataStore<Person>, Person> recorder = new RecordingDataProcessor<>(0);
        List<LogEntry> list = recorder.getLog();
        f1.subscribe(recorder);

        ObjectHandle<Person> h1 = persons.add(new Person("Darth", 100, "London"));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).action()).isEqualTo("add");
        assertThat(list.get(0).object()).isSameAs(h1.getObject());

        persons.update(h1, new Person("Darth", 210, "New York"));
        assertThat(list).hasSize(2);
        assertThat(list.get(1).action()).isEqualTo("update");
        assertThat(list.get(1).object()).isSameAs(h1.getObject());

        persons.remove(h1);
        assertThat(list).hasSize(3);
        assertThat(list.get(2).action()).isEqualTo("remove");
        assertThat(list.get(2).object()).isSameAs(h1.getObject());
    }
}

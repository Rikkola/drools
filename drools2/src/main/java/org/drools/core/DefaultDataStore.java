package org.drools.core;


import org.drools.api.data.DataProcessor;
import org.drools.base.base.ValueResolver;

import java.util.IdentityHashMap;
import java.util.Map;
import org.drools.api.data.DataHandle;
import org.drools.api.data.DataStore;

public class DefaultDataStore<T> extends AbstractDataSource<T> implements DataStore<T> {
    private DataHandleFactory handleFactory;

    private Map<T, DataHandle> store;

    private ValueResolver valueResolver;

    protected DefaultDataStore() {
        this.handleFactory = new DataHandleFactory();
        this. store = new IdentityHashMap<>();
    }

    public DataHandle add(T t) {
        DataHandle dh = createDataHandle(t);
        store.put(t, dh);

        subscribers.forEach(s -> s.add(dh, valueResolver));
        return dh;
    }

    @Override
    public void update(DataHandle<T> dh, T object) {
        subscribers.forEach(s -> s.update(dh, valueResolver));
    }

    @Override
    public void remove(T object) {
        remove(lookup(object));
    }

    @Override
    public <T1> DataStore<T1> as(Class cls) {
        return null;
    }

    @Override
    public void remove(DataHandle<T> dh) {
        subscribers.forEach(s -> s.remove(dh, valueResolver));
        store.remove(dh.getObject());
    }

    protected DataHandle createDataHandle(T t) {
        return handleFactory.newDataHandle(t);
    }

    @Override
    public DataHandle lookup(T object) {
        return store.get(object);
    }

    public void subscribe(DataProcessor processor) {
//        super.subscribe(processor);
//        store.values().forEach(dh -> internalInsert(dh, processor));
    }

}

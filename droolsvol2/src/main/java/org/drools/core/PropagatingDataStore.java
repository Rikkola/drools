package org.drools.core;


import org.drools.api.data.DataProcessor;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import org.drools.api.data.DataHandle;
import org.drools.api.data.DataStore;

public class PropagatingDataStore<T> extends AbstractDataSource<T> implements DataStore<T> {
    private ArrayList<DataProcessor<DataStore<T>, T>> subscribers;

    private DataHandleFactory handleFactory;

    private Map<T, DataHandle> store;

    private SelfContext<DataStore<T>> ctx;

    protected PropagatingDataStore(TypeIndexer<DataStore<T>> typeIndexer) {
        this.handleFactory = new DataHandleFactory();
        this.store = new IdentityHashMap<>();
        this.ctx = new SelfContext<>(this);
        this.ctx.setTypeIndexer(typeIndexer);
        this.subscribers = new ArrayList<>();
    }

    public DataHandle add(T t) {
        DataHandle dh = createDataHandle(t);
        store.put(t, dh);

        subscribers.forEach(s -> s.add(ctx, dh));
        return dh;
    }

    @Override
    public void update(DataHandle<T> dh, T object) {
        subscribers.forEach(s -> s.update(ctx, dh));
    }

    @Override
    public void remove(T object) {
        remove(lookup(object));
    }

    @Override
    public void remove(DataHandle<T> dh) {
        store.remove(dh.getObject());
        subscribers.forEach(s -> s.remove(ctx, dh));
    }

    protected DataHandle createDataHandle(T t) {
        return handleFactory.newDataHandle(t);
    }

    @Override
    public DataHandle lookup(T object) {
        return store.get(object);
    }

    public void subscribe(DataProcessor processor) {
        subscribers.add(processor);
    }

    public void unsubscribe(DataProcessor processor) {
        subscribers.remove(processor);
    }

    public Context<DataStore<T>> getContext() {
        return ctx;
    }
}

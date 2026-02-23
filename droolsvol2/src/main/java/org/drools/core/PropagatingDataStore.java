package org.drools.core;


import org.drools.api.data.DataProcessor;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import org.drools.api.data.ObjectHandle;
import org.drools.api.data.DataStore;
import org.drools.base.common.NetworkNode;

public class PropagatingDataStore<T> extends AbstractDataSource<T> implements DataStore<T> {
    private ArrayList<DataProcessor<DataStore<T>, T>> subscribers;

    private DataHandleFactory handleFactory;

    private Map<T, ObjectHandle> store;

    private SelfContext<DataStore<T>> ctx;

    protected PropagatingDataStore(int id, TypeIndexer<DataStore<T>> typeIndexer) {
        super(id);
        this.handleFactory = new DataHandleFactory();
        this.store = new IdentityHashMap<>();
        this.ctx = new SelfContext<>(this);
        this.ctx.setTypeIndexer(typeIndexer);
        this.subscribers = new ArrayList<>();
    }

    public ObjectHandle add(T t) {
        ObjectHandle dh = createDataHandle(t);
        store.put(t, dh);

        subscribers.forEach(s -> s.add(ctx, dh));
        return dh;
    }

    @Override
    public void update(ObjectHandle<T> dh, T object) {
        subscribers.forEach(s -> s.update(ctx, dh));
    }

    @Override
    public void remove(T object) {
        remove(lookup(object));
    }

    @Override
    public void remove(ObjectHandle<T> dh) {
        store.remove(dh.getObject());
        subscribers.forEach(s -> s.remove(ctx, dh));
    }

    protected ObjectHandle createDataHandle(T t) {
        return handleFactory.newDataHandle(t, this);
    }

    @Override
    public ObjectHandle lookup(T object) {
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

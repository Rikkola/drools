package org.drools.core;


import org.drools.api.data.DataHandle;
import org.drools.api.data.DataStore;

import java.util.IdentityHashMap;
import java.util.Map;

public class NonPropagatingDataStore<T> extends AbstractDataSource<T> implements DataStore<T> {

    private DataHandleFactory handleFactory;

    private Map<T, DataHandle> store;

    protected NonPropagatingDataStore() {
        this.handleFactory = new DataHandleFactory();
        this.store = new IdentityHashMap<>();
    }

    public DataHandle add(T t) {
        DataHandle dh = createDataHandle(t);
        store.put(t, dh);
        return dh;
    }

    @Override
    public void update(DataHandle<T> dh, T object) {
        ((InternalDataHandle<T>)dh).setObject(object);
    }

    @Override
    public void remove(T object) {
        remove(lookup(object));
    }

    @Override
    public void remove(DataHandle<T> dh) {
        store.remove(dh.getObject());
    }

    protected DataHandle createDataHandle(T t) {
        return handleFactory.newDataHandle(t);
    }

    @Override
    public DataHandle lookup(T object) {
        return store.get(object);
    }

}

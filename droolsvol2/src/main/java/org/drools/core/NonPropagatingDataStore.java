package org.drools.core;


import org.drools.api.data.ObjectHandle;
import org.drools.api.data.DataStore;

import java.util.IdentityHashMap;
import java.util.Map;

public class NonPropagatingDataStore<T> extends AbstractDataSource<T> implements DataStore<T> {

    private DataHandleFactory handleFactory;

    private Map<T, ObjectHandle> store;

    protected NonPropagatingDataStore(int id) {
        super(id);
        this.handleFactory = new DataHandleFactory();
        this.store = new IdentityHashMap<>();
    }

    public ObjectHandle add(T t) {
        ObjectHandle dh = createDataHandle(t);
        store.put(t, dh);
        return dh;
    }

    @Override
    public void update(ObjectHandle<T> dh, T object) {
        ((InternalDataHandle<T>)dh).setObject(object);
    }

    @Override
    public void remove(T object) {
        remove(lookup(object));
    }

    @Override
    public void remove(ObjectHandle<T> dh) {
        store.remove(dh.getObject());
    }

    protected ObjectHandle createDataHandle(T t) {
        return handleFactory.newDataHandle(t, this);
    }

    @Override
    public ObjectHandle lookup(T object) {
        return store.get(object);
    }

}

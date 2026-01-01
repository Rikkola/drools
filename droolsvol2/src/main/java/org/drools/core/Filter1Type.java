package org.drools.core;

import org.drools.api.data.DataHandle;
import org.drools.api.data.DataProcessor;
import org.drools.base.base.ObjectType;

public class Filter1Type<DS, T> extends AbstractDataProcessor<DS, T> implements DataProcessor<DS, T> {

    private ObjectType type;

    public Filter1Type(ObjectType type) {
        super();
        this.type = type;
    }

    @Override
    public void add(Context<DS> ctx, DataHandle<T> handle) {
        if (type.isAssignableFrom(handle.getObject().getClass())) {
            subscribers.forEach(c -> c.add(ctx, handle));
        }
    }

    @Override
    public void update(Context<DS> ctx, DataHandle<T> handle) {
        if (type.isAssignableFrom(handle.getObject().getClass())) {
            subscribers.forEach(c -> c.update(ctx, handle));
        }
    }

    @Override
    public void remove(Context<DS> ctx, DataHandle<T> handle) {
        if (type.isAssignableFrom(handle.getObject().getClass())) {
            subscribers.forEach(c -> c.remove(ctx, handle));
        }
    }
}

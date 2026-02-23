package org.drools.core;

import org.drools.api.data.ObjectHandle;
import org.drools.api.data.DataProcessor;
import org.drools.core.function.Predicate2;

public class Filter1<DS, T> extends AbstractDataProcessor<DS, T> implements DataProcessor<DS, T> {
    private Predicate2<Context<DS>, T> predicate;

    public Filter1(Predicate2<Context<DS>, T> predicate) {
        super();
        this.predicate = predicate;
    }

    @Override
    public void add(Context<DS> ctx, ObjectHandle<T> handle) {
        if (predicate.test(ctx, handle.getObject())) {
            subscribers.forEach(c -> c.add(ctx, handle));
        }
    }

    @Override
    public void update(Context<DS> ctx, ObjectHandle<T> handle) {
        if (predicate.test(ctx, handle.getObject())) {
            subscribers.forEach(c -> c.update(ctx, handle));
        } else {
            subscribers.forEach( c -> c.remove(ctx, handle) );
        }
    }

    @Override
    public void remove(Context<DS> ctx, ObjectHandle<T> handle) {
        subscribers.forEach( c -> c.remove(ctx, handle) );
    }


}

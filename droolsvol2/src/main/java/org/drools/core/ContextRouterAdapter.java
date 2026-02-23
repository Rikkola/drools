package org.drools.core;

import org.drools.api.data.ObjectHandle;
import org.drools.api.data.DataProcessor;

public class ContextRouterAdapter<I, O, T> implements DataProcessor<I, T> {
    private int index;

    private Router<O> router;

    public ContextRouterAdapter(int index, Router<O> router) {
        this.index = index;
        this.router = router;
    }

    @Override
    public void add(Context<I> ctx, ObjectHandle<T> handle) {
        router.add(index, handle);
    }

    @Override
    public void update(Context<I> ctx, ObjectHandle<T> handle) {
        router.update(index, handle);
    }

    @Override
    public void remove(Context<I> ctx, ObjectHandle<T> handle) {
        router.remove(index, handle);
    }
}

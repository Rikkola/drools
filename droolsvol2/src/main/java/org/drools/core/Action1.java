package org.drools.core;

import org.drools.api.data.DataHandle;
import org.drools.api.data.DataProcessor;
import org.drools.core.function.Consumer2;

public class Action1<DS, T> extends AbstractDataProcessor<DS, T> implements DataProcessor<DS, T> {
    private Consumer2<Context<DS>, T> consumer;

    public Action1(Consumer2<Context<DS>, T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void add(Context<DS> ctx, DataHandle<T> handle) {
        consumer.accept(ctx, handle.get());
        subscribers.forEach( c -> c.add(ctx, handle) );
    }

    @Override
    public void update(Context<DS> ctx, DataHandle<T> handle) {
        consumer.accept(ctx, handle.get());
        subscribers.forEach( c -> c.add(ctx, handle) );
    }

    @Override
    public void remove(Context<DS> ctx, DataHandle<T> handle) {
        consumer.accept(ctx, handle.get());
        subscribers.forEach( c -> c.remove(ctx, handle) );
    }
}

package org.drools.api.data;

import org.drools.core.Context;

public interface DataProcessor<DS, T> {


    void add(Context<DS> ctx, DataHandle<T> handle);

    void update(Context<DS> ctx, DataHandle<T> handle);

    void remove(Context<DS> ctx, DataHandle<T> handle);

}

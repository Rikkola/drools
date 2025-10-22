package org.drools.api.data;

import org.drools.base.base.ValueResolver;

public interface DataProcessor<T> {

    void add(DataHandle<T> handle, ValueResolver valueResolver);

    void update(DataHandle<T> handle, ValueResolver valueResolver);

    void remove(DataHandle<T> handle, ValueResolver valueResolver);

}

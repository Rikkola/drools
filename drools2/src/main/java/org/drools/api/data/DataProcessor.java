package org.drools.api.data;

import org.drools.base.base.ValueResolver;
import org.drools.core.util.DoubleLinkedEntry;

public interface DataProcessor<T> extends DoubleLinkedEntry<DataProcessor<T>> {

    void add(DataHandle<T> handle, ValueResolver valueResolver);

    void update(DataHandle<T> handle, ValueResolver valueResolver);

    void remove(DataHandle<T> handle, ValueResolver valueResolver);

}

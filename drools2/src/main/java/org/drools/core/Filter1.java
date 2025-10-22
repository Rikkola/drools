package org.drools.core;

import org.drools.api.data.DataHandle;
import org.drools.api.data.DataProcessor;
import org.drools.base.base.ValueResolver;

public class Filter1<A> extends AbstractDataProcessor<A> implements DataProcessor<A>  {
    private FilterNode<A> n;

    @Override
    public void add(DataHandle<A> handle, ValueResolver valueResolver) {
        if (n.getPredicate().isAllowed(handle, valueResolver)) {
            getChildren().stream().forEach(c -> c.add(handle, valueResolver));
        }
    }

    @Override
    public void update(DataHandle<A> handle, ValueResolver valueResolver) {
        if (n.getPredicate().isAllowed(handle, valueResolver)) {
            getChildren().stream().forEach(c -> c.update(handle, valueResolver));
        } else {
            getChildren().stream().forEach( c -> c.remove(handle, valueResolver) );
        }

    }

    @Override
    public void remove(DataHandle<A> handle, ValueResolver valueResolver) {
        getChildren().stream().forEach( c -> c.remove(handle, valueResolver) );
    }
}

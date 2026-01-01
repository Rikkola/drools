package org.drools.core;

import org.drools.api.data.DataProcessor;

import java.util.ArrayList;

public abstract class AbstractDataProcessor<DS, T> {
    protected ArrayList<DataProcessor<DS, T>> subscribers;

    public AbstractDataProcessor() {
        this.subscribers = new ArrayList<DataProcessor<DS, T>>();
    }

    public void subscribe(DataProcessor<DS, T> processor) {
        subscribers.add(processor);
    }

    public void unsubscribe(DataProcessor<DS, T> processor) {
        subscribers.remove(processor);
    }
}

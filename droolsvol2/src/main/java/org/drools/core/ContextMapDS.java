package org.drools.core;

import org.drools.api.data.DataHandle;
import org.drools.api.data.DataProcessor;
import org.drools.api.data.DataSource;
import org.drools.api.data.DataStore;
import org.drools.api.data.DataStream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextMapDS extends AbstractContext<Map<String, DataSource<?>>>
        implements Context<Map<String, DataSource<?>>> {

    private Map<String, DataSource<?>> sources;

    public ContextMapDS() {
        this.sources = new HashMap<>();
    }

    public Map<String, DataSource<?>> ds() {
        return sources;
    }

    public <T extends DataSource<T>> T  store(String name, T... t) {
        return (T) sources.get(name);
    }

    public <M> M getMemory(Object object) {
        return null;
    }

}

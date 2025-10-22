package org.drools.core;

import org.drools.api.data.DataSource;
import org.drools.api.data.DataStore;

import java.util.Map;

public class ContextMapDS implements Context<Map<String, DataSource<?>>> {

    private Map<String, DataSource<?>> sources;
//
//    public Context() {
//        this.sources = new HashMap<>();
//    }
//
//    public <T> DataStore<T> dataStore(String name) {
//        return (DataStore<T>) sources.get(name);
//    }

    public Map<String, DataSource<?>> ds() {
        return sources;
    }
}

package org.drools.core;

import org.drools.api.data.DataSource;

import java.util.Map;

public class ContextPojoDS<DS> implements Context<DS> {

    private DS sources;

    public ContextPojoDS(DS sources) {
        this.sources = sources;
    }

    //
//    public Context() {
//        this.sources = new HashMap<>();
//    }
//
//    public <T> DataStore<T> dataStore(String name) {
//        return (DataStore<T>) sources.get(name);
//    }

    public DS ds() {
        return sources;
    }
}

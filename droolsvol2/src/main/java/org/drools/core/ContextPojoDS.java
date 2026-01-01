package org.drools.core;

import org.drools.api.data.DataHandle;
import org.drools.api.data.DataProcessor;
import org.drools.api.data.DataSource;

import java.util.List;
import java.util.Map;

public class ContextPojoDS<DS> extends AbstractContext<DS> implements Context<DS> {

    private DS sources;

    public ContextPojoDS(DS sources) {
        this.sources = sources;
    }

    public DS ds() {
        return sources;
    }

    @Override
    public <M> M getMemory(Object object) {
        return null;
    }

}

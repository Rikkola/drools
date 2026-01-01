package org.drools.core;

import org.drools.api.data.DataSource;

public class SelfContext<DS extends DataSource> extends AbstractContext<DS> implements Context<DS> {
    private DS dataSource;

    public SelfContext(DS dataSource) {
        this.dataSource = dataSource;
    }

    public DS get() {
        return dataSource;
    }

    @Override
    public DS ds() {
        return dataSource;
    }

    @Override
    public <M> M getMemory(Object object) {
        return null;
    }

}

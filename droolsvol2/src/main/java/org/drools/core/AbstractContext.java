package org.drools.core;

import org.drools.api.data.DataHandle;
import org.drools.api.data.DataProcessor;
import org.drools.api.data.DataSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractContext<DS> implements Context<DS> {

    // Type erasure is necessary here, so it can be cast in the getDataProcessorsByTypeAssignment method.
    private TypeIndexer<DS> typeIndexer;

    public AbstractContext() {

    }

    public TypeIndexer<DS> getTypeIndexer() {
        return typeIndexer;
    }

    public void setTypeIndexer(TypeIndexer<DS> typeIndexer) {
        this.typeIndexer = typeIndexer;
    }

    @Override
    public <T, K extends T> List<DataProcessor<DS, K>> getDataProcessorsByTypeAssignment(DataHandle<T> handle) {
        return typeIndexer.getDataProcessorsByTypeAssignment(handle.getObject().getClass());
    }

}

package org.drools.core;

import org.drools.api.data.ObjectHandle;
import org.drools.api.data.DataProcessor;

import java.util.List;

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
    public <T, K extends T> List<DataProcessor<DS, K>> getDataProcessorsByTypeAssignment(ObjectHandle<T> handle) {
        return typeIndexer.getDataProcessorsByTypeAssignment(handle.getObject().getClass());
    }

}

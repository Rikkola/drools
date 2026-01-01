package org.drools.core;

import org.drools.api.data.DataHandle;
import org.drools.api.data.DataProcessor;

import java.util.List;

public interface Context<DS> {

    DS ds();

    <T, K extends T> List<DataProcessor<DS, K>> getDataProcessorsByTypeAssignment(DataHandle<T> handle);

    <M> M getMemory(Object object);

}

package org.drools.core;

import org.drools.api.data.DataHandle;
import org.drools.api.data.DataProcessor;

import java.util.List;

public class Filter1TypeIndex<DS, T> extends AbstractDataProcessor<DS, T> implements DataProcessor<DS, T> {

    public Filter1TypeIndex() {
        super();
    }

    @Override
    public void add(Context<DS> ctx, DataHandle<T> handle) {
        List<DataProcessor<DS, T>> list = ctx.getDataProcessorsByTypeAssignment(handle);
        if (list != null) {
            for (int i = 0, size = list.size(); i < size; i++) {
                DataProcessor<DS, T> processor = list.get(i);
                processor.add(ctx, handle);
            }
        }
    }

    @Override
    public void update(Context<DS> ctx, DataHandle<T> handle) {
        List<DataProcessor<DS, T>> list = ctx.getDataProcessorsByTypeAssignment(handle);
        if (list != null) {
            for (int i = 0, size = list.size(); i < size; i++) {
                DataProcessor<DS, T> processor = list.get(i);
                processor.update(ctx, handle);
            }
        }
    }

    @Override
    public void remove(Context<DS> ctx, DataHandle<T> handle) {
        List<DataProcessor<DS, T>> list = ctx.getDataProcessorsByTypeAssignment(handle);
        if (list != null) {
            for (int i = 0, size = list.size(); i < size; i++) {
                DataProcessor<DS, T> processor = list.get(i);
                processor.remove(ctx, handle);
            }
        }
    }


}

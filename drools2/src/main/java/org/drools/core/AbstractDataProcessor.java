package org.drools.core;

import org.drools.api.data.DataProcessor;

import java.util.ArrayList;
import java.util.List;

public class AbstractDataProcessor<T> {
    private List<DataProcessor<T>> children;

    public List<DataProcessor<T>> getChildren() {
        return children;
    }
}

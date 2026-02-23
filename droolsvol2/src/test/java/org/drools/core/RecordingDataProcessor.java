package org.drools.core;

import org.drools.api.data.ObjectHandle;
import org.drools.api.data.DataProcessor;

import java.util.ArrayList;
import java.util.List;

public class RecordingDataProcessor<DS, T> implements DataProcessor<DS, T> {
    private int            index;
    private List<LogEntry> log;

    public RecordingDataProcessor(int index) {
        this.index = index;
        this.log = new ArrayList<>();
    }

    public List<LogEntry> getLog() {
        return log;
    }

    @Override
    public void add(Context<DS> ctx, ObjectHandle<T> h) {
        log.add(new LogEntry(index,"add", h, h.getObject()));
    }

    @Override
    public void update(Context<DS> ctx, ObjectHandle<T> h) {
        log.add(new LogEntry(index,"update", h, h.getObject()));
    }

    @Override
    public void remove(Context<DS> ctx, ObjectHandle<T> h) {
        log.add(new LogEntry(index,"remove", h, h.getObject()));
    }
}

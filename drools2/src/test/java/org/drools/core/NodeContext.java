package org.drools.core;

import org.drools.core.function.Tuple;


import java.util.List;

public class NodeContext<I, O> {
    private boolean                        initialised;
    private List<O>                        list;
    private O                              current;
    private int                            cursor = -1;
    private org.drools.core.function.Tuple t;

    public NodeContext(org.drools.core.function.Tuple t) {
        this.t = t;
    }

    public boolean isInitialised() {
        return initialised;
    }

    public void setInitialised(boolean initialised) {
        this.initialised = initialised;
    }

    public List<O> getList() {
        return list;
    }

    public void setList(List<O> list) {
        this.list = list;
    }

    public O getCurrent() {
        return current;
    }

    public void setCurrent(O current) {
        this.current = current;
    }

    public void incrementCursor() {
        cursor++;
    }

    public int getCursor() {
        return cursor;
    }

    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    public <T extends Tuple> T getT() {
        return (T) t;
    }

    public <T extends Tuple> void setT(T t) {
        this.t = t;
    }
}

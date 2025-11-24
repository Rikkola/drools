package org.drools.core;

import org.drools.core.function.Tuple;


import java.util.List;

public class NodeContext<O> {
    public static final int BEGIN = 0;
    public static final int RUN = 1;
    public static final int END = 2;

    private int                            state;
    private List<O>                        list;
    private O[]                            array;
    private Iterable<O>                    iterable;
    private O                              current;
    private int                            cursor = -1;
    private Tuple tuple;

    public NodeContext(Tuple tuple) {
        this.tuple = tuple;
        this.state = BEGIN;
    }

    public boolean isInitialised() {
        return state >= RUN;
    }

    public void setInitialised() {
        this.state = RUN;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public List<O> getList() {
        return list;
    }

    public void setList(List<O> list) {
        this.list = list;
    }

    public O[] getArray() {
        return array;
    }

    public void setArray(O[] array) {
        this.array = array;
    }

    public Iterable<O> getIterable() {
        return iterable;
    }

    public void setIterable(Iterable<O> iterable) {
        this.iterable = iterable;
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

    public <T extends Tuple> T getTuple() {
        return (T) tuple;
    }

}

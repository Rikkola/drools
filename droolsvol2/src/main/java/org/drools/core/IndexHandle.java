package org.drools.core;

public class IndexHandle implements Handle {
    private int index;

    public IndexHandle(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}

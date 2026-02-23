package org.drools.core;

public class ContextPojoDS<DS> extends AbstractContext<DS> implements Context<DS> {

    private DS sources;

    public ContextPojoDS(DS sources) {
        this.sources = sources;
    }

    public DS ds() {
        return sources;
    }

    @Override
    public <M> M getMemory(Object object) {
        return null;
    }

    @Override
    public String toString() {
        return "ContextPojoDS{" +
               "sources=" + sources +
               '}';
    }
}

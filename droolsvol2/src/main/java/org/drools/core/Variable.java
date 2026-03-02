package org.drools.core;

public class Variable<T> {
    private String name;
    private Class type;

    public Variable(String name, Class type) {
        this.name = name;
        this.type = type;
    }

    public static <K> Variable<K> of(String name, K... type) {
        return new Variable<K>(name, type.getClass().getComponentType());
    }

    public String name() {
        return name;
    }
    public Class type() {
        return type;
    }
}

package org.drools.core;

import java.util.List;

record Library(String name, List<Room> rooms) {
    public String toString() {
        return "Library[name=" + name + "]";
    }
}

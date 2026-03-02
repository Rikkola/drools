package org.drools.core;

import java.util.List;

record Room(String name, List<Shelf> shelves) {
    public String toString() {
        return "Room[name=" + name + "]";
    }
}

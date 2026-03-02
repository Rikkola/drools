package org.drools.core;

import java.util.List;

record Shelf(String name, List<Book> books) {
    public String toString() {
        return "Shelf[name=" + name + "]";
    }
}

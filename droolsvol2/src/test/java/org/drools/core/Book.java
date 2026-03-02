package org.drools.core;

import java.util.List;

record Book(String title, List<Page> pages) {
    public String toString() {
        return "Book[name=" + title + "]";
    }
}

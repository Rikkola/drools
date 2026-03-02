package org.drools.core;

record Page(int number, String content) {
    public String toString() {
        return "Page[number=" + number + "]";
    }
}

package org.drools.core;

public class BaseNode implements NetworkNode {
    private int id;
    private int pathIndex;
    private int size;
    private int objectIndex;

    public BaseNode(int id, int pathIndex, int objectIndex) {
        this.id          = id;
        this.pathIndex   = pathIndex;
        this.objectIndex = objectIndex;
    }

    public BaseNode(int id, int pathIndex, int objectIndex, int size) {
        this.id          = id;
        this.pathIndex   = pathIndex;
        this.objectIndex = objectIndex;
        this.size        = size;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getPathIndex() {
        return pathIndex;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getObjectIndex() {
        return objectIndex;
    }
}

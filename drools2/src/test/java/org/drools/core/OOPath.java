package org.drools.core;

import org.drools.core.function.Tuple;

public class OOPath<R, L, T extends Tuple> {
    PathNode<?, ?, T> leaf;

    PathContext pathContext;

    public OOPath(PathNode<?, L, T> leaf) {
        this.leaf = leaf;
        PathNode node = leaf.getParent();
        int i = 1;
        while (node != null) {
            i++;
            node = node.getParent();
        }

        pathContext = new PathContext(i);
    }

    public L next() {
        return (L) leaf.next(pathContext);
    }

    public PathContext getPathContext() {
        return pathContext;
    }
}

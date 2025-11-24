package org.drools.core;

import org.drools.core.function.Predicate2;
import org.drools.core.function.Tuple;

import java.util.Iterator;

public class OOPath<R, L, T extends Tuple> {
    PathNode<?, L, T> leaf;
    private int size;

    public OOPath(PathNode<?, L, T> leaf) {
        this.leaf = leaf;
        PathNode node = leaf.getParent();
        int i = 1;
        while (node != null) {
            i++;
            node = node.getParent();
        }

        size = i;
    }

    public PathContext<?> getPathContext(Iterator<L> it) {
        return ((OOPathIterator)it).pathContext;
    }

    public Iterator<L> iterator(R root) {
        PathContext<T> pathContext = new PathContext<>(size);
        pathContext.getContext(0).setCurrent(root);

        return new OOPathIterator<>(leaf,
                                    pathContext);

    }

    public void forEach(R root, Predicate2<L, PathContext<T>> fn) {
        OOPathIterator<L, T> it = (OOPathIterator<L, T>) iterator(root);
        boolean loop = true;
        while (loop && it.hasNext()) {
            loop = it.next(fn);
        }
    }

    public static class OOPathIterator<L, T extends Tuple> implements Iterator<L> {
        private PathNode<?, L, T> leaf;

        private PathContext<T> pathContext;

        public OOPathIterator(PathNode<?, L,T> leaf,
                              PathContext<T> pathContext) {
            this.leaf = leaf;
            this.pathContext = pathContext;
        }

        @Override
        public boolean hasNext() {
            return leaf.hasNext(pathContext);
        }

        @Override
        public L next() {
            return leaf.next(pathContext);
        }


        public boolean next(Predicate2<L, PathContext<T>> fn) {
            return fn.test(leaf.next(pathContext), pathContext);
        }
    }
}

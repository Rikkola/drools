package org.drools.core;

import org.drools.core.function.Function1;
import org.drools.core.function.Predicate1;
import org.drools.core.function.Tuple;
import org.drools.core.function.Tuple.Tuple2;
import org.drools.core.function.Tuple.Tuple3;
import org.drools.core.function.Tuple.Tuple4;
import org.drools.core.function.Tuple.Tuple5;

import java.util.List;

public interface PathNode<I, O, T extends Tuple> {

    O next(PathContext pathContext);

    public PathNode<? ,? ,?> getParent();

    public static class RootPathNode<O, T extends Tuple> implements PathNode<Void, O, T> {
        private O o;
        private boolean consmed;
        private T t;

        public RootPathNode(O o, T t) {
            this.o = o;
            this.t = t;
        }

        @Override
        public O next(PathContext pathContext) {
            if (!consmed) {
                consmed = true;
                t.set(0,o);
                return o;
            } else {
                return null;
            }
        }

        @Override
        public PathNode<?, ?, ?> getParent() {
            return null;
        }
    }


    // library.rooms -> room.shelves -> shelf.books -> book.pages -> page
    public static class ListPathNode<I, O, T extends Tuple>  implements PathNode<I, O, T>  {
        private AccessType            type;
        private int                   index;
        private Predicate1<O>         flt1;
        private Function1<I, List<O>> fn1;
        private PathNode<?, I, ?>     parent;

        public ListPathNode(AccessType type, int index,
                            Function1<I, List<O>> fn1, Predicate1<O> flt1,
                            PathNode<?, I, ?> parent) {
            this.type   = type;
            this.index = index;
            this.flt1   = flt1;
            this.fn1    = fn1;
            this.parent = parent;
        }

        public O next(PathContext pathContext) {
            NodeContext<I,O> ctx = pathContext.getContext(index);

            if (!ctx.isInitialised()) {
                if (parent != null) {
                    I i = parent.next(pathContext);
                    if (i != null) {
                        ctx.setList(fn1.apply(i));
                    }
                }
                ctx.setInitialised(true);
            }

            do {
                ctx.incrementCursor();
                if (ctx.getCursor() < ctx.getList().size()) {
                    ctx.setCurrent(ctx.getList().get(ctx.getCursor()));
                    ctx.getT().set(index, ctx.getCurrent());
                } else {
                    I i = parent.next(pathContext);
                    if ( i != null) {
                        ctx.setList(fn1.apply(i));
                    } else {
                        ctx.setList(null);
                    }

                    ctx.setCursor(-1);
                    ctx.setCurrent(null);
                }
            } while ((ctx.getCurrent() == null || !flt1.test(ctx.getCurrent())) &&
                     ctx.getList() != null);

            return ctx.getCurrent();
        }

        @Override
        public PathNode<?, ?, ?> getParent() {
            return parent;
        }
    }

}

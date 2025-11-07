package org.drools.core;

import org.drools.core.function.Function1;
import org.drools.core.function.Predicate1;
import org.drools.core.function.Tuple;

import java.util.List;

public interface PathNode<I, O, T extends Tuple> {

    boolean hasNext(PathContext pathContext);

    O next(PathContext pathContext);

    public PathNode<? ,? ,?> getParent();

    public static class RootPathNode<O, T extends Tuple> implements PathNode<Void, O, T> {
        Predicate1<O> flt1;

        public RootPathNode(Predicate1<O> flt1) {
            this.flt1 = flt1;
        }

        @Override
        public O next(PathContext pathContext) {
            NodeContext<?,O> ctx = pathContext.getContext(0);

            if (!ctx.isInitialised()) {
                ctx.setInitialised();
                O o = ctx.getCurrent();
                ctx.getTuple().set(0, o);
                ctx.setCurrent(null);
                return o;
            } else {
                ctx.getTuple().set(0, null);
                return null;
            }
        }

        @Override
        public boolean hasNext(PathContext pathContext) {
            NodeContext<?,O> ctx = pathContext.getContext(0);

            return !ctx.isInitialised();
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
        private Function1<I, ?> fn1;
        private PathNode<?, I, ?>     parent;

        public ListPathNode(AccessType type, int index,
                            Function1<I, ?> fn1, Predicate1<O> flt1,
                            PathNode<?, I, ?> parent) {
            this.type   = type;
            this.index = index;
            this.flt1   = flt1;
            this.fn1    = fn1;
            this.parent = parent;
        }

        public O primeNext(PathContext pathContext) {
            NodeContext<I,O> ctx = pathContext.getContext(index);

            if (!ctx.isInitialised()) {
                if (parent != null) {
                    I i = parent.next(pathContext);
                    if (i != null) {
                        ctx.setList((List<O>) fn1.apply(i));
                    }
                }
                ctx.setInitialised();
            }

            do {
                ctx.incrementCursor();
                if (ctx.getCursor() < ctx.getList().size()) {
                    ctx.setCurrent(ctx.getList().get(ctx.getCursor()));
                    ctx.getTuple().set(index, ctx.getCurrent());
                } else {
                    I i = parent.next(pathContext);
                    if ( i != null) {
                        ctx.setList((List<O>) fn1.apply(i));
                    } else {
                        ctx.setList(null);
                    }

                    ctx.setCursor(-1);
                    ctx.setCurrent(null);
                    ctx.getTuple().set(index, null);
                }
            } while ((ctx.getCurrent() == null || !flt1.test(ctx.getCurrent())) &&
                     ctx.getList() != null);

            if (ctx.getCurrent() == null) {
                ctx.setState(NodeContext.END);
            }

            return ctx.getCurrent();
        }

        public boolean hasNext(PathContext pathContext) {
            NodeContext<I,O> ctx = pathContext.getContext(index);

            if ( ctx.getCurrent() != null) {
                // this ensures prime is never called twice
                return true;
            }

            if (ctx.getState() != NodeContext.END) {
                primeNext(pathContext);
            }

            if ( ctx.getCurrent() != null) {
                return true;
            }

            return false;
        }

        public O next(PathContext pathContext) {
            NodeContext<I,O> ctx = pathContext.getContext(index);
            if (ctx.getState() == NodeContext.END) {
                return null;
            }

            O o = ctx.getCurrent();
            if ( o == null) {
                o = primeNext(pathContext);
            }
            ctx.setCurrent(null);

            return o;
        }

        @Override
        public PathNode<?, ?, ?> getParent() {
            return parent;
        }
    }

}

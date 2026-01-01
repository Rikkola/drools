package org.drools.core;

import org.drools.core.function.Function2;
import org.drools.core.function.Predicate2;
import org.drools.core.function.BaseTuple;
import org.drools.core.function.BaseTuple.Tuple0;

import java.util.List;

public interface PathNode<I, O, T extends BaseTuple> {

    boolean hasNext(PathContext<T> pathContext);

    O next(PathContext pathContext);

    public PathNode<?, I, ?> getParent();

    int index();

    public static class RootPathNode<O, T extends BaseTuple> implements PathNode<Void, O, T> {
        Predicate2<PathContext<Tuple0>, O> flt2;

        public RootPathNode(Predicate2<PathContext<Tuple0>, O> flt2) {
            this.flt2 = flt2;
        }

        @Override
        public O next(PathContext pathContext) {
            NodeContext<O> ctx = pathContext.getContext(0);

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
            NodeContext<O> ctx = pathContext.getContext(0);

            return !ctx.isInitialised();
        }

        @Override
        public PathNode<Void, Void, ?> getParent() {
            return null;
        }

        @Override
        public int index() {
            return 0;
        }
    }


    // library.rooms -> room.shelves -> shelf.books -> book.pages -> page
    public static class ListPathNode<I, O, T extends BaseTuple> implements PathNode<I, O, T> {
        private AccessType             type;
        private int                    index;
        private Function2<PathContext<T>, I, ?>    fn2;
        private Predicate2<PathContext<T>, O>      flt2;
        private PathNode<?, I, ?> parent;

        public ListPathNode(AccessType type,
                            Function2<PathContext<T>, I, ?> fn1, Predicate2<PathContext<T>, O> flt2,
                            PathNode<?, I, ?> parent) {
            this.type   = type;
            this.index = parent.index() +1;
            this.flt2  = flt2;
            this.fn2   = fn1;
            this.parent = parent;
        }

        public O primeNext(PathContext pathContext) {
            NodeContext<O> ctx = pathContext.getContext(index);

            if (!ctx.isInitialised()) {
                if (parent != null) {
                    I i = parent.next((PathContext<T>) pathContext);
                    if (i != null) {
                        ctx.setList((List<O>) fn2.apply(pathContext, i));
                    }
                }
                ctx.setInitialised();
            }

            do {
                ctx.incrementCursor();
                if (ctx.getCursor() <ctx.getList().size()) {
                    ctx.setCurrent(ctx.getList().get(ctx.getCursor()));
                    ctx.getTuple().set(index, ctx.getCurrent());
                } else {
                    I i = parent.next((PathContext<T>) pathContext);
                    if ( i != null) {
                        ctx.setList((List<O>) fn2.apply(pathContext, i));
                    } else {
                        ctx.setList(null);
                    }

                    ctx.setCursor(-1);
                    ctx.setCurrent(null);
                    ctx.getTuple().set(index, null);
                }
            } while ((ctx.getCurrent() == null || !flt2.test((PathContext<T>)pathContext, ctx.getCurrent())) &&
                     ctx.getList() != null);

            if (ctx.getCurrent() == null) {
                ctx.setState(NodeContext.END);
            }

            return ctx.getCurrent();
        }

        public boolean hasNext(PathContext pathContext) {
            NodeContext<O> ctx = pathContext.getContext(index);

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
            NodeContext<O> ctx = pathContext.getContext(index);
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
        public PathNode<?, I, ?> getParent() {
            return parent;
        }

        public int index() {
            return index;
        }
    }

}

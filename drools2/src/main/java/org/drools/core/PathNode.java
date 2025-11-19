package org.drools.core;

import org.drools.core.function.Function2;
import org.drools.core.function.Predicate2;
import org.drools.core.function.Tuple;

import java.util.List;

public interface PathNode<I, O, TI extends Tuple, TO extends Tuple> {

    boolean hasNext(PathContext<O, TO> pathContext);

    O next(PathContext pathContext);

    public PathNode<? ,? , ?, TI> getParent();

    public static class RootPathNode<O, TO extends Tuple> implements PathNode<O, O, TO, TO> {
        Predicate2<PathContext<O, TO>, O> flt2;

        public RootPathNode(Predicate2<PathContext<O, TO>, O> flt2) {
            this.flt2 = flt2;
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
        public PathNode<O, O, TO, TO> getParent() {
            return null;
        }
    }


    // library.rooms -> room.shelves -> shelf.books -> book.pages -> page
    public static class ListPathNode<I, O, TI extends Tuple, TO extends Tuple> implements PathNode<I, O, TI, TO> {
        private AccessType             type;
        private int                    index;
        private Function2<PathContext<I, TI>, I, ?>    fn2;
        private Predicate2<PathContext<O, TO>, O>      flt2;
        private PathNode<?, I, ?, TI> parent;

        public ListPathNode(AccessType type, int index,
                            Function2<PathContext<I, TI>, I, ?> fn1, Predicate2<PathContext<O, TO>, O> flt2,
                            PathNode<?, I, ?, TI> parent) {
            this.type   = type;
            this.index = index;
            this.flt2  = flt2;
            this.fn2   = fn1;
            this.parent = parent;
        }

        public O primeNext(PathContext pathContext) {
            NodeContext<I,O> ctx = pathContext.getContext(index);

            if (!ctx.isInitialised()) {
                if (parent != null) {
                    I i = parent.next((PathContext<I, TI>) pathContext);
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
                    I i = parent.next((PathContext<O, TO>) pathContext);
                    if ( i != null) {
                        ctx.setList((List<O>) fn2.apply(pathContext, i));
                    } else {
                        ctx.setList(null);
                    }

                    ctx.setCursor(-1);
                    ctx.setCurrent(null);
                    ctx.getTuple().set(index, null);
                }
            } while ((ctx.getCurrent() == null || !flt2.test((PathContext<O, TO>)pathContext, ctx.getCurrent())) &&
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
        public PathNode<?, I, ?, TI> getParent() {
            return parent;
        }
    }

}

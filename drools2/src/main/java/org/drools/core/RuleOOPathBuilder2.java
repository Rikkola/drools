package org.drools.core;

import org.drools.core.PathNode.ListPathNode;
import org.drools.core.PathNode.RootPathNode;
import org.drools.core.function.Function2;
import org.drools.core.function.Predicate2;
import org.drools.core.function.Tuple;
import org.drools.core.function.Tuple.Tuple0;
import org.drools.core.function.Tuple.Tuple1;
import org.drools.core.function.Tuple.Tuple2;
import org.drools.core.function.Tuple.Tuple3;
import org.drools.core.function.Tuple.Tuple4;
import org.drools.core.function.Tuple.Tuple5;
import org.drools.core.function.Tuple.Tuple6;

public class RuleOOPathBuilder2 {

    public static class BasePath<END,  A, B, T extends Tuple> {
        protected Function2<PathContext<T>,A,?> fn2;
        protected Predicate2<PathContext<T>,B> flt2;
        protected END end;

        public BasePath(END end) {
            this.end = end;
        }

        public Function2<PathContext<T>, A, ?> function() {
            return fn2;
        }

        public Predicate2<PathContext<T>, B> filter() {
            return flt2;
        }
    }

    public static class Path2<END, T extends Tuple, A, B> extends BasePath<END, A, B, T> {

        public Path2(END end) {
            super(end);
        }

        public END path(Function2<PathContext<T>, A,?> fn2,
                        Predicate2<PathContext<T>,B> flt2) {
            return end;
        }
    }

    public static class Path3<END, T extends Tuple, A, B, C> extends BasePath<END, A, B, T> {
        private  Path2<END, T, B, C> path2;

        public Path3(END end) {
            super(end);
        }

        public Path2<END, T, B, C> path(Function2<PathContext<T>,A,?> fn2,
                                        Predicate2<PathContext<T>,B> flt2) {
            path2 = new Path2<>(end);

            return path2;
        }
    }

    public static class Path4<END, T extends Tuple, A, B, C, D> extends BasePath<END, A, B, T> {
        private Path3<END, T, B, C, D> path3;

        PathNode<A, B, Tuple4<A, B, C, D>> path;

        public Path4(PathNode<A, B, Tuple4<A, B, C, D>> path, END end) {
            super(end);

            this.path = path;
        }

        public Path3<END, T, B, C, D> path(Function2<PathContext<T>,A,?> fn2,
                                           Predicate2<PathContext<T>,B> flt2) {

            path3 = new Path3<>(end);
            return path3;
        }


    }

    public static class Path5<END, T extends Tuple, A, B, C, D, E> extends BasePath<END, A, B, T> {
        private Path4<END, T, B, C, D, E> path4;

        PathNode<A, B, T> path5;


        public Path5(PathNode<A,B,T> path, END end) {
            super(end);
            path5 = path;
        }

        public Path4<END, T, B, C, D, E> path(Function2<PathContext<T>,A,?> fn2,
                                              Predicate2<PathContext<T>,B> flt2) {
            this.fn2 = fn2;
            this.flt2 = flt2;

            PathNode<A, B, T> path = new Path4<>((PathNode<A, B, Tuple4<A, B, C, D>>) path5, end);

            path4 = new Path4<>((PathNode<A, B, Tuple4<A, B, C, D>>)path, end);

            return path4;
        }

        public PathNode<A, B, Tuple5<A, B, C, D, E>> getPath() {
            return path5;
        }
    }

    public static class Path6<END, T extends Tuple, A, B, C, D, E, F> extends BasePath<END, A, B, T> {
        private Path5<END, T, B, C, D, E, F> path5;

        public Path6(END end) {
            super(end);
        }


        public Path5<END, T, B, C, D, E, F> path(Function2<PathContext<T>,A,?> fn2,
                                                 Predicate2<PathContext<T>,B> flt2) {
            this.fn2 = fn2;
            this.flt2 = flt2;

            path5 = new Path5<>(end);
            return path5;
        }
    }

//    RootPathNode<Library> library = new RootPathNode<>((ctx, l) -> true);
//
//    ListPathNode<Library, Room, Tuple2<Library, Room>> room = new ListPathNode<>(AccessType.LIST, 1, (ctx, l) -> l.rooms(), (ctx, r) -> r.name() != null, library);
//
//    ListPathNode<Room, Shelf, Tuple3<Library, Room, Shelf>> shelf = new ListPathNode<>(AccessType.LIST, 2, (ctx, r) -> r.shelves(), (ctx, s) -> s.name() != null, room);
//
//    ListPathNode<Shelf, Book, Tuple4<Library, Room, Shelf, Book>> book = new ListPathNode<>(AccessType.LIST, 3, (ctx, s) -> s.books(), (ctx, b) -> b.title() != null, shelf);
//
//    ListPathNode<Book, Page, Tuple5<Library, Room, Shelf, Book, Page>> page = new ListPathNode<>(AccessType.LIST, 4, (ctx, b) -> b.pages(), (ctx, p) -> p.number() >= 0, book);


//    record OOPathBuilder1<A>(AccessType accessType, Function2<PathContext<Tuple0>,A, ?> fn2, Predicate2<PathContext<Tuple0>,A> flt2) { }
//
//    record OOPathBuilder2<A, B>(AccessType accessType, Function2<PathContext<Tuple1<A>>,A, ?> fn2, Predicate2<PathContext<Tuple2<A, B>>,A> flt2, OOPathBuilder1<A> parent) { }
//
//    record OOPathBuilder3<A, B, C>(AccessType accessType, Function2<PathContext<Tuple3<A, B, C>>, B, ?> fn2, Predicate2<PathContext<Tuple3<A, B, C>>, B> flt2, OOPathBuilder2<A, B> parent) { }
//
//    record OOPathBuilder4<A, B, C, D>(AccessType accessType, Function2<PathContext<Tuple4<A, B, C, D>>, C, ?> fn2, Predicate2<PathContext<Tuple4<A, B, C, D>>,C> flt2, OOPathBuilder3<A, B, C> parent) { }
//
//    record OOPathBuilder5<A, B, C, D, E>(AccessType accessType, Function2<PathContext<Tuple5<A, B, C, D, E>>, D, ?> fn2, Predicate2<PathContext<Tuple5<A, B, C, D, E>>,D> flt2, OOPathBuilder4<A, B, C, D> parent) { }
//
//    record OOPathBuilder6<A, B, C, D, E, F>(AccessType accessType, Function2<PathContext<Tuple6<A, B, C, D, E, F>>, E, ?> fn2, Predicate2<PathContext<Tuple6<A, B, C, D, E, F>>,E> flt2, OOPathBuilder5<A, B, C, D, E> parent) { }
}

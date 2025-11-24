package org.drools.core;

import org.drools.core.OOPathTest.Book;
import org.drools.core.OOPathTest.Library;
import org.drools.core.OOPathTest.Page;
import org.drools.core.OOPathTest.Room;
import org.drools.core.OOPathTest.Shelf;
import org.drools.core.PathNode.ListPathNode;
import org.drools.core.PathNode.RootPathNode;
import org.drools.core.RuleBuilder.Join2First;
import org.drools.core.RuleOOPathBuilder2.Path2;
import org.drools.core.RuleOOPathBuilder2.Path3;
import org.drools.core.RuleOOPathBuilder2.Path4;
import org.drools.core.RuleOOPathBuilder2.Path5;
import org.drools.core.RuleOOPathBuilder2.Path6;
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

public class OOPathBuilder<END> {

    protected END end;

    public OOPathBuilder(END end) {
        this.end = end;
    }

    public static class BuilderEnd<R, L, T extends Tuple> {
        public OOPath<R, L, T> build() {
            return null;
        }
    }

    <A, B, C, D, E> Path4<BuilderEnd<A, E, Tuple5<A, B, C, D, E>>,
                          Tuple5<A, B, C, D, E>, B, C, D, E> path5(Function2<PathContext<Tuple5<A, B, C, D, E>>, A,?> fn2,
                                                                   Predicate2<PathContext<Tuple5<A, B, C, D, E>>,B> flt2) {
        RootPathNode<A> root = new RootPathNode<>((ctx, l) -> true);
        ListPathNode<A, B, Tuple5<A, B, C, D, E>> path = new ListPathNode<>(AccessType.LIST, 1, fn2, flt2, root);


        Path5<BuilderEnd<A, E, Tuple5<A, B, C, D, E>>, Tuple5<A, B, C, D, E>, A, B, C, D, E> path5 = new Path5<>(path,
                                                                                                                 (BuilderEnd<A, E, Tuple5<A, B, C, D, E>>)end);
        return path5.path(fn2, flt2);
    }

    <A, B, C, D> Path3<BuilderEnd, Tuple4<A, B, C, D>, B, C, D> path4(Function2<PathContext<Tuple4<A, B, C, D>>, A,?> fn2,
                                                                      Predicate2<PathContext<Tuple4<A, B, C, D>>,B> flt2) {
        RootPathNode<A> root = new RootPathNode<>((ctx, l) -> true);
        ListPathNode<A, B, Tuple4<A, B, C, D>> path = new ListPathNode<>(AccessType.LIST, 1, fn2, flt2, root);

        Path4<BuilderEnd, Tuple4<A, B, C, D>, A, B, C, D> path5 = new Path4<>(path,
                                                                              (BuilderEnd<A, B, Tuple4<A, B, C, D>>)end);
        return path5.path(fn2, flt2);
    }

    <A, B, C> Path2<BuilderEnd,Tuple3<A, B, C>, B, C> path3(Function2<PathContext<Tuple3<A, B, C>>, A,?> fn2,
                                                            Predicate2<PathContext<Tuple3<A, B, C>>,B> flt2) {
        Path3<BuilderEnd, Tuple3<A, B, C>, A, B, C> path3 = new Path3<>((BuilderEnd<A, C, Tuple3<A, B, C>>)end);
        return path3.path(fn2, flt2);
    }

    <A, B> BuilderEnd path2(Function2<PathContext<Tuple2<A, B>>, A,?> fn2,
                            Predicate2<PathContext<Tuple2<A, B>>,B> flt2) {
        Path2<BuilderEnd, Tuple2<A, B>, A, B> path2 = new Path2<>((BuilderEnd<A, B, Tuple2<A, B>>)end);
        return path2.path(fn2, flt2);
    }

//    public <A> OOPathBuilderA1<A> path() {
//        return path((ctx, o)->true);
//    }
//    public <A> OOPathBuilderA1<A> path(Predicate2<PathContext<Tuple0>, A> flt2) {
//        return new OOPathBuilderA1<>(new OOPathBuilder1<>(AccessType.OBJECT, null, flt2));
//    }
//
//    public static class OOPathBuilderA1<A> {
//        private OOPathBuilder1<A> b1;
//
//        public OOPathBuilderA1(OOPathBuilder1<A> b1) {
//            this.b1 = b1;
//        }
//
//        public <B> OOPathBuilderA2<A, B> path(Function2<PathContext<Tuple1<A>>, A,?> fn2,
//                                              Predicate2<PathContext<Tuple1<A>>, B> flt2) {
//            return new OOPathBuilderA2<>(new OOPathBuilder2<>(AccessType.LIST, fn2, flt2, b1));
//        }
//
//        static <A, B>  PathNode<?, A, Tuple0> build(OOPathBuilder2<A, B> b2) {
//            return new RootPathNode<>( b2.parent().flt2());
//        }
//    }
//
//    public static class OOPathBuilderA2<A, B> {
//        private OOPathBuilder2<A, B> b2;
//
//        public OOPathBuilderA2(OOPathBuilder2<A,B> b2) {
//            this.b2 = b2;
//        }
//
//        public <C> OOPathBuilderA3<A, B, C> path(Function2<PathContext<Tuple2<A, B>>, B,?> fn2,
//                                                 Predicate2<PathContext<Tuple2<A, B>>, C> fl2) {
//            return new OOPathBuilderA3<>(new OOPathBuilder3<>(AccessType.LIST, fn2, fl2, b2));
//        }
//
//        public OOPath<A, B, Tuple1<A>> build() {
//            return new OOPath<>(build(b2));
//        }
//
//        static <A, B>  PathNode<A, B, Tuple1<A>> build(OOPathBuilder2<A, B> b2) {
//            return new ListPathNode<>(AccessType.LIST, 1,
//                                      b2.fn2(), b2.flt2(), OOPathBuilderA1.build(b2));
//        }
//    }
//
//    public static class OOPathBuilderA3<A, B, C> {
//        private OOPathBuilder3<A, B, C> b3;
//
//        public OOPathBuilderA3(OOPathBuilder3<A, B, C> b3) {
//            this.b3 = b3;
//        }
//
//        public <D> OOPathBuilderA4<A, B, C, D> path(Function2<PathContext<Tuple3<A, B, C>>, C,?> fn2,
//                                                    Predicate2<PathContext<Tuple3<A, B, C>>, D> flt2) {
//            return new OOPathBuilderA4<>(new OOPathBuilder4<>(AccessType.LIST, fn2, flt2, b3));
//        }
//
//        public OOPath<A, C, Tuple2<A, B>> build() {
//            return new OOPath<>(build(b3));
//        }
//
//        static <A, B, C>  PathNode<B, C, Tuple2<A, B>> build(OOPathBuilder3<A, B, C> b3) {
//            return new ListPathNode<>(AccessType.LIST, 2,
//                                      b3.fn2(), b3.flt2(), OOPathBuilderA2.build(b3.parent()));
//        }
//    }
//
//    public static class OOPathBuilderA4<A, B, C, D> {
//        private OOPathBuilder4<A, B, C, D> b4;
//
//        public OOPathBuilderA4(OOPathBuilder4<A, B, C, D> p) {
//            this.b4 = p;
//        }
//
//        public <E> OOPathBuilderA5<A, B, C, D, E> path(Function2<PathContext<Tuple4<A, B, C, D>>, D,?> fn2,
//                                                       Predicate2<PathContext<Tuple4<A, B, C, D>>, E> flt2) {
//            return new OOPathBuilderA5<>(new OOPathBuilder5<>(AccessType.LIST, fn2, flt2, b4));
//        }
//
//        public OOPath<A, D, Tuple3<A, B, C>> build() {
//            return new OOPath<>(build(b4));
//        }
//
//        static <A, B, C, D>  PathNode<C, D, Tuple3<A, B, C>> build(OOPathBuilder4<A, B, C, D> b4) {
//            return new ListPathNode<>(AccessType.LIST, 3,
//                                      b4.fn2(), b4.flt2(), OOPathBuilderA3.build(b4.parent()));
//        }
//    }
//
//    public static class OOPathBuilderA5<A, B, C, D, E> {
//        private OOPathBuilder5<A, B, C, D, E> b5;
//
//        public OOPathBuilderA5(OOPathBuilder5<A, B, C, D, E> b5) {
//            this.b5 = b5;
//        }
//
//        public OOPath<A,E, Tuple4<A, B, C, D>> build() {
//            return new OOPath<>(build(b5));
//        }
//
//        static <A, B, C, D, E>  PathNode<D, E, Tuple5<A, B, C, D, E>> build(OOPathBuilder5<A, B, C, D, E> b5) {
//            return new ListPathNode<D, E, Tuple5<A, B, C, D, E>>(AccessType.LIST, 4,
//                                      b5.fn2(), b5.flt2(), OOPathBuilderA4.build(b5.parent()));
//        }
//    }
//
//    record OOPathBuilder1<A>(AccessType accessType, Function2<PathContext<Tuple0>, A, ?> fn2, Predicate2<PathContext<Tuple0>, A> flt2) { }
//
//    record OOPathBuilder2<A, B>(AccessType accessType, Function2<PathContext<Tuple1<A>>, A, ?> fn2, Predicate2<PathContext<Tuple1<A>>, B> flt2, OOPathBuilder1<A> parent) { }
//
//    record OOPathBuilder3<A, B, C>(AccessType accessType, Function2<PathContext<Tuple2<A, B>>, B, ?> fn2, Predicate2<PathContext<Tuple2<A, B>>, C> flt2, OOPathBuilder2<A, B> parent) { }
//
//    record OOPathBuilder4<A, B, C, D>(AccessType accessType, Function2<PathContext<Tuple3<A, B, C>>, C, ?> fn2, Predicate2<PathContext<Tuple3<A, B, C>>, D> flt2, OOPathBuilder3<A, B, C> parent) { }
//
//    record OOPathBuilder5<A, B, C, D, E>(AccessType accessType, Function2<PathContext<Tuple4<A, B, C, D>>, D, ?> fn2, Predicate2<PathContext<Tuple4<A, B, C, D>>, E> flt2, OOPathBuilder4<A, B, C, D> parent) { }
}

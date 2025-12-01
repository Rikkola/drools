package org.drools.core;

import org.drools.core.PathNode.ListPathNode;
import org.drools.core.PathNode.RootPathNode;
import org.drools.core.function.Function2;
import org.drools.core.function.Predicate2;
import org.drools.core.function.Tuple.Tuple0;
import org.drools.core.function.Tuple.Tuple1;
import org.drools.core.function.Tuple.Tuple2;
import org.drools.core.function.Tuple.Tuple3;
import org.drools.core.function.Tuple.Tuple4;

public class MVEL_OOPathBuilder {

    public <A> OOPathBuilderA1<A> path() {
        return path( (ctx, o) ->true);
    }

    public <A> OOPathBuilderA1<A> path(Predicate2<PathContext<Tuple0>, A> flt2) {
        return new OOPathBuilderA1<>(new OOPathBuilder1<>(AccessType.OBJECT, flt2));
    }

    public static class OOPathBuilderA1<A> {
        private OOPathBuilder1<A> b1;

        public OOPathBuilderA1(OOPathBuilder1<A> b1) {
            this.b1 = b1;
        }

        public <B> OOPathBuilderA2<A, B> path(AccessType accessType,
                                              Function2<PathContext<Tuple1<A>>,A, ?> fn2,
                                              Predicate2<PathContext<Tuple1<A>>, B> flt2) {
            return new OOPathBuilderA2<>(new OOPathBuilder2<>(accessType, fn2, flt2, b1));
        }

        static <A>  PathNode<Void, A, Tuple0> build(OOPathBuilder1<A> b1) {
            return new RootPathNode<>( b1.flt2());
        }
    }

    public static class OOPathBuilderA2<A, B> {
        private OOPathBuilder2<A, B> b2;

        public OOPathBuilderA2(OOPathBuilder2<A,B> b2) {
            this.b2 = b2;
        }

        public <C> OOPathBuilderA3<A, B, C> path(AccessType accessType,
                                                 Function2<PathContext<Tuple2<A, B>>,B,?> fn2,
                                                 Predicate2<PathContext<Tuple2<A, B>>,C> fl2) {
            return new OOPathBuilderA3<>(new OOPathBuilder3<>(accessType, fn2, fl2, b2));
        }

        public OOPath<A, B, Tuple1<A>> build() {
            return new OOPath<>(build(b2));
        }

        static <A, B>  PathNode<A, B, Tuple1<A>> build(OOPathBuilder2<A, B> b2) {
            return new ListPathNode<>(AccessType.LIST,
                                      b2.fn2(), b2.flt2(), OOPathBuilderA1.build(b2.parent()));
        }
    }

    public static class OOPathBuilderA3<A, B, C> {
        private OOPathBuilder3<A, B, C> b3;

        public OOPathBuilderA3(OOPathBuilder3<A, B, C> b3) {
            this.b3 = b3;
        }

        public <D> OOPathBuilderA4<A, B, C, D> path(AccessType accessType,
                                                    Function2<PathContext<Tuple3<A, B, C>>,C,?> fn2,
                                                    Predicate2<PathContext<Tuple3<A, B, C>>,D> flt2) {
            return new OOPathBuilderA4<>(new OOPathBuilder4<>(accessType, fn2, flt2, b3));
        }

        public OOPath<A, C, Tuple2<A, B>> build() {
            return new OOPath<>(build(b3));
        }

        static <A, B, C>  PathNode<B, C, Tuple2<A, B>> build(OOPathBuilder3<A, B, C> b3) {
            return new ListPathNode<>(AccessType.LIST,
                                      b3.fn2(), b3.flt2(), OOPathBuilderA2.build(b3.parent()));
        }
    }

    public static class OOPathBuilderA4<A, B, C, D> {
        private OOPathBuilder4<A, B, C, D> b4;

        public OOPathBuilderA4(OOPathBuilder4<A, B, C, D> p) {
            this.b4 = p;
        }

        public <E> OOPathBuilderA5<A, B, C, D, E> path(AccessType accessType,
                                                       Function2<PathContext<Tuple4<A, B, C, D>>,D,?> fn2,
                                                       Predicate2<PathContext<Tuple4<A, B, C, D>>,E> flt2) {
            return new OOPathBuilderA5<>(new OOPathBuilder5<>(accessType, fn2, flt2, b4));
        }

        public OOPath<A, D, Tuple3<A, B, C>> build() {
            return new OOPath<>(build(b4));
        }

        static <A, B, C, D>  PathNode<C, D, Tuple3<A, B, C>> build(OOPathBuilder4<A, B, C, D> b4) {
            return new ListPathNode<>(AccessType.LIST,
                                      b4.fn2(), b4.flt2(), OOPathBuilderA3.build(b4.parent()));
        }
    }

    public static class OOPathBuilderA5<A, B, C, D, E> {
        private OOPathBuilder5<A, B, C, D, E> b5;

        public OOPathBuilderA5(OOPathBuilder5<A, B, C, D, E> b5) {
            this.b5 = b5;
        }

        public OOPath<A,E, Tuple4<A, B, C, D>> build() {
            return new OOPath<>(build(b5));
        }

        static <A, B, C, D, E>  PathNode<D, E, Tuple4<A, B, C, D>> build(OOPathBuilder5<A, B, C, D, E> b5) {
            return new ListPathNode<>(AccessType.LIST,
                                      b5.fn2 (), b5.flt2(), OOPathBuilderA4.build(b5.parent()));
        }
    }

    record OOPathBuilder1<A>(AccessType accessType, Predicate2<PathContext<Tuple0>, A> flt2) { }

    record OOPathBuilder2<A, B>(AccessType accessType, Function2<PathContext<Tuple1<A>>, A, ?> fn2, Predicate2<PathContext<Tuple1<A>>, B> flt2, OOPathBuilder1<A> parent) { }

    record OOPathBuilder3<A, B, C>(AccessType accessType, Function2<PathContext<Tuple2<A, B>>, B, ?> fn2, Predicate2<PathContext<Tuple2<A, B>>, C> flt2, OOPathBuilder2<A, B> parent) { }

    record OOPathBuilder4<A, B, C, D>(AccessType accessType, Function2<PathContext<Tuple3<A, B, C>>, C, ?> fn2, Predicate2<PathContext<Tuple3<A, B, C>>,D> flt2, OOPathBuilder3<A, B, C> parent) { }

    record OOPathBuilder5<A, B, C, D, E>(AccessType accessType, Function2<PathContext<Tuple4<A, B, C, D>>, D, ?> fn2, Predicate2<PathContext<Tuple4<A, B, C, D>>, E> flt2, OOPathBuilder4<A, B, C, D> parent) { }
}
package org.drools.core;

import org.drools.core.PathNode.ListPathNode;
import org.drools.core.PathNode.RootPathNode;
import org.drools.core.function.Function1;
import org.drools.core.function.Predicate1;
import org.drools.core.function.Tuple.Tuple1;
import org.drools.core.function.Tuple.Tuple2;
import org.drools.core.function.Tuple.Tuple3;
import org.drools.core.function.Tuple.Tuple4;
import org.drools.core.function.Tuple.Tuple5;

public class RuleOOPathBuilder {

    public <A> OOPathBuilderA1<A> path() {
        return path(o->true);
    }

    public <A> OOPathBuilderA1<A> path(Predicate1<A> flt1) {
        return new OOPathBuilderA1<>(new OOPathBuilder1<>(AccessType.OBJECT, null, flt1));
    }

    public static class OOPathBuilderA1<A> {
        private OOPathBuilder1<A> b1;

        public OOPathBuilderA1(OOPathBuilder1<A> b1) {
            this.b1 = b1;
        }

        public <B> OOPathBuilderA2<A, B> path(AccessType accessType,
                                              Function1<A,?> fn1,
                                              Predicate1<B> flt1) {
            return new OOPathBuilderA2<>(new OOPathBuilder2<>(accessType, fn1, flt1, b1));
        }

        static <A, B>  PathNode<?, A, Tuple1<A>> build(OOPathBuilder2<A, B> b2) {
            return new RootPathNode<>( b2.parent().flt1());
        }
    }

    public static class OOPathBuilderA2<A, B> {
        private OOPathBuilder2<A, B> b2;

        public OOPathBuilderA2(OOPathBuilder2<A,B> b2) {
            this.b2 = b2;
        }

        public <C> OOPathBuilderA3<A, B, C> path(Function1<B,?> fn1,
                                                 Predicate1<C> fl1) {
            return new OOPathBuilderA3<>(new OOPathBuilder3<>(AccessType.DYNAMIC, fn1, fl1, b2));
        }

        public OOPath<A, B, Tuple2<A, B>> build() {
            return new OOPath<>(build(b2));
        }

        static <A, B>  PathNode<A, B, Tuple2<A, B>> build(OOPathBuilder2<A, B> b2) {
            return new ListPathNode<>(AccessType.LIST, 1,
                                      b2.fn1(), b2.flt1(), OOPathBuilderA1.build(b2));
        }
    }

    public static class OOPathBuilderA3<A, B, C> {
        private OOPathBuilder3<A, B, C> b3;

        public OOPathBuilderA3(OOPathBuilder3<A, B, C> b3) {
            this.b3 = b3;
        }

        public <D> OOPathBuilderA4<A, B, C, D> path(AccessType accessType,
                                                Function1<C,?> fn1,
                                                Predicate1<D> flt1) {
            return new OOPathBuilderA4<>(new OOPathBuilder4<>(accessType, fn1, flt1, b3));
        }

        public OOPath<A, C, Tuple3<A, B, C>> build() {
            return new OOPath<>(build(b3));
        }

        static <A, B, C>  PathNode<B, C, Tuple3<A, B, C>> build(OOPathBuilder3<A, B, C> b3) {
            return new ListPathNode<>(AccessType.LIST, 2,
                                      b3.fn1(), b3.flt1(), OOPathBuilderA2.build(b3.parent()));
        }
    }

    public static class OOPathBuilderA4<A, B, C, D> {
        private OOPathBuilder4<A, B, C, D> b4;

        public OOPathBuilderA4(OOPathBuilder4<A, B, C, D> p) {
            this.b4 = p;
        }

        public <E> OOPathBuilderA5<A, B, C, D, E> path(AccessType accessType,
                                                       Function1<D,?> fn1,
                                                       Predicate1<E> flt1) {
            return new OOPathBuilderA5<>(new OOPathBuilder5<>(accessType, fn1, flt1, b4));
        }

        public OOPath<A, D, Tuple4<A, B, C, D>> build() {
            return new OOPath<>(build(b4));
        }

        static <A, B, C, D>  PathNode<C, D, Tuple4<A, B, C, D>> build(OOPathBuilder4<A, B, C, D> b4) {
            return new ListPathNode<>(AccessType.LIST, 3,
                                      b4.fn1(), b4.flt1(), OOPathBuilderA3.build(b4.parent()));
        }
    }

    public static class OOPathBuilderA5<A, B, C, D, E> {
        private OOPathBuilder5<A, B, C, D, E> b5;

        public OOPathBuilderA5(OOPathBuilder5<A, B, C, D, E> b5) {
            this.b5 = b5;
        }

        public OOPath<A,E, Tuple5<A, B, C, D, E>> build() {
            return new OOPath<>(build(b5));
        }

        static <A, B, C, D, E>  PathNode<D, E, Tuple5<A, B, C, D, E>> build(OOPathBuilder5<A, B, C, D, E> b5) {
            return new ListPathNode<>(AccessType.LIST, 4,
                                      b5.fn1(), b5.flt1(), OOPathBuilderA4.build(b5.parent()));
        }
    }

    record OOPathBuilder1<A>(AccessType accessType, Function1<A, ?> fn1, Predicate1<A> flt1) { }

    record OOPathBuilder2<A, B>(AccessType accessType, Function1<A, ?> fn1, Predicate1<B> flt1, OOPathBuilder1<A> parent) { }

    record OOPathBuilder3<A, B, C>(AccessType accessType, Function1<B, ?> fn1, Predicate1<C> flt1, OOPathBuilder2<A, B> parent) { }

    record OOPathBuilder4<A, B, C, D>(AccessType accessType, Function1<C, ?> fn1, Predicate1<D> flt1, OOPathBuilder3<A, B, C> parent) { }

    record OOPathBuilder5<A, B, C, D, E>(AccessType accessType, Function1<D, ?> fn1, Predicate1<E> flt1, OOPathBuilder4<A, B, C, D> parent) { }
}

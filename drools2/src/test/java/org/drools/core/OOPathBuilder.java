package org.drools.core;

import org.drools.core.PathNode.RootPathNode;
import org.drools.core.function.Function1;
import org.drools.core.function.Predicate1;

import org.drools.core.function.Tuple.Tuple1;
import org.drools.core.function.Tuple.Tuple2;
import org.drools.core.function.Tuple.Tuple3;
import org.drools.core.function.Tuple.Tuple4;
import org.drools.core.function.Tuple.Tuple5;

public class OOPathBuilder {

    public static <A, B> OOPathBuilderA2<A, B> path(AccessType accessType,
                                                    Function1<A,?> fn1,
                                                    Predicate1<B> flt1) {
        return new OOPathBuilderA2<>(new OOPathBuilder2<>(accessType, fn1, flt1));
    }

    public static class OOPathBuilderA2<A, B> {
        private OOPathBuilder2<A, B> p;

        public OOPathBuilderA2(OOPathBuilder2<A,B> p) {
            this.p = p;
        }

        public <C> OOPathBuilderA3<A, B, C> path(AccessType accessType,
                                                 Function1<B,?> fn2,
                                                 Predicate1<C> fl2) {
            return new OOPathBuilderA3<>(new OOPathBuilder3<>(accessType, fn2, fl2, p));
        }

        public OOPath<A, B, Tuple2<A, B>> build() {
//            RootPathNode<Library, Tuple1<Library>> library = new RootPathNode<>(l1, tuple5);
//            ListPathNode<Library, Room, Tuple2<Library, Room>> room = new ListPathNode<>(AccessType.LIST, 1, tuple5, l -> l.rooms(), r -> r.name() != null, library);

//            Tuple2<A, B> tuple2  = new Tuple2<>();
//            RootPathNode<A, Tuple1<A>> library = new RootPathNode<>(l1, tuple5);

            return null;
        }
    }

    public static class OOPathBuilderA3<A, B, C> {
        private OOPathBuilder3<A, B, C> p;

        public OOPathBuilderA3(OOPathBuilder3<A, B, C> p) {
            this.p = p;
        }

        public <D> OOPathBuilderA4<A, B, C, D> path(AccessType accessType,
                                                Function1<C,?> fn1,
                                                Predicate1<D> flt1) {
            return new OOPathBuilderA4<>(new OOPathBuilder4<>(accessType, fn1, flt1, p));
        }

        public OOPath<A, C, Tuple3<A, B, C>> build() {
            return null;
        }
    }

    public static class OOPathBuilderA4<A, B, C, D> {
        private OOPathBuilder4<A, B, C, D> p;

        public OOPathBuilderA4(OOPathBuilder4<A, B, C, D> p) {
            this.p = p;
        }

        public <E> OOPathBuilderA5<A, B, C, D, E> path(AccessType accessType,
                                                    Function1<D,?> fn1,
                                                    Predicate1<E> flt1) {
            return new OOPathBuilderA5<>(new OOPathBuilder5<>(accessType, fn1, flt1, p));
        }

        public OOPath<A, D, Tuple4<A, B, C, D>> build() {
            return null;
        }
    }

    public static class OOPathBuilderA5<A, B, C, D, E> {
        private OOPathBuilder5<A, B, C, D, E> p;

        public OOPathBuilderA5(OOPathBuilder5<A, B, C, D, E> p) {
            this.p = p;
        }

        public OOPath<A,E, Tuple5<A, B, C, D, E>> build() {
            return null;
        }
    }

    record OOPathBuilder2<A, B>(AccessType accessType, Function1<A, ?> fn1, Predicate1<B> flt1) { }

    record OOPathBuilder3<A, B, C>(AccessType accessType, Function1<B, ?> fn1, Predicate1<C> flt1, OOPathBuilder2<A, B> p) { }

    record OOPathBuilder4<A, B, C, D>(AccessType accessType, Function1<C, ?> fn1, Predicate1<D> flt1, OOPathBuilder3<A, B, C> p) { }

    record OOPathBuilder5<A, B, C, D, E>(AccessType accessType, Function1<D, ?> fn1, Predicate1<E> flt1, OOPathBuilder4<A, B, C, D> p) { }

//    record OOPathBuilderB<A, B>(AccessType accessType, Function2<A, B, ?> fn2, Predicate2<A, B> flt2, OOPathBuilderA<A> a) {}
//
//    record OOPathBuilderC<A, B, C>(AccessType accessType, Function3<A, B, C, ?> fn3, Predicate3<A, B, C> flt3, OOPathBuilderB<A, B> b) {}
//
//    record OOPathBuilderD<A, B, C, D>(AccessType accessType, Function4<A, B, C, D, ?> fn1, Predicate4<A, B, C, D> flt1, OOPathBuilderC<A, B, C> c) {}

//    public static class OOPathBuilderA1<A> {
//        private OOPathBuilderA<A> a;
//
//        public OOPathBuilderA1(OOPathBuilderA<A> a) {
//            this.a = a;
//        }
//
//        public <B> OOPathBuilderB1<A, B> path(AccessType accessType,
//                                              Function2<A, B, ?> fn2,
//                                              Predicate2<A, B> flt2) {
//            return new OOPathBuilderB1<>(new OOPathBuilderB<>(accessType, fn2, flt2, a));
//        }
//
//        OOPath<A, ?> build() {
//            return null;
//        }
//    }
//
//    public static class OOPathBuilderB1<A, B> {
//        private OOPathBuilderB<A, B> b;
//
//        public OOPathBuilderB1(OOPathBuilderB<A, B> b) {
//            this.b = b;
//        }
//
//        public <C> OOPathBuilderC1<A, B, C> path(AccessType accessType,
//                                                 Function3<A, B, C, ?> fn3,
//                                                 Predicate3<A, B, C> flt3) {
//            return new OOPathBuilderC1<>(new OOPathBuilderC<>(accessType, fn3, flt3, b));
//        }
//    }
//
//    public static class OOPathBuilderC1<A, B, C> {
//        private OOPathBuilderC<A, B, C> c;
//
//        public OOPathBuilderC1(OOPathBuilderC<A, B, C> c) {
//            this.c = c;
//        }
//
//        public <D> OOPathBuilderD1<A, B, C, D> path(AccessType accessType,
//                                                 Function4<A, B, C, D, ?> fn4,
//                                                 Predicate4<A, B, C, D> flt4) {
//            return new OOPathBuilderD1<>(new OOPathBuilderD<>(accessType, fn4, flt4, c));
//        }
//    }
//
//    public static class OOPathBuilderD1<A, B, C, D> {
//        private OOPathBuilderD<A, B, C, D> d;
//
//        public OOPathBuilderD1(OOPathBuilderD<A, B, C, D> d) {
//            this.d = d;
//        }
//    }

//    public static class OOPathBuilder2 {
//        private OOPathBuilder1 OOPathBuilder1;
//
//        public OOPathBuilder2(OOPathBuilder1 OOPathBuilder1) {
//            this.OOPathBuilder1 = OOPathBuilder1;
//        }
//
//        public OOPath build() {
//           return null;
//        }
//
//        public OOPathBuilder2(AccessType accessType) {
//
//        }
//
//        public OOPathBuilder2 fn(Function1 fn1) {
//
//        }
//
//        public OOPathBuilder2 test(Predicate1 test1) {
//
//        }
//        //Function1 fn1, Predicate1 test1
//    }
}

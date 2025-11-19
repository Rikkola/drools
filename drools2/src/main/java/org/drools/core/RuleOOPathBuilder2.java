package org.drools.core;

import org.drools.core.function.Function2;
import org.drools.core.function.Predicate2;
import org.drools.core.function.Tuple;
import org.drools.core.function.Tuple.Tuple1;
import org.drools.core.function.Tuple.Tuple2;
import org.drools.core.function.Tuple.Tuple3;
import org.drools.core.function.Tuple.Tuple4;
import org.drools.core.function.Tuple.Tuple5;
import org.drools.core.function.Tuple.Tuple6;

public class RuleOOPathBuilder2 {

    public static class BasePath<A, B, T extends Tuple> {
        protected Function2<PathContext<A, T>,A,?> fn2;
        protected Predicate2<PathContext<B, T>,B> flt2;

        public Function2<PathContext<A, T>, A, ?> function() {
            return fn2;
        }

        public Predicate2<PathContext<B, T>, B> filter() {
            return flt2;
        }
    }

    public static class Path2<END, T extends Tuple, A, B> extends BasePath<A, B, T> {
        public END path(Function2<PathContext<?, T>, A,?> fn2,
                        Predicate2<PathContext<B, T>,B> flt2) {
            return null;
        }
    }

    public static class Path3<END, T extends Tuple, A, B, C> extends BasePath<A, B, T> {
        private  Path2<END, T, B, C> path2;

        public Path2<END, T, B, C> path(Function2<PathContext<?, T>,A,?> fn2,
                                        Predicate2<PathContext<B, T>,B> flt2) {
            path2 = new Path2<>();

            return path2;
        }
    }

    public static class Path4<END, T extends Tuple, A, B, C, D> extends BasePath<A, B, T> {
        private Path3<END, T, B, C, D> path3;

        public Path3<END, T, B, C, D> path(Function2<PathContext<?, T>,A,?> fn2,
                                           Predicate2<PathContext<B, T>,B> flt2) {
            path3 = new Path3<>();

            return path3;
        }


    }

    public static class Path5<END, T extends Tuple, A, B, C, D, E> extends BasePath<A, B, T> {
        private Path4<END, T, B, C, D, E> path4;

        public Path4<END, T, B, C, D, E> path(Function2<PathContext<A, ?>,A,?> fn2,
                                              Predicate2<PathContext<B, T>,B> flt2) {
            this.fn2 = fn2;
            this.flt2 = flt2;

            path4 = new Path4<>();
            return path4;
        }
    }

    public static class Path6<END, T extends Tuple, A, B, C, D, E, F> extends BasePath<A, B, T> {
        private Path5<END, T, B, C, D, E, F> path5;

        public Path5<END, T, B, C, D, E, F> path(Function2<PathContext<?, T>,A,?> fn2,
                                                 Predicate2<PathContext<B, T>,B> flt2) {
            this.fn2 = fn2;
            this.flt2 = flt2;

            path5 = new Path5<>();
            return path5;
        }
    }

    record OOPathBuilder1<A>(AccessType accessType, Function2<PathContext<A, Tuple1<A>>,A, ?> fn2, Predicate2<PathContext<?, Tuple1<A>>,A> flt2) { }

    record OOPathBuilder2<A, B>(AccessType accessType, Function2<PathContext<B, Tuple2<A, B>>,A, ?> fn2, Predicate2<PathContext<B, Tuple2<A, B>>,A> flt2, OOPathBuilder1<A> parent) { }

    record OOPathBuilder3<A, B, C>(AccessType accessType, Function2<PathContext<C, Tuple3<A, B, C>>, B, ?> fn2, Predicate2<PathContext<C, Tuple3<A, B, C>>, B> flt2, OOPathBuilder2<A, B> parent) { }

    record OOPathBuilder4<A, B, C, D>(AccessType accessType, Function2<PathContext<D, Tuple4<A, B, C, D>>, C, ?> fn2, Predicate2<PathContext<D, Tuple4<A, B, C, D>>,C> flt2, OOPathBuilder3<A, B, C> parent) { }

    record OOPathBuilder5<A, B, C, D, E>(AccessType accessType, Function2<PathContext<E, Tuple5<A, B, C, D, E>>, D, ?> fn2, Predicate2<PathContext<E, Tuple5<A, B, C, D, E>>,D> flt2, OOPathBuilder4<A, B, C, D> parent) { }

    record OOPathBuilder6<A, B, C, D, E, F>(AccessType accessType, Function2<PathContext<F, Tuple6<A, B, C, D, E, F>>, E, ?> fn2, Predicate2<PathContext<F, Tuple6<A, B, C, D, E, F>>,E> flt2, OOPathBuilder5<A, B, C, D, E> parent) { }

//    record OOPathBuilder1<A>(AccessType accessType, Predicate2<PathContext<A, Tuple1<A>>, A> flt2) { }
//
//    record OOPathBuilder2<A, B>(AccessType accessType, Function2<PathContext<A, Tuple1<A>>, A, ?> fn2, Predicate2<PathContext<B, Tuple2<A, B>>, B> flt2, OOPathBuilder1<A> parent) { }
//
//    record OOPathBuilder3<A, B, C>(AccessType accessType, Function2<PathContext<B, Tuple2<A, B>>, B, ?> fn2, Predicate2<PathContext<C, Tuple3<A, B, C>>, C> flt2, OOPathBuilder2<A, B> parent) { }
//
//    record OOPathBuilder4<A, B, C, D>(AccessType accessType, Function2<PathContext<C, Tuple3<A, B, C>>, C, ?> fn2, Predicate2<PathContext<D, Tuple4<A, B, C, D>>,D> flt2, OOPathBuilder3<A, B, C> parent) { }
//
//    record OOPathBuilder5<A, B, C, D, E>(AccessType accessType, Function2<PathContext<D, Tuple4<A, B, C, D>>, D, ?> fn2, Predicate2<PathContext<E, Tuple5<A, B, C, D, E>>, E> flt2, OOPathBuilder4<A, B, C, D> parent) { }
//
//    record OOPathBuilder6<A, B, C, D, E, F>(AccessType accessType, Function2<PathContext<E, Tuple5<A, B, C, D, E>>, E, ?> fn2, Predicate2<PathContext<F, Tuple6<A, B, C, D, E, F>>, F> flt2, OOPathBuilder5<A, B, C, D, E> parent) { }
}

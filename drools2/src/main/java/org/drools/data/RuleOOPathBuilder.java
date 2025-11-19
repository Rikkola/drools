package org.drools.data;

import org.drools.core.AccessType;
import org.drools.core.OOPath;
import org.drools.core.PathContext;
import org.drools.core.PathNode;
import org.drools.core.PathNode.ListPathNode;
import org.drools.core.PathNode.RootPathNode;
import org.drools.core.function.Function2;
import org.drools.core.function.Predicate2;
import org.drools.core.function.Tuple.Tuple1;
import org.drools.core.function.Tuple.Tuple2;
import org.drools.core.function.Tuple.Tuple3;
import org.drools.core.function.Tuple.Tuple4;
import org.drools.core.RuleBuilder.BaseRuleBuilder;
import org.drools.core.function.Tuple.Tuple5;
import org.drools.core.function.Tuple.Tuple6;

public class RuleOOPathBuilder {

    public <END extends BaseRuleBuilder, A> OOPathBuilderA1<END, A> path() {
        return path((ctx, o) ->true);
    }

    public <END extends BaseRuleBuilder,A> OOPathBuilderA1<END, A> path(Predicate2<PathContext<A, Tuple1<A>>, A> flt2) {
        return new OOPathBuilderA1<>(new OOPathBuilder1<>(AccessType.OBJECT, flt2));
    }

    public static class OOPathBuilderA1<END extends BaseRuleBuilder, A> {
        private OOPathBuilder1<A> b1;

        public OOPathBuilderA1(OOPathBuilder1<A> b1) {
            this.b1 = b1;
        }

        public <B> OOPathBuilderA2<END, A, B> path(AccessType accessType,
                                                   Function2<PathContext<A, Tuple1<A>>,A, ?> fn2,
                                                   Predicate2<PathContext<B, Tuple2<A, B>>, B> flt2) {
            return new OOPathBuilderA2<>(new OOPathBuilder2<>(accessType, fn2, flt2, b1));
        }

        private static <A> PathNode<A, A, Tuple1<A>, Tuple1<A>> build(OOPathBuilder1<A> b1) {
            return new RootPathNode<>( b1.flt2());
        }
    }

    public static class OOPathBuilderA2<END extends BaseRuleBuilder, A, B> {
        private OOPathBuilder2<A, B> b2;

        public OOPathBuilderA2(OOPathBuilder2<A,B> b2) {
            this.b2 = b2;
        }

        public <C> OOPathBuilderA3<END, A, B, C> path(Function2<PathContext<B, Tuple2<A, B>>,B,?> fn2,
                                                      Predicate2<PathContext<C, Tuple3<A, B, C>>,C> fl2) {
            return new OOPathBuilderA3<>(new OOPathBuilder3<>(AccessType.DYNAMIC, fn2, fl2, b2));
        }

        public OOPath<A, B, Tuple2<A, B>> build() {
            return new OOPath<>(build(b2));
        }

        private static <A, B>  PathNode<A, B, Tuple1<A>, Tuple2<A, B>> build(OOPathBuilder2<A, B> b2) {
            return new ListPathNode<>(AccessType.LIST, 1,
                                      b2.fn2(), b2.flt2(), OOPathBuilderA1.build(b2.parent()));
        }
    }

    public static class OOPathBuilderA3<END extends BaseRuleBuilder, A, B, C> {
        private OOPathBuilder3<A, B, C> b3;

        public OOPathBuilderA3(OOPathBuilder3<A, B, C> b3) {
            this.b3 = b3;
        }

        public <D> OOPathBuilderA4<END, A, B, C, D> path(Function2<PathContext<C,Tuple3<A, B, C>>,C,?> fn2,
                                                         Predicate2<PathContext<D,Tuple4<A, B, C, D>>,D> flt2) {
            return new OOPathBuilderA4<>(new OOPathBuilder4<>(AccessType.DYNAMIC, fn2, flt2, b3));
        }

        public OOPath<A, C, Tuple3<A, B, C>> build() {
            return new OOPath<>(build(b3));
        }

        private static <A, B, C>  PathNode<B, C, Tuple2<A, B>, Tuple3<A, B, C>> build(OOPathBuilder3<A, B, C> b3) {
            return new ListPathNode<>(AccessType.LIST, 2,
                                      b3.fn2(), b3.flt2(), OOPathBuilderA2.build(b3.parent()));
        }
    }

    public static class OOPathBuilderA4<END extends BaseRuleBuilder, A, B, C, D> {
        private OOPathBuilder4<A, B, C, D> b4;

        public OOPathBuilderA4(OOPathBuilder4<A, B, C, D> p) {
            this.b4 = p;
        }

        public <E> OOPathBuilderA5<END, A, B, C, D, E> path(Function2<PathContext<D,Tuple4<A, B, C, D>>,D,?> fn2,
                                                            Predicate2<PathContext<E,Tuple5<A, B, C, D, E>>,E> flt2) {
            return new OOPathBuilderA5<>(new OOPathBuilder5<>(AccessType.DYNAMIC, fn2, flt2, b4));
        }

        public OOPath<A, D, Tuple4<A, B, C, D>> build() {
            return new OOPath<>(build(b4));
        }

        private static <A, B, C, D>  PathNode<C, D, Tuple3<A, B, C>, Tuple4<A, B, C, D>> build(OOPathBuilder4<A, B, C, D> b4) {
            return new ListPathNode<>(AccessType.LIST, 3,
                                      b4.fn2(), b4.flt2(), OOPathBuilderA3.build(b4.parent()));
        }
    }

    public static class OOPathBuilderA5<END extends BaseRuleBuilder, A, B, C, D, E> {
        private OOPathBuilder5<A, B, C, D, E> b5;

        public OOPathBuilderA5(OOPathBuilder5<A, B, C, D, E> b5) {
            this.b5 = b5;
        }

        public <F> OOPathBuilderA6<END, A, B, C, D, E, F> path(Function2<PathContext<E,Tuple5<A, B, C, D, E>>,E,?> fn2,
                                                               Predicate2<PathContext<F,Tuple6<A, B, C, D, E, F>>,F> flt2) {
            return new OOPathBuilderA6<>(new OOPathBuilder6<>(AccessType.DYNAMIC, fn2, flt2, b5)); //flt2, b5.parent()));
        }
    }

    public static class OOPathBuilderA6<END extends BaseRuleBuilder, A, B, C, D, E, F> {
        private OOPathBuilder6<A, B, C, D, E, F> b6;

        public OOPathBuilderA6(OOPathBuilder6<A, B, C, D, E, F> b6) {
            this.b6 = b6;
        }


        public END endPath() {
            return (END) null;
        }
    }

    record OOPathBuilder1<A>(AccessType accessType, Predicate2<PathContext<A, Tuple1<A>>, A> flt2) { }

    record OOPathBuilder2<A, B>(AccessType accessType, Function2<PathContext<A, Tuple1<A>>, A, ?> fn2, Predicate2<PathContext<B, Tuple2<A, B>>, B> flt2, OOPathBuilder1<A> parent) { }

    record OOPathBuilder3<A, B, C>(AccessType accessType, Function2<PathContext<B, Tuple2<A, B>>, B, ?> fn2, Predicate2<PathContext<C, Tuple3<A, B, C>>, C> flt2, OOPathBuilder2<A, B> parent) { }

    record OOPathBuilder4<A, B, C, D>(AccessType accessType, Function2<PathContext<C, Tuple3<A, B, C>>, C, ?> fn2, Predicate2<PathContext<D, Tuple4<A, B, C, D>>,D> flt2, OOPathBuilder3<A, B, C> parent) { }

    record OOPathBuilder5<A, B, C, D, E>(AccessType accessType, Function2<PathContext<D, Tuple4<A, B, C, D>>, D, ?> fn2, Predicate2<PathContext<E, Tuple5<A, B, C, D, E>>, E> flt2, OOPathBuilder4<A, B, C, D> parent) { }

    record OOPathBuilder6<A, B, C, D, E, F>(AccessType accessType, Function2<PathContext<E, Tuple5<A, B, C, D, E>>, E, ?> fn2, Predicate2<PathContext<F, Tuple6<A, B, C, D, E, F>>, F> flt2, OOPathBuilder5<A, B, C, D, E> parent) { }
}

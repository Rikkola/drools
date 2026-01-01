package org.drools.core;

import org.drools.core.function.BiLinearTuplePredicateCache;
import org.drools.core.function.Predicate5;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BiLinearTuplePredicateCacheTest {

    @Test
    public void test1() {
        TupleImpl<B> b = rootTuple(new B("b1"));
        TupleImpl<C> c = rootTuple(new C("c1"));
        TupleImpl<D> d = rightTuple(new D("d1"));
        TupleImpl<E> e = rightTuple(new E("e1"));
        TupleImpl<F> f = rightTuple(new F("f1"));

        //TupleImpl<C> bc = joinTuple(b, c);
        TupleImpl<D> cd = rightJoinTuple(c, d);
        TupleImpl<D> bcd = leftJoinTuple(b, cd);

        //TupleImpl<E> bcde = joinTuple(d, e);

        record DS() {}

        BiLinearTuplePredicateCache cache    = new BiLinearTuplePredicateCache();
        final List<String>              recorder = new ArrayList<>();
        cache.setLeft(bcd, (Predicate5<Context<DS>, B, C, D, E>) (ctx, p1, p2, p3, p4) -> recorder.add(p1.name() + ":" + p2.name() + ":" +
                                                                                                                               p3.name() + ":" + p4.name()));
        cache.applyRight(new ContextPojoDS(new DS()), e);
        cache.clear();
        assertThat(recorder).containsExactly("b1:c1:d1:e1");
//
//        recorder.clear();
//        cache.setLeft(bc, (Predicate4<Context<DS>, B, C, D>) (ctx, p1, p2, p3) -> recorder.add(p1.name() + ":" + p2.name() + ":" + p3.name()));
//        cache.applyRight(new ContextPojoDS(new DS()), d);
//        cache.clear();
//        assertThat(recorder).containsExactly("b1:c1:d1");
//
//        recorder.clear();
//        cache.setLeft(bcd, (Predicate5<Context<DS>, B, C, D, E>) (ctx, p1, p2, p3, p4) -> recorder.add(p1.name() + ":" + p2.name() + ":" +
//                                                                                                                                p3.name() + ":" + p4.name()));
//        cache.applyRight(new ContextPojoDS(new DS()), e);
//        cache.clear();
//        assertThat(recorder).containsExactly("b1:c1:d1:e1");

    }

    private static int counter;
    public <T> TupleImpl<T> rootTuple(T o) {
        return new LeftTuple(new DataHandleImpl<>(counter++, o), new Sink() {
            @Override
            public char[] getId() {
                return new char[0];
            }
        }, true);
    }

    public <T> TupleImpl<T> rightTuple(T o) {
        return new RightTuple(new DataHandleImpl<>(counter++, o), new Sink() {
            @Override
            public char[] getId() {
                return new char[0];
            }
        }, true);
    }

    public <T1, T2> TupleImpl<T2> rightJoinTuple(TupleImpl <T1> leftTuple, TupleImpl<T2> rightTuple) {
        return new RightTuple(leftTuple, rightTuple, new Sink() {
            @Override
            public char[] getId() {
                return new char[0];
            }
        });
    }

    public <T1, T2> TupleImpl<T2> leftJoinTuple(TupleImpl <T1> leftTuple, TupleImpl<T2> rightTuple) {
        return new LeftTuple(leftTuple, rightTuple, new Sink() {
            @Override
            public char[] getId() {
                return new char[0];
            }
        });
    }

    record B(String name) {}

    record C(String name) {}

    record D(String name) {}

    record E(String name) {}

    record F(String name) {}
}

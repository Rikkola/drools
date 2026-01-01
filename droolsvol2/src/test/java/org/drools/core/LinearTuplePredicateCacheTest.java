package org.drools.core;

import org.drools.core.function.LinearTuplePredicateCache;
import org.drools.core.function.Predicate1;
import org.drools.core.function.Predicate2;
import org.drools.core.function.Predicate3;
import org.drools.core.function.Predicate4;
import org.drools.core.function.Predicate5;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

public class LinearTuplePredicateCacheTest {

    @Test
    public void test1() {
        TupleImpl<B> b = rootTuple(new B("b1"));
        TupleImpl<C> c = rightTuple(new C("c1"));
        TupleImpl<D> d = rightTuple(new D("d1"));
        TupleImpl<E> e = rightTuple(new E("e1"));

        TupleImpl<C> bc = joinTuple(b, c);
        TupleImpl<D> bcd = joinTuple(bc, d);

        record DS() {}

        LinearTuplePredicateCache cache    = new LinearTuplePredicateCache();
        final List<String>              recorder = new ArrayList<>();
        cache.setLeft(b, (Predicate3<Context<DS>, B, C>) (ctx, p1, p2) -> recorder.add(p1.name() + ":" + p2.name()));
        cache.applyRight(new ContextPojoDS(new DS()), c);
        cache.clear();
        assertThat(recorder).containsExactly("b1:c1");

        recorder.clear();
        cache.setLeft(bc, (Predicate4<Context<DS>, B, C, D>) (ctx, p1, p2, p3) -> recorder.add(p1.name() + ":" + p2.name() + ":" + p3.name()));
        cache.applyRight(new ContextPojoDS(new DS()), d);
        cache.clear();
        assertThat(recorder).containsExactly("b1:c1:d1");

        recorder.clear();
        cache.setLeft(bcd, (Predicate5<Context<DS>, B, C, D, E>) (ctx, p1, p2, p3, p4) -> recorder.add(p1.name() + ":" + p2.name() + ":" +
                                                                                                                               p3.name() + ":" + p4.name()));
        cache.applyRight(new ContextPojoDS(new DS()), e);
        cache.clear();
        assertThat(recorder).containsExactly("b1:c1:d1:e1");

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

    public <T1, T2> TupleImpl<T2> joinTuple(TupleImpl <T1> leftTuple, TupleImpl<T2> rightTuple) {
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

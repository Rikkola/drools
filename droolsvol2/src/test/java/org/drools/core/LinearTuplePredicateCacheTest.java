package org.drools.core;

import org.drools.core.TupleUtil.DynamicInvocationHandler;
import org.drools.core.function.LinearTuplePredicateCache;
import org.drools.core.function.Predicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class LinearTuplePredicateCacheTest {
    private char[] letters = TupleUtil.getAlphabet();

    @Test
    public void testExecutionCacheLeft() throws Exception {
        List<TupleImpl<?>> objects = new ArrayList<>();
        List<TupleImpl<?>> joins = new ArrayList<>();

        for(int i = 1; i < 10; i++) {
            objects.add(TupleUtil.objectTuple(TupleUtil.getAlphabet()[i], i + 1, 1));
        }

        joins.add(objects.get(0));

        for(int i = 1; i < 9; i++) {
            joins.add(TupleUtil.joinTuple(joins.get(i - 1), objects.get(i), objects.size() + i));
        }

        for ( int i = 3; i < 10; i++ ) {
            Class cls = Class.forName(Predicate.class.getName() + (i));

            final List<String> recorder = new ArrayList<>();
            Predicate proxy =  (Predicate) Proxy.newProxyInstance(
                    LinearTuplePredicateCacheTest.class.getClassLoader(),
                    new Class[] { cls },
                    new DynamicInvocationHandler(recorder, i));

            LinearTuplePredicateCache cache    = new LinearTuplePredicateCache(proxy);
            cache.setLeft(joins.get(i-2));
            cache.applyRight(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), objects.get(i-2));

            Object[] args = new Object[i];
            String s = "Predicate" + i + ".test ctx, ";
            for ( int j = 1; j < args.length; j++ ) {
                args[j] = Character.toString(letters[j]);
                s = s + args[j];
                if (j < args.length - 1) {
                    s = s + ", ";
                }
            }

            assertThat(recorder).containsExactly(s);

            recorder.clear();
            cache.clear();
        }
    }

    @Test
    public void testExecutionCacheRight() throws Exception {
        List<TupleImpl<?>> objects = new ArrayList<>();
        List<TupleImpl<?>> joins = new ArrayList<>();

        for(int i = 1; i < 10; i++) {
            objects.add(TupleUtil.objectTuple(TupleUtil.getAlphabet()[i], i + 1, 1));
        }

        joins.add(objects.get(0));

        for(int i = 1; i < 9; i++) {
            joins.add(TupleUtil.joinTuple(joins.get(i - 1), objects.get(i), objects.size() + i));
        }

        for ( int i = 3; i < 10; i++ ) {
            Class cls = Class.forName(Predicate.class.getName() + (i));

            final List<String> recorder = new ArrayList<>();
            Predicate proxy =  (Predicate) Proxy.newProxyInstance(
                    LinearTuplePredicateCacheTest.class.getClassLoader(),
                    new Class[] { cls },
                    new DynamicInvocationHandler(recorder, i));

            LinearTuplePredicateCache cache    = new LinearTuplePredicateCache(proxy);

            cache.setRight(objects.get(i-2));
            cache.applyLeft(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), joins.get(i-2));

            Object[] args = new Object[i];
            String s = "Predicate" + i + ".test ctx, ";
            for ( int j = 1; j < args.length; j++ ) {
                args[j] = Character.toString(letters[j]);
                s = s + args[j];
                if (j < args.length - 1) {
                    s = s + ", ";
                }
            }

            assertThat(recorder).containsExactly(s);

            recorder.clear();
            cache.clear();
        }
    }

}

package org.drools.core;


import org.drools.core.Letters.B;
import org.drools.core.Letters.C;
import org.drools.core.Letters.D;
import org.drools.core.Letters.E;
import org.drools.core.Letters.F;
import org.drools.core.Letters.G;
import org.drools.core.Letters.H;
import org.drools.core.Letters.I;
import org.drools.core.Letters.J;
import org.drools.core.Letters.K;
import org.drools.core.TupleUtil.DynamicInvocationHandler;
import org.drools.core.function.BiLinearTuplePredicateCache;
import org.drools.core.function.Predicate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;


public class BiLinearTuplePredicateCacheTest {
    private char[] letters = TupleUtil.getAlphabet();

    @Test
    public void testDeepWalk(){
        //TupleImpl<B> b = TupleUtil.objectTuple(new B("b1"), 1, 1);
        TupleImpl<C> c = TupleUtil.objectTuple(new C("c1"), 2, 1, 1);
        TupleImpl<D> d = TupleUtil.objectTuple(new D("d1"), 3, 1, 1);
        TupleImpl<E> e = TupleUtil.objectTuple(new E("e1"), 4, 1, 1);
        TupleImpl<F> f = TupleUtil.objectTuple(new F("f1"), 5, 3, 1);
        TupleImpl<G> g = TupleUtil.objectTuple(new G("g1"), 6, 2, 1);
        TupleImpl<H> h = TupleUtil.objectTuple(new H("h1"), 7, 1, 1);
        TupleImpl<I> i = TupleUtil.objectTuple(new I("i1"), 8, 2, 1);
        TupleImpl<J> j = TupleUtil.objectTuple(new J("j1"), 9, 2, 1);
        TupleImpl<K> k = TupleUtil.objectTuple(new K("k1"), 10, 1, 1);

        // It's only the objectIndex and size of the last tuple that matters.
        TupleImpl<E> de = TupleUtil.joinTuple(d, e, 13, 2, 1);

        TupleImpl<H> gh = TupleUtil.joinTuple(g, h, 14, 2, 1);
        TupleImpl<H> f2 = TupleUtil.leftTuple(f,15, 2, 1);
        TupleImpl<H> fgh = TupleUtil.joinTuple(f2, gh, 16, 3, 2);

        TupleImpl<K> jk = TupleUtil.joinTuple(j, k, 17, 2, 1);
        TupleImpl<K> jk2 = TupleUtil.leftTuple(jk, 18, 2, 1);
        TupleImpl<K> ijk = TupleUtil.joinTuple(i, jk2, 19, 3, 2);

        TupleImpl<K> fghijk = TupleUtil.joinTuple(fgh, ijk, 20, 6, 3);

        // only this one matters for this test
        TupleImpl<K> defghijk = TupleUtil.joinTuple(de, fghijk, 21, 9, 8);

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(10));

        cache.setRight(defghijk);

        assertArrayEquals(new Object[] {
                null, null, d, e, f, g,  h, i, j, k
        }, cache.values());

        cache.applyLeft(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), c);

        assertArrayEquals(new Object[] {
                null, c, d, e, f, g,  h, i, j, k
        }, cache.values());
    }

    private static Predicate getPredicate(int s)  {

        Class cls = null;
        try {
            cls = Class.forName(Predicate.class.getName() + (s));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        final List<String> recorder = new ArrayList<>();
        Predicate proxy =  (Predicate) Proxy.newProxyInstance(
                BiLinearTuplePredicateCache.class.getClassLoader(),
                new Class[] { cls },
                new DynamicInvocationHandler(recorder, s));
        return proxy;
    }

    @Test
    public void testSetLeft() {
        TupleImpl<B> b = TupleUtil.objectTuple(new B("b1"), 1, 1);
        TupleImpl<C> c = TupleUtil.objectTuple(new C("c1"), 2, 1);
        TupleImpl<D> d = TupleUtil.objectTuple(new D("d1"), 3, 1);
        TupleImpl<E> e = TupleUtil.objectTuple(new E("e1"), 4, 1);

        TupleImpl<C> bc  = TupleUtil.joinTuple(b, c, 10, 2, 2);
        TupleImpl<E> de = TupleUtil.joinTuple(d, e, 11, 4, 2);

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(5));

        cache.setLeft(bc);

        assertArrayEquals(new Object[] {
                null, b, c, null, null
        }, cache.values());

        cache.applyRight(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), de);
        assertArrayEquals(new Object[] {
                null, b, c, d, e
        }, cache.values());

    }

    @Test
    public void testSetRight() {
        TupleImpl<B> b = TupleUtil.objectTuple(new B("b1"), 1, 1);
        TupleImpl<C> c = TupleUtil.objectTuple(new C("c1"), 2, 1);
        TupleImpl<D> d = TupleUtil.objectTuple(new D("d1"), 3, 1);
        TupleImpl<E> e = TupleUtil.objectTuple(new E("e1"), 4, 1);

        TupleImpl<C> bc  = TupleUtil.joinTuple(b, c, 10, 2, 2);
        TupleImpl<E> de = TupleUtil.joinTuple(d, e, 11, 4, 2);

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(5));

        cache.setRight(de);

        assertArrayEquals(new Object[] {
                null, null, null, d, e
        }, cache.values());

        cache.applyLeft(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), bc);

        assertArrayEquals(new Object[] {
                null, b, c,  d, e
        }, cache.values());

    }

    @Test
    public void testLinearExecutionCacheLeft() throws Exception {
        // The data structures are linear, but it uses the BiLinearTuplePredicateCache to test it still works
        List<TupleImpl<?>> objects = new ArrayList<>();
        List<TupleImpl<?>> joins   = new ArrayList<>();

        for(int i = 1; i < 10; i++) {
            objects.add(TupleUtil.objectTuple(TupleUtil.getAlphabet()[i], i + 1, 1, i));
        }

        joins.add(objects.get(0));

        for(int i = 1; i < 9; i++) {
            joins.add(TupleUtil.joinTuple(joins.get(i - 1), objects.get(i), objects.size() + i, i+1, 2));
        }

        for ( int i = 3; i < 10; i++ ) {
            Class cls = Class.forName(Predicate.class.getName() + (i));

            final List<String> recorder = new ArrayList<>();
            Predicate proxy =  (Predicate) Proxy.newProxyInstance(
                    LinearTuplePredicateCacheTest.class.getClassLoader(),
                    new Class[] { cls },
                    new DynamicInvocationHandler(recorder, i));

            BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(proxy);
            cache.setLeft(joins.get(i-3));
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
    public void testLinearExecutionCacheRight() throws Exception {
        // The data structures are linear, but it uses the BiLinearTuplePredicateCache to test it still works
        List<TupleImpl<?>> objects = new ArrayList<>();
        List<TupleImpl<?>> joins   = new ArrayList<>();

        for(int i = 1; i < 10; i++) {
            objects.add(TupleUtil.objectTuple(TupleUtil.getAlphabet()[i], i + 1, 1, i));
        }

        joins.add(objects.get(0));

        for(int i = 1; i < 9; i++) {
            joins.add(TupleUtil.joinTuple(joins.get(i - 1), objects.get(i), objects.size() + i, i+1, 2));
        }

        for ( int i = 3; i < 10; i++ ) {
            Class cls = Class.forName(Predicate.class.getName() + (i));

            final List<String> recorder = new ArrayList<>();
            Predicate proxy =  (Predicate) Proxy.newProxyInstance(
                    LinearTuplePredicateCacheTest.class.getClassLoader(),
                    new Class[] { cls },
                    new DynamicInvocationHandler(recorder, i));

            BiLinearTuplePredicateCache cache    = new BiLinearTuplePredicateCache(proxy);

            cache.setRight(objects.get(i-3));
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

    @Test
    public void testBiLinearExecutionCacheLeft() throws Exception {
        // This uses a right input join tuple as left input for the tip of the setLeft and an object for the applyRight
        // Note it will iterate from 3 to 8 arity, to test each part of the test() method invocation.
        List<TupleImpl<?>> objects = new ArrayList<>();

        for(int i = 1; i < 10; i++) {
            objects.add(TupleUtil.objectTuple(TupleUtil.getAlphabet()[i], i + 1, 1, i));
        }

        List<List<TupleImpl<?>>> listOfJoins = new ArrayList<>();
        for (int j = 1; j < 7; j++) {
            List<TupleImpl<?>> joins   = new ArrayList<>();
            joins.add(objects.get(0));
            listOfJoins.add(joins);
            for (int i = 1; i <= j; i++) {
                if (i < j) {
                    joins.add(TupleUtil.joinTuple(joins.get(i - 1), objects.get(i), objects.size() + i, i + 1, 2));
                } else {
                    // this is the last one
                    TupleImpl t = TupleUtil.joinTuple(objects.get(i), objects.get(i + 1), objects.size() + i, i + 1, 2);
                    joins.add(TupleUtil.joinTuple(joins.get(i - 1), t, objects.size() + 2, i + 2, 2));
                }
            }
        }

        for ( int j = 0; j < listOfJoins.size(); j++ ) {
            List<TupleImpl<?>> joins = listOfJoins.get(j);
            for (int i = joins.size()+1; i < joins.size()+2; i++) {
                Class cls = Class.forName(Predicate.class.getName() + (i+2));

                final List<String> recorder = new ArrayList<>();
                Predicate proxy = (Predicate) Proxy.newProxyInstance(
                        LinearTuplePredicateCacheTest.class.getClassLoader(),
                        new Class[]{cls},
                        new DynamicInvocationHandler(recorder, i+2));

                BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(proxy);
                cache.setLeft(joins.get(i - 2));
                cache.applyRight(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), objects.get(i));

                Object[] args = new Object[i+2];
                String   s    = "Predicate" + (i+2) + ".test ctx, ";
                for (int k = 1; k < args.length; k++) {
                    args[k] = Character.toString(letters[k]);
                    s       = s + args[k];
                    if (k < args.length - 1) {
                        s = s + ", ";
                    }
                }

                assertThat(recorder).containsExactly(s);

                recorder.clear();
                cache.clear();
            }
        }
    }

    @Test
    public void testBiLinearExecutionCacheRight() throws Exception {
        // This uses lienar tuples for the setLeft, and a join tuple as the applyRight
        // Note it will iterate from 3 to 8 arity, to test each part of the test() method invocation.
        List<TupleImpl<?>> objects = new ArrayList<>();

        for(int i = 1; i < 10; i++) {
            objects.add(TupleUtil.objectTuple(TupleUtil.getAlphabet()[i], i + 1, 1, i));
        }

        List<List<TupleImpl<?>>> listOfJoins = new ArrayList<>();
        for (int j = 1; j < 8; j++) {
            List<TupleImpl<?>> joins   = new ArrayList<>();
            joins.add(objects.get(0));
            listOfJoins.add(joins);
            for (int i = 1; i <= j; i++) {
                if (i < j) {
                    joins.add(TupleUtil.joinTuple(joins.get(i - 1), objects.get(i), objects.size() + i, i + 1, 2));
                } else {
                    // this is the last one
                    TupleImpl t = TupleUtil.joinTuple(objects.get(i), objects.get(i + 1), objects.size() + i, i + 1, 2);
                    joins.add(TupleUtil.joinTuple(joins.get(i - 1), t, objects.size() + 2, i + 2, 2));
                }
            }
        }

        for ( int j = 0; j < listOfJoins.size(); j++ ) {
            List<TupleImpl<?>> joins = listOfJoins.get(j);
            for (int i = joins.size()+1; i < joins.size()+2; i++) {
                Class cls = Class.forName(Predicate.class.getName() + (i+1));

                final List<String> recorder = new ArrayList<>();
                Predicate proxy = (Predicate) Proxy.newProxyInstance(
                        LinearTuplePredicateCacheTest.class.getClassLoader(),
                        new Class[]{cls},
                        new DynamicInvocationHandler(recorder, i+1));

                BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(proxy);
                cache.setRight(joins.get(i - 2));
                cache.applyLeft(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), joins.get(i - 3));

                Object[] args = new Object[i+1];
                String   s    = "Predicate" + (i+1) + ".test ctx, ";
                for (int k = 1; k < args.length; k++) {
                    args[k] = Character.toString(letters[k]);
                    s       = s + args[k];
                    if (k < args.length - 1) {
                        s = s + ", ";
                    }
                }

                assertThat(recorder).containsExactly(s);

                recorder.clear();
                cache.clear();
            }
        }
    }

}

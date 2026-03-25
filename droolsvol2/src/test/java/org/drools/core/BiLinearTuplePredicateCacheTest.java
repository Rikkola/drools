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
import static org.drools.core.TupleUtil.*;


public class BiLinearTuplePredicateCacheTest {
    private char[] letters = TupleUtil.getAlphabet();

    @Test
    public void testDeepWalk(){
        C c1 = new C("c1");
        D d1 = new D("d1"); E e1 = new E("e1");
        F f1 = new F("f1"); G g1 = new G("g1"); H h1 = new H("h1");
        I i1 = new I("i1"); J j1 = new J("j1"); K k1 = new K("k1");

        // c is a separate left input at objectIndex 1
        resetIndex();
        TupleImpl cTuple = leaf(c1);

        // d-k form the right tree, objectIndex continues from 2
        TupleImpl de = join(leaf(d1), leaf(e1));
        TupleImpl fgh = join(leaf(f1), join(leaf(g1), leaf(h1)));
        TupleImpl ijk = join(leaf(i1), join(leaf(j1), leaf(k1)));
        TupleImpl defghijk = join(de, join(fgh, ijk));
        System.out.println("testDeepWalk - right tree:\n" + TupleTreePrinter.print(defghijk));

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(10));

        cache.setRight(defghijk);

        TupleImpl[] values = cache.values();
        assertThat(values[2].get()).isSameAs(d1);
        assertThat(values[3].get()).isSameAs(e1);
        assertThat(values[4].get()).isSameAs(f1);
        assertThat(values[5].get()).isSameAs(g1);
        assertThat(values[6].get()).isSameAs(h1);
        assertThat(values[7].get()).isSameAs(i1);
        assertThat(values[8].get()).isSameAs(j1);
        assertThat(values[9].get()).isSameAs(k1);

        cache.applyLeft(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), cTuple);

        values = cache.values();
        assertThat(values[1].get()).isSameAs(c1);
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
        B b1 = new B("b1"); C c1 = new C("c1");
        D d1 = new D("d1"); E e1 = new E("e1");

        resetIndex();
        TupleImpl bc = join(leaf(b1), leaf(c1));
        TupleImpl de = join(leaf(d1), leaf(e1));
        System.out.println("testSetLeft - left: bc, right: de\n" + TupleTreePrinter.print(bc) + TupleTreePrinter.print(de));

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(5));

        cache.setLeft(bc);

        TupleImpl[] values = cache.values();
        assertThat(values[1].get()).isSameAs(b1);
        assertThat(values[2].get()).isSameAs(c1);
        assertThat(values[3]).isNull();
        assertThat(values[4]).isNull();

        cache.applyRight(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), de);

        values = cache.values();
        assertThat(values[1].get()).isSameAs(b1);
        assertThat(values[2].get()).isSameAs(c1);
        assertThat(values[3].get()).isSameAs(d1);
        assertThat(values[4].get()).isSameAs(e1);
    }

    @Test
    public void testSetRight() {
        B b1 = new B("b1"); C c1 = new C("c1");
        D d1 = new D("d1"); E e1 = new E("e1");

        resetIndex();
        TupleImpl bc = join(leaf(b1), leaf(c1));
        TupleImpl de = join(leaf(d1), leaf(e1));
        System.out.println("testSetRight - left: bc, right: de\n" + TupleTreePrinter.print(bc) + TupleTreePrinter.print(de));

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(5));

        cache.setRight(de);

        TupleImpl[] values = cache.values();
        assertThat(values[1]).isNull();
        assertThat(values[2]).isNull();
        assertThat(values[3].get()).isSameAs(d1);
        assertThat(values[4].get()).isSameAs(e1);

        cache.applyLeft(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), bc);

        values = cache.values();
        assertThat(values[1].get()).isSameAs(b1);
        assertThat(values[2].get()).isSameAs(c1);
        assertThat(values[3].get()).isSameAs(d1);
        assertThat(values[4].get()).isSameAs(e1);
    }

    @Test
    public void testLinearExecutionCacheLeft() throws Exception {
        Object[] allObjects = new Object[9];
        for (int i = 0; i < 9; i++) {
            allObjects[i] = TupleUtil.getAlphabet()[i + 1];
        }

        for (int arity = 3; arity < 10; arity++) {
            resetIndex();
            // Build left: linear chain of (arity-2) objects
            TupleImpl leftChain = leaf(allObjects[0]);
            for (int i = 1; i < arity - 2; i++) {
                leftChain = join(leftChain, leaf(allObjects[i]));
            }
            // Right: single object
            TupleImpl rightObj = leaf(allObjects[arity - 2]);

            Class cls = Class.forName(Predicate.class.getName() + arity);
            final List<String> recorder = new ArrayList<>();
            Predicate proxy = (Predicate) Proxy.newProxyInstance(
                    LinearTuplePredicateCacheTest.class.getClassLoader(),
                    new Class[]{cls},
                    new DynamicInvocationHandler(recorder, arity));

            BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(proxy);
            System.out.println("testLinearExecutionCacheLeft - arity=" + arity + " left:\n" + TupleTreePrinter.print(leftChain) + "right:\n" + TupleTreePrinter.print(rightObj));
            cache.setLeft(leftChain);
            cache.applyRight(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), rightObj);

            String s = "Predicate" + arity + ".test ctx, ";
            for (int j = 1; j < arity; j++) {
                s = s + Character.toString(letters[j]);
                if (j < arity - 1) s = s + ", ";
            }

            assertThat(recorder).containsExactly(s);
            recorder.clear();
            cache.clear();
        }
    }

    @Test
    public void testLinearExecutionCacheRight() throws Exception {
        Object[] allObjects = new Object[9];
        for (int i = 0; i < 9; i++) {
            allObjects[i] = TupleUtil.getAlphabet()[i + 1];
        }

        for (int arity = 3; arity < 10; arity++) {
            resetIndex();
            // Right: single object
            TupleImpl rightObj = leaf(allObjects[arity - 3]);
            // Left: linear chain of (arity-1) objects
            resetIndex();
            TupleImpl leftChain = leaf(allObjects[0]);
            for (int i = 1; i < arity - 1; i++) {
                leftChain = join(leftChain, leaf(allObjects[i]));
            }

            Class cls = Class.forName(Predicate.class.getName() + arity);
            final List<String> recorder = new ArrayList<>();
            Predicate proxy = (Predicate) Proxy.newProxyInstance(
                    LinearTuplePredicateCacheTest.class.getClassLoader(),
                    new Class[]{cls},
                    new DynamicInvocationHandler(recorder, arity));

            BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(proxy);
            System.out.println("testLinearExecutionCacheRight - arity=" + arity + " left:\n" + TupleTreePrinter.print(leftChain) + "right:\n" + TupleTreePrinter.print(rightObj));
            cache.setRight(rightObj);
            cache.applyLeft(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), leftChain);

            String s = "Predicate" + arity + ".test ctx, ";
            for (int j = 1; j < arity; j++) {
                s = s + Character.toString(letters[j]);
                if (j < arity - 1) s = s + ", ";
            }

            assertThat(recorder).containsExactly(s);
            recorder.clear();
            cache.clear();
        }
    }

    @Test
    public void testBiLinearExecutionCacheLeft() throws Exception {
        Object[] allObjects = new Object[9];
        for (int i = 0; i < 9; i++) {
            allObjects[i] = TupleUtil.getAlphabet()[i + 1];
        }

        for (int j = 1; j < 7; j++) {
            resetIndex();
            // Build linear chain, with last step being a bi-linear join (pair on right)
            TupleImpl chain = leaf(allObjects[0]);
            for (int i = 1; i < j; i++) {
                chain = join(chain, leaf(allObjects[i]));
            }
            TupleImpl lastPair = join(leaf(allObjects[j]), leaf(allObjects[j + 1]));
            TupleImpl leftTuple = join(chain, lastPair);

            // Right: single object
            TupleImpl rightObj = leaf(allObjects[j + 2]);

            int arity = j + 4;
            Class cls = Class.forName(Predicate.class.getName() + arity);
            final List<String> recorder = new ArrayList<>();
            Predicate proxy = (Predicate) Proxy.newProxyInstance(
                    LinearTuplePredicateCacheTest.class.getClassLoader(),
                    new Class[]{cls},
                    new DynamicInvocationHandler(recorder, arity));

            BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(proxy);
            System.out.println("testBiLinearExecutionCacheLeft - arity=" + arity + " left:\n" + TupleTreePrinter.print(leftTuple) + "right:\n" + TupleTreePrinter.print(rightObj));
            cache.setLeft(leftTuple);
            cache.applyRight(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), rightObj);

            String s = "Predicate" + arity + ".test ctx, ";
            for (int k = 1; k < arity; k++) {
                s = s + Character.toString(letters[k]);
                if (k < arity - 1) s = s + ", ";
            }

            assertThat(recorder).containsExactly(s);
            recorder.clear();
            cache.clear();
        }
    }

    @Test
    public void testBiLinearExecutionCacheRight() throws Exception {
        Object[] allObjects = new Object[9];
        for (int i = 0; i < 9; i++) {
            allObjects[i] = TupleUtil.getAlphabet()[i + 1];
        }

        for (int j = 1; j < 8; j++) {
            // Left: linear chain of j objects
            resetIndex();
            TupleImpl leftChain = leaf(allObjects[0]);
            for (int i = 1; i < j; i++) {
                leftChain = join(leftChain, leaf(allObjects[i]));
            }

            // Right: linear chain with a pair at the end
            resetIndex();
            TupleImpl rightChain = leaf(allObjects[0]);
            for (int i = 1; i < j; i++) {
                rightChain = join(rightChain, leaf(allObjects[i]));
            }
            TupleImpl lastPair = join(leaf(allObjects[j]), leaf(allObjects[j + 1]));
            TupleImpl rightTuple = join(rightChain, lastPair);

            int arity = j + 3;
            Class cls = Class.forName(Predicate.class.getName() + arity);
            final List<String> recorder = new ArrayList<>();
            Predicate proxy = (Predicate) Proxy.newProxyInstance(
                    LinearTuplePredicateCacheTest.class.getClassLoader(),
                    new Class[]{cls},
                    new DynamicInvocationHandler(recorder, arity));

            BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(proxy);
            System.out.println("testBiLinearExecutionCacheRight - arity=" + arity + " left:\n" + TupleTreePrinter.print(leftChain) + "right:\n" + TupleTreePrinter.print(rightTuple));
            cache.setRight(rightTuple);
            cache.applyLeft(new ContextPojoDS(new PropagatingDataStore<>(0, new TypeIndexer())), leftChain);

            String s = "Predicate" + arity + ".test ctx, ";
            for (int k = 1; k < arity; k++) {
                s = s + Character.toString(letters[k]);
                if (k < arity - 1) s = s + ", ";
            }

            assertThat(recorder).containsExactly(s);
            recorder.clear();
            cache.clear();
        }
    }

    @Test
    public void testSymmetricTree() {
        B b1 = new B("b1"); C c1 = new C("c1");
        D d1 = new D("d1"); E e1 = new E("e1");

        resetIndex();
        TupleImpl abcd = join(join(leaf(b1), leaf(c1)), join(leaf(d1), leaf(e1)));
        System.out.println("testSymmetricTree:\n" + TupleTreePrinter.print(abcd));

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(5));
        cache.setRight(abcd);

        TupleImpl[] values = cache.values();
        assertThat(values[1].get()).isSameAs(b1);
        assertThat(values[2].get()).isSameAs(c1);
        assertThat(values[3].get()).isSameAs(d1);
        assertThat(values[4].get()).isSameAs(e1);
    }

    @Test
    public void testRightHeavyTree() {
        B b1 = new B("b1"); C c1 = new C("c1");
        D d1 = new D("d1"); E e1 = new E("e1");

        resetIndex();
        TupleImpl tree = join(leaf(b1), join(leaf(c1), join(leaf(d1), leaf(e1))));
        System.out.println("testRightHeavyTree:\n" + TupleTreePrinter.print(tree));

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(5));
        cache.setRight(tree);

        TupleImpl[] values = cache.values();
        assertThat(values[1].get()).isSameAs(b1);
        assertThat(values[2].get()).isSameAs(c1);
        assertThat(values[3].get()).isSameAs(d1);
        assertThat(values[4].get()).isSameAs(e1);
    }

    @Test
    public void testAsymmetricDeep() {
        B b1 = new B("b1"); C c1 = new C("c1");
        D d1 = new D("d1"); E e1 = new E("e1");
        F f1 = new F("f1");

        resetIndex();
        TupleImpl tree = join(join(join(leaf(b1), leaf(c1)), leaf(d1)), join(leaf(e1), leaf(f1)));
        System.out.println("testAsymmetricDeep:\n" + TupleTreePrinter.print(tree));

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(6));
        cache.setRight(tree);

        TupleImpl[] values = cache.values();
        assertThat(values[1].get()).isSameAs(b1);
        assertThat(values[2].get()).isSameAs(c1);
        assertThat(values[3].get()).isSameAs(d1);
        assertThat(values[4].get()).isSameAs(e1);
        assertThat(values[5].get()).isSameAs(f1);
    }

    // ==================== Table-driven walkTreeAndAssign tests ====================

    @Test
    public void testTableSymmetricTree() {
        B b1 = new B("b1"); C c1 = new C("c1");
        D d1 = new D("d1"); E e1 = new E("e1");

        resetIndex();
        TupleImpl tree = join(join(leaf(b1), leaf(c1)), join(leaf(d1), leaf(e1)));

        int[] table = BiLinearTuplePredicateCache.buildTable(tree);
        System.out.println("testTableSymmetricTree table: " + java.util.Arrays.toString(table));
        assertArrayEquals(new int[]{2, 1, 0, 2, 1, 1, 0}, table);

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(5));
        cache.walkTreeAndAssign(tree, table);

        TupleImpl[] values = cache.values();
        assertThat(values[1].get()).isSameAs(b1);
        assertThat(values[2].get()).isSameAs(c1);
        assertThat(values[3].get()).isSameAs(d1);
        assertThat(values[4].get()).isSameAs(e1);
    }

    @Test
    public void testTableRightHeavyTree() {
        B b1 = new B("b1"); C c1 = new C("c1");
        D d1 = new D("d1"); E e1 = new E("e1");

        resetIndex();
        TupleImpl tree = join(leaf(b1), join(leaf(c1), join(leaf(d1), leaf(e1))));

        int[] table = BiLinearTuplePredicateCache.buildTable(tree);
        System.out.println("testTableRightHeavyTree table: " + java.util.Arrays.toString(table));
        assertArrayEquals(new int[]{3, 1, 0, 2, 0, 2, 0}, table);

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(5));
        cache.walkTreeAndAssign(tree, table);

        TupleImpl[] values = cache.values();
        assertThat(values[1].get()).isSameAs(b1);
        assertThat(values[2].get()).isSameAs(c1);
        assertThat(values[3].get()).isSameAs(d1);
        assertThat(values[4].get()).isSameAs(e1);
    }

    @Test
    public void testTableAsymmetricDeep() {
        B b1 = new B("b1"); C c1 = new C("c1");
        D d1 = new D("d1"); E e1 = new E("e1");
        F f1 = new F("f1");

        resetIndex();
        TupleImpl tree = join(join(join(leaf(b1), leaf(c1)), leaf(d1)), join(leaf(e1), leaf(f1)));

        int[] table = BiLinearTuplePredicateCache.buildTable(tree);
        System.out.println("testTableAsymmetricDeep table: " + java.util.Arrays.toString(table));

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(6));
        cache.walkTreeAndAssign(tree, table);

        TupleImpl[] values = cache.values();
        assertThat(values[1].get()).isSameAs(b1);
        assertThat(values[2].get()).isSameAs(c1);
        assertThat(values[3].get()).isSameAs(d1);
        assertThat(values[4].get()).isSameAs(e1);
        assertThat(values[5].get()).isSameAs(f1);
    }

    @Test
    public void testTableDeepWalk() {
        C c1 = new C("c1");
        D d1 = new D("d1"); E e1 = new E("e1");
        F f1 = new F("f1"); G g1 = new G("g1"); H h1 = new H("h1");
        I i1 = new I("i1"); J j1 = new J("j1"); K k1 = new K("k1");

        resetIndex();
        TupleImpl cTuple = leaf(c1);

        TupleImpl de = join(leaf(d1), leaf(e1));
        TupleImpl fgh = join(leaf(f1), join(leaf(g1), leaf(h1)));
        TupleImpl ijk = join(leaf(i1), join(leaf(j1), leaf(k1)));
        TupleImpl defghijk = join(de, join(fgh, ijk));

        int[] table = BiLinearTuplePredicateCache.buildTable(defghijk);
        System.out.println("testTableDeepWalk table: " + java.util.Arrays.toString(table));

        BiLinearTuplePredicateCache cache = new BiLinearTuplePredicateCache(getPredicate(10));
        cache.walkTreeAndAssign(defghijk, table);

        TupleImpl[] values = cache.values();
        assertThat(values[2].get()).isSameAs(d1);
        assertThat(values[3].get()).isSameAs(e1);
        assertThat(values[4].get()).isSameAs(f1);
        assertThat(values[5].get()).isSameAs(g1);
        assertThat(values[6].get()).isSameAs(h1);
        assertThat(values[7].get()).isSameAs(i1);
        assertThat(values[8].get()).isSameAs(j1);
        assertThat(values[9].get()).isSameAs(k1);
    }

}

package org.drools.core;

import org.drools.base.base.ClassObjectType;
import org.drools.base.rule.Declaration;
import org.drools.base.rule.Pattern;
import org.drools.core.TupleUtil.DummyNode;
import org.drools.core.function.CompactLinearTuplePredicateCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class CompactLinearTuplePredicateCacheTest {

    @Test
    public void testOneAtStart() {
        DummyNode                        node  = new DummyNode(10, 8, 8, 1, 1);
        Pattern                          p1    = new Pattern(0, 1, 1, new ClassObjectType(String.class), "var1");
        CompactLinearTuplePredicateCache cache = new CompactLinearTuplePredicateCache(node, new Declaration[]{p1.getDeclaration()});

        assertArrayEquals(new int[]{
                7
        }, cache.getSkipNext());
    }

    @Test
    public void testOneAtEnd() {
        DummyNode                   node     = new DummyNode(10, 8, 8, 1, 1);
        Pattern                          p1    = new Pattern(0, 8, 8, new ClassObjectType(String.class), "var1");
        CompactLinearTuplePredicateCache cache = new CompactLinearTuplePredicateCache(node, new Declaration[]{p1.getDeclaration()});

        assertArrayEquals(new int[]{
                0
        }, cache.getSkipNext());
    }

    @Test
    public void testOneAtMiddle() {
        DummyNode                   node     = new DummyNode(10, 8, 8, 1, 1);
        Pattern                          p1    = new Pattern(0, 4, 4, new ClassObjectType(String.class), "var1");
        CompactLinearTuplePredicateCache cache = new CompactLinearTuplePredicateCache(node, new Declaration[]{p1.getDeclaration()});

        assertArrayEquals(new int[]{
                4
        }, cache.getSkipNext());
    }

    @Test
    public void testOneStartOneAnotherAtEnd() {
        DummyNode                   node     = new DummyNode(10, 8, 8, 1, 1);
        Pattern                     p1       = new Pattern(0, 1, 1, new ClassObjectType(String.class), "var1");
        Pattern                          p2    = new Pattern(0, 8, 8, new ClassObjectType(String.class), "var2");
        CompactLinearTuplePredicateCache cache = new CompactLinearTuplePredicateCache(node, new Declaration[]{p1.getDeclaration(), p2.getDeclaration()});

        assertArrayEquals(new int[]{
                0, 6
        }, cache.getSkipNext());
    }

    @Test
    public void testOneStartOneMiddleOneAtEnd() {
        DummyNode                   node     = new DummyNode(10, 8, 8, 1, 1);
        Pattern                     p1       = new Pattern(0, 1, 1, new ClassObjectType(String.class), "var1");
        Pattern                     p2       = new Pattern(0, 4, 4, new ClassObjectType(String.class), "var2");
        Pattern                          p3    = new Pattern(0, 8, 8, new ClassObjectType(String.class), "var3");
        CompactLinearTuplePredicateCache cache = new CompactLinearTuplePredicateCache(node, new Declaration[]{p1.getDeclaration(), p2.getDeclaration(), p3.getDeclaration()});

        assertArrayEquals(new int[]{
                0, 3, 2
        }, cache.getSkipNext());
    }

    @Test
    public void testTwoMiddle() {
        DummyNode                   node     = new DummyNode(10, 8, 8, 1, 1);
        Pattern                     p1       = new Pattern(0, 3, 3, new ClassObjectType(String.class), "var1");
        Pattern                          p2    = new Pattern(0, 6, 6, new ClassObjectType(String.class), "var2");
        CompactLinearTuplePredicateCache cache = new CompactLinearTuplePredicateCache(node, new Declaration[]{p1.getDeclaration(), p2.getDeclaration()});

        assertArrayEquals(new int[]{
                2, 2
        }, cache.getSkipNext());
    }
}


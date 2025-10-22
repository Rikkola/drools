package org.drools.core;

import org.drools.core.function.Function1;
import org.drools.core.function.Predicate1;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GraphTest {

    public static class A {
        private List<B> b;
    }

    public static class B {
        private List<C> c;
    }

    public static class C {
        private List<D> d;
    }

    public static class D {
    }

    @Test
    public void test1() {

    }

}

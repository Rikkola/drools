package org.drools.core;

import org.drools.core.function.Function1;
import org.drools.core.function.Predicate1;

public class DataBuilder {

    public static TypeFilterBuilder<Object> store(String id) {
        return new TypeFilterBuilder<Object>(id, Object.class);
    }

    public static <A> TypeFilterBuilder<A> store(String id, Class<A> cls) {
        return new TypeFilterBuilder(id, cls);
    }

    public static void stream(String persons) {

    }

    public static class TypeFilterBuilder<A> {

        public TypeFilterBuilder(String id, Class cls) {}

        public <A> LiteralFilterBuilder<A> type(Class... objectClass) {
            return new LiteralFilterBuilder();
        }
    }

    public static class LiteralFilterBuilder<A> {

        public LiteralFilterBuilder<A> filter(Predicate1<A> predicate) {
            return this;
        }

        public <R> LiteralFilterBuilder2<A> index(Function1<A, R> f) {
            return new LiteralFilterBuilder2<>(this);
        }

        public <R> LiteralFilterBuilder2<A> index(String name, Function1<A, R> f) {
            return new LiteralFilterBuilder2<>(this);
        }

    }

    public static class LiteralFilterBuilder2<A> {
        private LiteralFilterBuilder<A> returnObject;

        public LiteralFilterBuilder2(LiteralFilterBuilder<A> returnObject) {
            this.returnObject = returnObject;
        }

        public LiteralFilterBuilder<A> filter(Predicate1<A> predicate) {
            return returnObject;
        }
    }
}

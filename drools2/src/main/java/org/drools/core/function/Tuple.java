package org.drools.core.function;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class Tuple {
    protected int size;

    // Does not need to be concurrent, as the value is always the same for the key, it'll eventually be consistent.
    private static Map<String, Constructor<?>> constructors = new ConcurrentHashMap<>();

    public abstract <T> T get(int index);

    public abstract <T> void set(int index, T t);

    private <T> Constructor<T> getConstructor(Class<T> cls) {
        Constructor<?> con =  constructors.computeIfAbsent(cls.getName(), (k) -> {
            Constructor<?>[]  cons = cls.getDeclaredConstructors();
            for ( Constructor<?> i : cons) {
                if (i.getParameterCount() == size) {
                    return i;
                }
            }
            throw new IllegalStateException("Unable to resolve constructor for class" + cls.getCanonicalName());
        });

        return (Constructor<T>) con;
    }

    public <T> T as(T... v) {
        Class  cls  = v.getClass().getComponentType();
        Constructor<T> con = getConstructor(cls);
        try {
            switch (size) {
                case 1: {
                    Tuple1<?>   t   = (Tuple1<?>) this;
                    return con.newInstance(t.a);
                }
                case 2: {
                    Tuple2<?, ?>   t   = (Tuple2<?, ?>) this;
                    return con.newInstance(t.a, t.b);
                }
                case 3: {
                    Tuple3<?, ?, ?> t   = (Tuple3<?, ?, ?>) this;
                    return con.newInstance(t.a, t.b, t.c);
                }
                case 4: {
                    Tuple4<?, ?, ?, ?> t   = (Tuple4<?, ?, ?, ?>) this;
                    return con.newInstance(t.a, t.b, t.c, t.d);
                }
                case 5: {
                    Tuple5<?, ?, ?, ?, ?> t   = (Tuple5<?, ?, ?, ?, ?>) this;
                    return con.newInstance(t.a, t.b, t.c, t.d, t.e);
                }
            }
        } catch (InvocationTargetException|InstantiationException|IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        throw new IllegalStateException("Unable to instantiate target class");
    }

    public int size() {
        return size;
    }

    public static class Tuple1<A> extends Tuple {
        protected A a;

        public Tuple1() {
            super();
        }

        public Tuple1(A a) {
            this.a = a;
            this.size = 1;


        }

        public A getA() {
            return a;
        }

        public void setA(A a) {
            this.a = a;
        }

        @Override
        public <T> T get(int index) {
            switch (index) {
                case 0: {
                    return (T) a;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }

        public <T> void set(int index, T t) {
            switch (index) {
                case 0: {
                    this.a = (A) t;
                    break;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }
    }



    public static class Tuple2<A, B> extends Tuple1<A> {
        protected B b;

        public Tuple2() {
            super();
        }

        public Tuple2(A a, B b) {
            super(a);
            this.b = b;
            this.size = 2;
        }

        public B getB() {
            return b;
        }

        public void setB(B b) {
            this.b = b;
        }

        @Override
        public <T> T get(int index) {
            switch (index) {
                case 0: {
                    return (T) a;
                }
                case 1: {
                    return (T) b;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public <T> void set(int index, T t) {
            switch (index) {
                case 0: {
                    a = (A) t;
                    break;
                }
                case 1: {
                    b = (B) t;
                    break;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }
    }

    public static class Tuple3<A, B, C> extends Tuple2<A, B> {
        protected C c;

        public Tuple3() {
            super();
        }

        public Tuple3(A a, B b, C c) {
            super(a, b);
            this.c = c;
            this.size = 3;
        }

        public C getC() {
            return c;
        }

        public void setC(C c) {
            this.c = c;
        }

        @Override
        public <T> T get(int index) {
            switch (index) {
                case 0: {
                    return (T) a;
                }
                case 1: {
                    return (T) b;
                }
                case 2: {
                    return (T) c;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public <T> void set(int index, T t) {
            switch (index) {
                case 0: {
                    a = (A) t;
                    break;
                }
                case 1: {
                    b = (B) t;
                    break;
                }
                case 2: {
                    c = (C) t;
                    break;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }
    }

    public static class Tuple4<A, B, C, D> extends Tuple3<A, B, C> {
        protected D d;

        public Tuple4() {
            super();
        }
        public Tuple4(A a, B b, C c, D d) {
            super(a, b, c);
            this.d = d;
            this.size = 4;
        }

        public D getD() {
            return d;
        }

        public void setD(D d) {
            this.d = d;
        }

        @Override
        public <T> T get(int index) {
            switch (index) {
                case 0: {
                    return (T) a;
                }
                case 1: {
                    return (T) b;
                }
                case 2: {
                    return (T) c;
                }
                case 3: {
                    return (T) d;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public <T> void set(int index, T t) {
            switch (index) {
                case 0: {
                    a = (A) t;
                    break;
                }
                case 1: {
                    b = (B) t;
                    break;
                }
                case 2: {
                    c = (C) t;
                    break;
                }
                case 3: {
                    d = (D) t;
                    break;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }
    }

    public static class Tuple5<A, B, C, D, E> extends Tuple4<A, B, C, D>{
        protected E e;

        public Tuple5() {
            super();
        }

        public Tuple5(A a, B b, C c, D d, E e) {
            super(a, b, c, d);
            this.e = e;
            this.size = 5;
        }

        public E getE() {
            return e;
        }

        public void setE(E e) {
            this.e = e;
        }

        @Override
        public <T> T get(int index) {
            switch (index) {
                case 0: {
                    return (T) a;
                }
                case 1: {
                    return (T) b;
                }
                case 2: {
                    return (T) c;
                }
                case 3: {
                    return (T) d;
                }
                case 4: {
                    return (T) e;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public <T> void set(int index, T t) {
            switch (index) {
                case 0: {
                    a = (A) t;
                    break;
                }
                case 1: {
                    b= (B) t;
                    break;
                }
                case 2: {
                    c = (C) t;
                    break;
                }
                case 3: {
                    d = (D) t;
                    break;
                }
                case 4: {
                    e = (E) t;
                    break;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public String toString() {
            return "Tuple5{" +
                   getA() +
                   ", " + getB() +
                   ", " + getC() +
                   ", " + getD() +
                   ", " + e +
                   '}';
        }
    }


    public static class Tuple6<A, B, C, D, E, F> extends Tuple5<A, B, C, D, E>{
        protected F f;

        public Tuple6() {
            super();
        }

        public Tuple6(A a, B b, C c, D d, E e, F f) {
            super(a, b, c, d, e);
            this.f = f;
            this.size = 6;
        }

        public F getF() {
            return f;
        }

        public void setF(F f) {
            this.f = f;
        }

        @Override
        public <T> T get(int index) {
            switch (index) {
                case 0: {
                    return (T) a;
                }
                case 1: {
                    return (T) b;
                }
                case 2: {
                    return (T) c;
                }
                case 3: {
                    return (T) d;
                }
                case 4: {
                    return (T) e;
                }
                case 5: {
                    return (T) f;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public <T> void set(int index, T t) {
            switch (index) {
                case 0: {
                    a = (A) t;
                    break;
                }
                case 1: {
                    b= (B) t;
                    break;
                }
                case 2: {
                    c = (C) t;
                    break;
                }
                case 3: {
                    d = (D) t;
                    break;
                }
                case 4: {
                    e = (E) t;
                    break;
                }
                case 5: {
                    f = (F) t;
                    break;
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public String toString() {
            return "Tuple6{" +
                   getA() +
                   ", " + getB() +
                   ", " + getC() +
                   ", " + getD() +
                   ", " + getE() +
                   ", " + getF() +
                   '}';
        }
    }

}

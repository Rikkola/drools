package org.drools.core.function;

public abstract class Tuple {

    public abstract <T> T get(int index);

    public abstract <T> void set(int index, T t);

    public static class Tuple1<A> extends Tuple {
        private A a;

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
        private B b;

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
                    return (T) getA();
                }
                case 1: {
                    return (T) getB();
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public <T> void set(int index, T t) {
            switch (index) {
                case 0: {
                    setA((A) t);
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
        private C c;

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
                    return (T) getA();
                }
                case 1: {
                    return (T) getB();
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
                    setA((A) t);
                    break;
                }
                case 1: {
                    setB((B) t);
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
        private D d;

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
                    return (T) getA();
                }
                case 1: {
                    return (T) getB();
                }
                case 2: {
                    return (T) getC();
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
                    setA((A) t);
                    break;
                }
                case 1: {
                    setB((B) t);
                    break;
                }
                case 2: {
                    setC((C) t);
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
        private E e;

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
                    return (T) getA();
                }
                case 1: {
                    return (T) getB();
                }
                case 2: {
                    return (T) getC();
                }
                case 3: {
                    return (T) getD();
                }
                case 4: {
                    return (T) getE();
                }
                default :
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public <T> void set(int index, T t) {
            switch (index) {
                case 0: {
                    setA((A) t);
                    break;
                }
                case 1: {
                    setB((B) t);
                    break;
                }
                case 2: {
                    setC((C) t);
                    break;
                }
                case 3: {
                    setD((D) t);
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

}

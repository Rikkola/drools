package org.drools.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

public class TupleUtil {
    private static int counter;

    public static <T> TupleImpl<T> objectTuple(T o, int id, int walkBack) {
        return objectTuple(o, id, walkBack, 0);
    }

    public static <T> TupleImpl<T> objectTuple(T o, int id, int walkBack, int objectIndex) {
        return new ObjectHandleTuple(new ObjectHandleImpl<>(counter++, o, node(0, 1)), node(id, 1, objectIndex, 1, walkBack));
    }

    public static <T1, T2> TupleImpl<T2> joinTuple(TupleImpl<T1> leftTuple, TupleImpl<T2> rightTuple, int id) {
        return new JoinTuple<>(leftTuple, rightTuple, node(id, leftTuple.getNetworkNode().getPathIndex() + 1));
    }

    public static <T1, T2> TupleImpl<T2> joinTuple(TupleImpl<T1> leftTuple, TupleImpl<T2> rightTuple, int id, int objectIndex, int size) {
        return new JoinTuple<>(leftTuple, rightTuple, node(id, leftTuple.getNetworkNode().getPathIndex() + 1, objectIndex, size));
    }

    public static <T1, T2> TupleImpl<T2> leftTuple(TupleImpl<T1> leftTuple, int id, int objectIndex, int size) {
        return new LeftTuple<>(leftTuple, node(id, leftTuple.getNetworkNode().getPathIndex() + 1, objectIndex, size));
    }

    public static DummyNode node(int id, int pathIndex) {
        return node(id, pathIndex, pathIndex, 1, 0);
    }

    public static DummyNode node(int id, int pathIndex, int objectIndex, int size) {
        return node(id, pathIndex, objectIndex, size, 0);
    }

    public static DummyNode node(int id, int pathIndex, int objectIndex, int size, int walkBack) {
        return new DummyNode(id, pathIndex, objectIndex, size, walkBack);
    }

    static char[] getAlphabet() {
        char firstLetter = 'A';
        char lastLetter = 'Z';
        int alphabetSize = lastLetter - firstLetter + 1;

        char[] alphabet = new char[alphabetSize];

        for (int index = 0; index < alphabetSize; index++) {
            alphabet[index] = (char) (index + firstLetter);
        }

        return alphabet;
    }

    public static class DummyNode extends BaseNode implements NetworkNode {
        public DummyNode(int id, int pathIndex, int objectIndex, int size, int walkBack) {
            super(id, pathIndex, objectIndex, size, walkBack);
        }
    }

    public static class DynamicInvocationHandler implements InvocationHandler {
        private List<String> recorder;
        private int          arity;

        public DynamicInvocationHandler(List<String> recorder, int arity) {
            this.recorder = recorder;
            this.arity = arity;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {

            if ( method.getName().equals("getArity") ) {
                return arity;
            }

            if ( method.getName().equals("test") ) {
                recorder.add("Predicate" + arity + "." + method.getName() + " " + predicateParameters(args));

                return true;
            }

            if ( method.getName().equals("toString") ) {
                return "Predicate" + arity + "." + method.getName();
            }

            throw new UnsupportedOperationException(method.getName());
        }

    }

    private static String predicateParameters(Object[] args) {
        String s = "ctx, "; // hardcode the ctx
        for (int i = 1; i < args.length; i++) { // use 1 to skip the ctx
            s = s + args[i];
            if (i < args.length - 1) {
                s = s + ", ";
            }
        }
        return s;
    }
}

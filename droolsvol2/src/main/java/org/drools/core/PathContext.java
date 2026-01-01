package org.drools.core;

import org.drools.core.function.BaseTuple;
import org.drools.core.function.BaseTuple.Tuple2;
import org.drools.core.function.BaseTuple.Tuple3;
import org.drools.core.function.BaseTuple.Tuple4;
import org.drools.core.function.BaseTuple.Tuple5;

public class PathContext<T extends BaseTuple> {

    private NodeContext[] contexts;

    public int size() {
        return contexts.length;
    }

    NodeContext getContext(int index) {
        return contexts[index];
    }

    public PathContext(int size) {
        contexts = new NodeContext[size];
        BaseTuple t = null;
        switch (size) {
            case 2: {
                t = new Tuple2();
            }
            case 3: {
                t = new Tuple3();
            }
            case 4: {
                t = new Tuple4();
            }
            case 5: {
                t = new Tuple5();
            }
        }

        switch (size) {
            case 5: {
                contexts[3] = new NodeContext(t);
            }
            case 4: {
                contexts[2] = new NodeContext(t);
            }
            case 3: {
                contexts[1] = new NodeContext(t);
            }
            case 2: {
                contexts[0] = new NodeContext(t);
            }
        }

        for (int i = 0; i < size; i++) {
            contexts[i] = new NodeContext(t);
        }
    }

    public T getTuple() {
        return (T) contexts[contexts.length-1].getTuple();
    }
}

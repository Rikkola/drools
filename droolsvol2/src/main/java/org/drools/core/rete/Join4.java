package org.drools.core.rete;

import org.drools.api.data.ObjectHandle;
import org.drools.core.Context;
import org.drools.core.Memory;
import org.drools.core.TupleImpl;
import org.drools.core.TupleMemory;
import org.drools.core.function.Predicate5;
import org.drools.core.function.Predicate6;
import org.drools.core.function.Predicate7;
import org.drools.core.function.Predicate8;
import org.drools.core.util.FastIterator;

public class Join4<DS, B, C, D, E> extends NetworkNode {
    private TupleSource leftInput;

    private TupleSource rightInput;

    private TupleSink sink;

    private Predicate8<Context<DS>, B, C, D, E, Object, Object, Object> predicate8;
    private Predicate7<Context<DS>, B, C, D, E, Object, Object> predicate7;
    private Predicate6<Context<DS>, B, C, D, E, Object> predicate6;
    private Predicate5<Context<DS>, B, C, D, E> predicate5;

    private int rightSize;

    private static class Join4Memory implements Memory {
        TupleMemory leftMemory;
        TupleMemory rightMemory;

        public TupleMemory leftMemory() {
            return leftMemory;
        }

        public TupleMemory rightMemory() {
            return rightMemory;
        }
    }

    private void leftAdd(Context<DS> ctx, TupleImpl<B> b, TupleImpl<C> c, TupleImpl<D> d, TupleImpl<E> e) {
        Join4Memory             memory      = ctx.getMemory(this);
        TupleMemory             rightMemory = memory.rightMemory();
        TupleImpl               rightTp     = rightMemory.getFirst4(b, c, d, e);
        FastIterator<TupleImpl> it          = rightMemory.fastIterator();
        switch (rightSize) {
            case 1:
                while ((rightTp = it.next(rightTp)) != null) {
                    //if (predicate6.test(ctx, b, c, d, e, rightTp)) {
                    if (predicate6.test(ctx,
                                        b.get(), c.get(), d.get(), e.get(),
                                        rightTp.get())) {

                    }
                }
                break;
            case 2:
                while ((rightTp = it.next(rightTp)) != null) {
                    if (predicate7.test(ctx,
                                        b.get(), c.get(), d.get(), e.get(),
                                        rightTp.get(), rightTp.getLeftParent().get())) {

                    }
                }
            case 3:
        }
//        if (predicate5.test(ds, b, c, d, e)) {
//            //sink.leftAdd(ds, b, c, d, e);
//        }
    }

    private void join(Context<DS> ctx, ObjectHandle<B> b, ObjectHandle<C> c, ObjectHandle<D> d, ObjectHandle<E> e) {
        int joins = 0;
        switch(joins) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
        }
    }
}

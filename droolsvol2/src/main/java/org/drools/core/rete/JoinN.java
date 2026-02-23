package org.drools.core.rete;

import org.drools.core.Context;
import org.drools.core.Memory;
import org.drools.core.TupleImpl;
import org.drools.core.TupleMemory;
import org.drools.core.function.Predicate5;
import org.drools.core.function.Predicate6;
import org.drools.core.function.Predicate7;
import org.drools.core.function.Predicate8;
import org.drools.core.util.FastIterator;

public class JoinN<DS, T> extends NetworkNode {
    private TupleSource leftInput;

    private TupleSource rightInput;

    private TupleSink sink;

    private Predicate8<Context<DS>, Object, Object, Object, Object, Object, Object, Object> predicate8;
    private Predicate7<Context<DS>, Object, Object, Object, Object, Object, Object> predicate7;
    private Predicate6<Context<DS>, Object, Object, Object, Object, Object> predicate6;
    private Predicate5<Context<DS>, Object, Object, Object, Object> predicate5;

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

    private void leftAdd(Context<DS> ctx, TupleImpl<T> tp) {
        Join4Memory             memory      = ctx.getMemory(this);
        TupleMemory             rightMemory = memory.rightMemory();
        TupleImpl               rightTp     = rightMemory.getFirstN(tp);
        FastIterator<TupleImpl> it          = rightMemory.fastIterator();

        while ((rightTp = it.next(rightTp)) != null) {

        }


//        switch (rightSize) {
//            case 1:
//                while ((rightTp = it.next(rightTp)) != null) {
//                    //if (predicate6.test(ctx, b, c, d, e, rightTp)) {
//                    if (predicate6.test(ctx,
//                                        b.get(), c.get(), d.get(), e.get(),
//                                        rightTp.get())) {
//
//                    }
//                }
//                break;
//            case 2:
//                while ((rightTp = it.next(rightTp)) != null) {
//                    if (predicate7.test(ctx,
//                                        b.get(), c.get(), d.get(), e.get(),
//                                        rightTp.get(), rightTp.getLeftParent().get())) {
//
//                    }
//                }
//            case 3:
//        }
////        if (predicate5.test(ds, b, c, d, e)) {
////            //sink.leftAdd(ds, b, c, d, e);
////        }
    }

//    private void join(Context<DS> ctx, DataHandle<B> b, DataHandle<C> c, DataHandle<D> d, DataHandle<E> e) {
//        int joins = 0;
//        switch(joins) {
//            case 1:
//            case 2:
//            case 3:
//            case 4:
//            case 5:
//        }
//    }
}

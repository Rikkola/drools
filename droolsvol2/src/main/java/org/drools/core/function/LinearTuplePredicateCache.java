package org.drools.core.function;

import org.drools.core.Context;
import org.drools.core.TupleImpl;

public class LinearTuplePredicateCache {
    private TupleImpl o1;
    private TupleImpl o2;
    private TupleImpl o3;
    private TupleImpl o4;
    private TupleImpl o5;
    private TupleImpl o6;
    private TupleImpl o7;
    private TupleImpl o8;
    private TupleImpl o9;
    private TupleImpl o10;

    private Predicate predicate;

    public void setLeft(TupleImpl t, Predicate p) {
        this.predicate = p;
        switch (t.getIndex()) {
            case 8:
                o10 = t;
                t = t.getParent();
            case 7:
                o9 = t;
                t = t.getParent();
            case 6:
                o8 = t;
                t = t.getParent();
            case 5:
                o7 = t;
                t = t.getParent();
            case 4:
                o6 = t;
                t = t.getParent();
            case 3:
                o5 = t;
                t = t.getParent();
            case 2:
                o4 = t;
                t = t.getParent();
            case 1:
                o3 = t;
                t = t.getParent();
            case 0:
                o2 = t;

        }
    }

    public boolean applyRight(Context ctx, TupleImpl rt) {
        switch (predicate.getArity()) {
            case 3:
                return ((Predicate3)predicate).test(ctx, o2.get(), rt.get());
            case 4:
                return ((Predicate4)predicate).test(ctx, o2.get(), o3.get(), rt.get());
            case 5:
                return ((Predicate5)predicate).test(ctx, o2.get(), o3.get(), o4.get(), rt.get());
            case 6:
                return ((Predicate6)predicate).test(ctx, o2.get(), o2.get(), o3.get(), o4.get(), rt.get());
            case 7:
                return ((Predicate7)predicate).test(ctx, o2.get(), o2.get(), o3.get(), o4.get(), o5.get(), rt.get());
            case 8:
                return ((Predicate8)predicate).test(ctx, o2.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), rt.get());
            case 9:
                return ((Predicate9)predicate).test(ctx, o2.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o7.get(), rt.get());
            case 10:
                return ((Predicate10)predicate).test(ctx, o2.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o8.get(), o9.get(), rt.get());

        }

        throw new IllegalArgumentException("Invalid predicate arity (" + predicate.getArity() + ")");
    }

//    public void setRight(TupleImpl t, int start, Predicate p) {
//        switch (p.getArity()) {
//            case 10:
//                o10 = t;
//                t = t.getParent();
//            case 9:
//                o9 = t;
//                t = t.getParent();
//            case 8:
//                o8 = t;
//                t = t.getParent();
//            case 7:
//                o7 = t;
//                t = t.getParent();
//            case 6:
//                o6 = t;
//                t = t.getParent();
//            case 5:
//                o5 = t;
//                t = t.getParent();
//            case 4:
//                o4 = t;
//                t = t.getParent();
//            case 3:
//                o3 = t;
//                t = t.getParent();
//            case 2:
//                o2 = t;
//                t = t.getParent();
//            case 1:
//                o1 = t;
//        }
//    }

    public void clear() {
        switch (predicate.getArity()) {
            case 10:
                o10 = null;
            case 9:
                o9 = null;
            case 8:
                o8 = null;
            case 7:
                o7 = null;
            case 6:
                o6 = null;
            case 5:
                o5 = null;
            case 4:
                o4 = null;
            case 3:
                o3 = null;
            case 2:
                o2 = null;
            case 1:
                o1 = null;
        }
        predicate = null;
    }
}

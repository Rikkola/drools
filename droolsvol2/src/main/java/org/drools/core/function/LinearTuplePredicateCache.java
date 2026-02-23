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

    private Predicate p;

    public LinearTuplePredicateCache(Predicate p) {
        this.p = p;
    }

    public void setLeft(TupleImpl t) {
        switch (t.getNetworkNode().getPathIndex()) {
            case 8:
                o8 = t;
                t = t.getParent();
            case 7:
                o7 = t;
                t = t.getParent();
            case 6:
                o6 = t;
                t = t.getParent();
            case 5:
                o5 = t;
                t = t.getParent();
            case 4:
                o4 = t;
                t = t.getParent();
            case 3:
                o3 = t;
                t = t.getParent();
            case 2:
                o2 = t;
                t = t.getParent();
            case 1:
                o1 = t;
                break;
            default:
                throw new IllegalArgumentException("Illegal tuple predicate index: " + t.getNetworkNode().getPathIndex());

        }
    }

    public boolean applyRight(Context ctx, TupleImpl rt) {
        switch (p.getArity()) {
            case 3:
                return ((Predicate3)p).test(ctx, o1.get(), rt.get());
            case 4:
                return ((Predicate4)p).test(ctx, o1.get(), o2.get(), rt.get());
            case 5:
                return ((Predicate5)p).test(ctx, o1.get(), o2.get(), o3.get(), rt.get());
            case 6:
                return ((Predicate6)p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), rt.get());
            case 7:
                return ((Predicate7)p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), rt.get());
            case 8:
                return ((Predicate8)p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), rt.get());
            case 9:
                return ((Predicate9)p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o7.get(), rt.get());
            case 10:
                return ((Predicate10)p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o7.get(), o8.get(), rt.get());

        }

        throw new IllegalArgumentException("Invalid predicate arity (" + p.getArity() + ")");
    }

    public void setRight(TupleImpl rt) {
        switch (p.getArity()) {
            case 10:
                o9 = rt;
                break;
            case 9:
                o8 = rt;
                break;
            case 8:
                o7 = rt;
                break;
            case 7:
                o6 = rt;
                break;
            case 6:
                o5 = rt;
                break;
            case 5:
                o4 = rt;
                break;
            case 4:
                o3 = rt;
                break;
            case 3:
                o2 = rt;
                break;
            case 2:
                o1 = rt;
                break;
            default:
                throw new IllegalArgumentException("Illegal tuple predicate index: " + rt.getNetworkNode().getPathIndex());
        }
    }

    public boolean applyLeft(Context ctx, TupleImpl t) {
        setLeft(t);
        switch (p.getArity()) {
            case 3:
                return ((Predicate3)p).test(ctx, o1.get(), o2.get());
            case 4:
                return ((Predicate4)p).test(ctx, o1.get(), o2.get(), o3.get());
            case 5:
                return ((Predicate5)p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get());
            case 6:
                return ((Predicate6)p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get());
            case 7:
                return ((Predicate7)p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get());
            case 8:
                return ((Predicate8)p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o7.get());
            case 9:
                return ((Predicate9)p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o7.get(), o8.get());
            case 10:
                return ((Predicate10)p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o7.get(), o8.get(), o9.get());
        }

        throw new IllegalArgumentException("Invalid predicate arity (" + p.getArity() + ")");
    }

    public void clear() {
        switch (p.getArity()) {
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
    }

    public TupleImpl[] values() {
        return new TupleImpl[] {
                null, o1, o2, o3, o4, o5, o6, o7, o8, o9
        };
    }

    @Override
    public String toString() {
        return "BiLinearTuplePredicateCache[" +
               "p=" + p +
               ", o9=" + o9 +
               ", o8=" + o8 +
               ", o7=" + o7 +
               ", o6=" + o6 +
               ", o5=" + o5 +
               ", o4=" + o4 +
               ", o3=" + o3 +
               ", o2=" + o2 +
               ", o1=" + o1 +
               ']';
    }
}

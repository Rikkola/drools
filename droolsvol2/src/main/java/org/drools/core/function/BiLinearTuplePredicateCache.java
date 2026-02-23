package org.drools.core.function;

import org.drools.core.Context;
import org.drools.core.ObjectHandleTuple;
import org.drools.core.TupleImpl;

public class BiLinearTuplePredicateCache {
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

    public BiLinearTuplePredicateCache(Predicate p) {
        this.p = p;
    }

    public void setLeft(TupleImpl t) {
        doloop: do {
            switch (t.getNetworkNode().getObjectIndex()) {
                case 9:
                    if (t.getRightParent().getClass() != ObjectHandleTuple.class) {
                        break;
                    }
                    o9 = t.getRightParent();;
                    t  = t.getParent();
                case 8:
                    if (t.getRightParent().getClass() != ObjectHandleTuple.class) {
                        break;
                    }
                    o8 = t.getRightParent();;
                    t  = t.getParent();
                case 7:
                    if (t.getRightParent().getClass() != ObjectHandleTuple.class) {
                        break;
                    }
                    o7 = t.getRightParent();;
                    t  = t.getParent();
                case 6:
                    if (t.getRightParent().getClass() != ObjectHandleTuple.class) {
                        break;
                    }
                    o6 = t.getRightParent();;
                    t  = t.getParent();
                case 5:
                    if (t.getRightParent().getClass() != ObjectHandleTuple.class) {
                        break;
                    }
                    o5 = t.getRightParent();;
                    t  = t.getParent();
                case 4:
                    if (t.getRightParent().getClass() != ObjectHandleTuple.class) {
                        break;
                    }
                    o4 = t.getRightParent();;
                    t  = t.getParent();
                case 3:
                    if (t.getRightParent().getClass() != ObjectHandleTuple.class) {
                        break;
                    }
                    o3 = t.getRightParent();;
                    t  = t.getParent();
                case 2:
                    if (t.getRightParent().getClass() != ObjectHandleTuple.class) {
                        break;
                    }
                    o2 = t.getRightParent();
                    t  = t.getParent();
                case 1:
                    o1 = t; // this is coming from the lian, so already a ObjectHandleTuple
                    break doloop;
                default:
                    throw new IllegalArgumentException("Illegal tuple predicate index: " + t.getNetworkNode().getObjectIndex());
            }
            walkTreeAndAssign(t);
            t = t.getParent();
        } while (true);
    }

    public void setRight(TupleImpl t) {
        if(t.getNetworkNode().getSize() == 1) {
            switch (t.getNetworkNode().getObjectIndex()) {
                case 9:
                    o9 = t;
                    break;
                case 8:
                    o8 = t;
                    break;
                case 7:
                    o7 = t;
                    break;
                case 6:
                    o6 = t;
                    break;
                case 5:
                    o5 = t;
                    break;
                case 4:
                    o4 = t;
                    break;
                case 3:
                    o3 = t;
                    break;
                case 2:
                    o2 = t;
                    break;
                case 1:
                    o1 = t;
                    break;
                default:
                    throw new IllegalArgumentException("Illegal tuple predicate index: " + t.getNetworkNode().getObjectIndex());
            }
        } else {
            walkTreeAndAssign(t);
        }
    }

    public boolean applyRight(Context ctx, TupleImpl rt) {
        setRight(rt);
        return test(ctx);
    }

    public boolean applyLeft(Context ctx, TupleImpl lt) {
        setLeft(lt);
        return test(ctx);
    }

    private boolean test(Context ctx) {
        switch (p.getArity()) {
            case 3:
                return ((Predicate3) p).test(ctx, o1.get(), o2.get());
            case 4:
                return ((Predicate4) p).test(ctx, o1.get(), o2.get(), o3.get());
            case 5:
                return ((Predicate5) p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get());
            case 6:
                return ((Predicate6) p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get());
            case 7:
                return ((Predicate7) p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get());
            case 8:
                return ((Predicate8) p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o7.get());
            case 9:
                return ((Predicate9) p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o7.get(), o8.get());
            case 10:
                return ((Predicate10) p).test(ctx, o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o7.get(), o8.get(), o9.get());
        }

        throw new IllegalArgumentException("Invalid predicate arity (" + p.getArity() + ")");
    }


    public void walkTreeAndAssign(TupleImpl t0) {
        int objectIndex = t0.getNetworkNode().getObjectIndex();
        int size = t0.getNetworkNode().getSize();

        int startIndex = objectIndex - size;

        TupleImpl t1 = null, t2 = null, t3 = null, t4 = null, t5 = null, t6 = null, t7 = null, t8 = null, t9 = null;
        int index = 0;
        TupleImpl c = t0;
        while (true) {
            // Find the next handle by traversing the graph, always going right when possible.
            while (c.getClass() != ObjectHandleTuple.class) {
                if (c.hasRightParent()) {
                    // assign the tuple to the register stack index
                    switch (index) {
                        case 0: break; // t0 is root and already assigned
                        case 1: t1 = c; break;
                        case 2: t2 = c; break;
                        case 3: t3 = c; break;
                        case 4: t4 = c; break;
                        case 5: t5 = c; break;
                        case 6: t6 = c; break;
                        case 7: t7 = c; break;
                        case 8: t8 = c; break;
                        case 9: t9 = c; break;
                        default: throw new IllegalStateException("Illegal tuple index: " + index);
                    }
                    c = c.getRightParent();
                    index++; // as it goes down the graph, it must incease the index
                } else {
                    // It does not need to go back to this node, so no need to record it in the registry nor increase the index
                    c = c.getLeftParent(); // this is for cases like eval, that has no right input
                }
            }

            // assign the current node to the correct position in the cache
            switch (startIndex + size) {
                case 1: o1 = c; break;
                case 2: o2 = c; break;
                case 3: o3 = c; break;
                case 4: o4 = c; break;
                case 5: o5 = c; break;
                case 6: o6 = c; break;
                case 7: o7 = c; break;
                case 8: o8 = c; break;
                case 9: o9 = c; break;
                default: throw new IllegalStateException("Illegal tuple index: " + (startIndex + size - 1));
            }
            size--;

            // walk back to the correct index to proceed from
            index = index - c.getNetworkNode().getWalkBack();

            if (size > 0) {
                // always go left from the current index position
                switch (index) {
                    case 0:
                        c = t0.getLeftParent();
                        break;
                    case 1:
                        c = t1.getLeftParent();
                        break;
                    case 2:
                        c = t2.getLeftParent();
                        break;
                    case 3:
                        c = t3.getLeftParent();
                        break;
                    case 4:
                        c = t4.getLeftParent();
                        break;
                    case 5:
                        c = t5.getLeftParent();
                        break;
                    case 6:
                        c = t6.getLeftParent();
                        break;
                    case 7:
                        c = t7.getLeftParent();
                        break;
                    case 8:
                        c = t8.getLeftParent();
                        break;
                    case 9:
                        c = t9.getLeftParent();
                        break;
                    default:
                        throw new IllegalStateException("Illegal tuple index: " + index);
                }
                index++; // it's gone down the graph, so don't forget to increase the index
            } else {
                break;
            }
        }
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
        TupleImpl[] tuples = new TupleImpl[p.getArity()];

        switch (p.getArity()) {
            case 10:
                tuples[9] = o9;
            case 9:
                tuples[8] = o8;
            case 8:
                tuples[7] = o7;
            case 7:
                tuples[6] = o6;
            case 6:
                tuples[5] = o5;
            case 5:
                tuples[4] = o4;
            case 4:
                tuples[3] = o3;
            case 3:
                tuples[2] = o2;
            case 2:
                tuples[1] = o1;
            case 1:
                tuples[0] = null;
        }


        return tuples;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for ( int x = 0; x < p.getArity(); x++) {
            if ( x < p.getArity() - 1) {
                sb.append(", ");
            }
            sb.append(", o=" + x + "o" + x);
        }

        return "BiLinearTuplePredicateCache[" +
               "p(" + p.getArity() + ")" + sb + ']';
    }

}

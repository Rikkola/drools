package org.drools.core.rete;

import org.drools.api.data.DataHandle;
import org.drools.core.Context;
import org.drools.core.function.Predicate4;

public class Join3<DS, B, C, D> {
    private TupleSource leftInput;

    private TupleSource rightInput;

    private TupleSink sink;

    private Predicate4 predicate4;

    private void leftAdd(Context<DS> ctx, DataHandle<B> b, DataHandle<C> c) {
        DataHandle<D> d = null;
        if (predicate4.test(ctx, b, c, d)) {

        }
    }
}

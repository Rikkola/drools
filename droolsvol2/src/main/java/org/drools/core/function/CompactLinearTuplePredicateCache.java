package org.drools.core.function;

import org.drools.base.rule.Declaration;
import org.drools.core.NetworkNode;
import org.drools.core.TupleImpl;

public class CompactLinearTuplePredicateCache {
    private Declaration[] declarations;
    private int[] skipNext;
    private NetworkNode node;

    private TupleImpl o1;
    private TupleImpl o2;
    private TupleImpl o3;
    private TupleImpl o4;
    private TupleImpl o5;
    private TupleImpl o6;
    private TupleImpl o7;
    private TupleImpl o8;
    private TupleImpl o9;

    public CompactLinearTuplePredicateCache(NetworkNode node, Declaration[] declarations) {
        this.node = node;
        this.declarations = declarations;

        int j = declarations.length-1;
        int skipCounter = 0;
        skipNext = new int[declarations.length];
        int k = 0;
        for (int i = node.getObjectIndex(); i >= 0 && j >= 0; i--) {
            if (i == declarations[j].getObjectIndex()) {
                skipNext[k++] =  skipCounter;
                skipCounter = 0;
                j--;
            } else {
                skipCounter++;
            }
        }
    }

    public int[] getSkipNext() {
        return skipNext;
    }

    public void setLeft(TupleImpl t) {
        int index = node.getObjectIndex();

        for  (int i = skipNext.length-1; i >= 0; i--) {
            switch (skipNext[i]) {
                case 9:
                    t = t.getLeftParent();
                case 8:
                    t = t.getLeftParent();
                case 7:
                    t = t.getLeftParent();
                case 6:
                    t = t.getLeftParent();
                case 5:
                    t = t.getLeftParent();
                case 4:
                    t = t.getLeftParent();
                case 3:
                    t = t.getLeftParent();
                case 2:
                    t = t.getLeftParent();
                case 1:
                    t = t.getLeftParent();
            }
        }

    }
}

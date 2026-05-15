package org.drools.base.reteoo;

import org.drools.base.rule.constraint.AlphaNodeFieldConstraint;

public class DynamicFilterProto {
    private AlphaNodeFieldConstraint constraint;
    private int                      filterIndex;
    private int                      otnIndex;

    public DynamicFilterProto(AlphaNodeFieldConstraint constraint, int filterIndex, int otnIndex) {
        this.constraint  = constraint;
        this.filterIndex = filterIndex;
        this.otnIndex    = otnIndex;
    }

    public AlphaNodeFieldConstraint getConstraint() {
        return constraint;
    }

    public int getFilterIndex() {
        return filterIndex;
    }

    public int getOtnIndex() {
        return otnIndex;
    }
}

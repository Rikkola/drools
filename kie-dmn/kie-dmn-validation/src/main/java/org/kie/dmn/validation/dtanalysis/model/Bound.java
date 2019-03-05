package org.kie.dmn.validation.dtanalysis.model;

import org.kie.dmn.feel.runtime.Range;
import org.kie.dmn.feel.runtime.Range.RangeBoundary;

public class Bound<V extends Comparable<V>> implements Comparable<Bound<V>> {

    private final V value;
    private final Range.RangeBoundary boundaryType;
    private final Interval parent;

    public Bound(V value, RangeBoundary boundaryType, Interval parent) {
        this.value = value;
        this.boundaryType = boundaryType;
        this.parent = parent;
    }

    @Override
    public int compareTo(Bound<V> o) {
        if (this.parent == o.parent && parent != null && o.parent != null) {
            // never swap lower/upper bounds of the same interval
            if (this.isLowerBound()) {
                return -1;
            } else {
                return 1;
            }
        }

        int valueCompare = compareValueDispatchingToInf(o);
        if (valueCompare != 0) {
            return valueCompare;
        }

        if (parent != null && o.parent != null) {
            if (this.isUpperBound() && o.isLowerBound()) {
                return -1;
            } else if (this.isLowerBound() && o.isUpperBound()) {
                return 1;
            }
        }

        if (this.boundaryType == o.boundaryType) {
            return 0;
        } else if (this.boundaryType == RangeBoundary.OPEN) {
            return -1;
        } else {
            return 1;
        }
    }

    private int compareValueDispatchingToInf(Bound<V> o) {
        if (this.value != Interval.NEG_INF && this.value != Interval.POS_INF && (o.value == Interval.NEG_INF || o.value == Interval.POS_INF)) {
            return 0 - o.value.compareTo(this.value);
        }
        return this.value.compareTo(o.value);
    }

    public V getValue() {
        return value;
    }

    public Range.RangeBoundary getBoundaryType() {
        return boundaryType;
    }

    public Interval getParent() {
        return parent;
    }

    public boolean isLowerBound() {
        return parent.getLowerBound() == this;
    }

    public boolean isUpperBound() {
        return parent.getUpperBound() == this;
    }

    @Override
    public String toString() {
        if (isLowerBound()) {
            return (boundaryType == RangeBoundary.OPEN ? "(" : "[") + value;
        } else {
            return value + (boundaryType == RangeBoundary.OPEN ? ")" : "]");
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((boundaryType == null) ? 0 : boundaryType.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Bound other = (Bound) obj;
        if (boundaryType != other.boundaryType)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    /**
     * Returns true if left is overlapping or adjacent to right
     */
    public static boolean adOrOver(Bound<?> left, Bound<?> right) {
        boolean isValueEqual = left.getValue().equals(right.getValue());
        boolean isBothOpen = left.getBoundaryType() == RangeBoundary.OPEN && right.getBoundaryType() == RangeBoundary.OPEN;
        return isValueEqual && !isBothOpen;
    }
}
package org.drools.core;

import org.drools.api.data.DataHandle;
import org.drools.api.data.DataProcessor;
import org.drools.base.rule.constraint.AlphaNodeFieldConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FilterNode<T> extends NetworkNode {
    private NetworkNode                 parent;
    private AlphaNodeFieldConstraint predicate;
    private List<NetworkNode>        children;

    public NetworkNode getParent() {
        return parent;
    }

    public void setParent(NetworkNode parent) {
        this.parent = parent;
    }

    public AlphaNodeFieldConstraint getPredicate() {
        return predicate;
    }

    public void setPredicate(AlphaNodeFieldConstraint predicate) {
        this.predicate = predicate;
    }

    public List<NetworkNode> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<NetworkNode> children) {
        this.children = children;
    }
}

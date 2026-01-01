package org.drools.core;

import org.drools.core.function.Predicate2;

import java.util.ArrayList;
import java.util.List;

public class FilterNode<DS, T> extends NetworkNode {
    private NetworkNode                parent;
    private Predicate2<Context<DS>, T> predicate;
    private List<NetworkNode>          children;

    public NetworkNode getParent() {
        return parent;
    }

    public void setParent(NetworkNode parent) {
        this.parent = parent;
    }

    public Predicate2<Context<DS>, T> getPredicate() {
        return predicate;
    }

    public List<NetworkNode> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<NetworkNode> children) {
        this.children = children;
    }
}

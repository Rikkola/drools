package org.drools.core.rete.builder;

import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.base.rule.GroupElement;
import org.drools.base.rule.LogicTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ReteBuilder {
    private BuildUtils buildUtils = new BuildUtils();

    public ReteBuilder() {
        //buildUtils.addBuilder();
    }

    public void addRule(RuleImpl rule) {
        GroupElement group = rule.getBody();

        // the list of terminal nodes
        //final List<TerminalNode> termNodes = new ArrayList<>();

        // transform rule and gets the array of subrules
        final GroupElement[] subrules = rule.getTransformedLhs(LogicTransformer.getInstance(), Collections.emptyMap());
    }

}

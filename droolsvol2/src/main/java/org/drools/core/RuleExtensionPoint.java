package org.drools.core;

import org.kie.api.definition.rule.Rule;

public class RuleExtensionPoint {
    private Rule rule;
    private int arity;

    public RuleExtensionPoint(Rule rule, int arity) {
        this.rule = rule;
        this.arity = arity;
    }

    public int arity() {
        return arity;
    }

    public void setArity(int arity) {
        this.arity = arity;
    }

    public static class RuleExtensionPoint2<DS, B> extends RuleExtensionPoint {
        public RuleExtensionPoint2(Rule rule) {
            super(rule,2);
        }
    }

    public static class RuleExtensionPoint3<DS, B, C> extends RuleExtensionPoint {
        public RuleExtensionPoint3(Rule rule) {
            super(rule,3);
        }
    }

    public static class RuleExtensionPoint4<DS, B, C, D> extends RuleExtensionPoint {
        public RuleExtensionPoint4(Rule rule) {
            super(rule,4);
        }
    }

    public static class RuleExtensionPoint5<DS, B, C, D, E> extends RuleExtensionPoint {
        public RuleExtensionPoint5(Rule rule) {
            super(rule, 5);
        }
    }

    public static class RuleExtensionPoint6<DS, B, C, D, E, F> extends RuleExtensionPoint {
        public RuleExtensionPoint6(Rule rule) {
            super(rule, 6);
        }
    }
}

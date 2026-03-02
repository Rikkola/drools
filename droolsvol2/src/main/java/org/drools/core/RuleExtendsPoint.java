package org.drools.core;

import org.kie.api.definition.rule.Rule;

public class RuleExtendsPoint {
    private Rule rule;
    private int arity;

    public RuleExtendsPoint(Rule rule, int arity) {
        this.rule = rule;
        this.arity = arity;
    }

    public int arity() {
        return arity;
    }

    public void setArity(int arity) {
        this.arity = arity;
    }

    public static class RuleExtendsPoint2<DS, B> extends RuleExtendsPoint {
        public RuleExtendsPoint2(Rule rule) {
            super(rule,2);
        }
    }

    public static class RuleExtendsPoint3<DS, B, C> extends RuleExtendsPoint {
        public RuleExtendsPoint3(Rule rule) {
            super(rule,3);
        }
    }

    public static class RuleExtendsPoint4<DS, B, C, D> extends RuleExtendsPoint {
        public RuleExtendsPoint4(Rule rule) {
            super(rule,4);
        }
    }

    public static class RuleExtendsPoint5<DS, B, C, D, E> extends RuleExtendsPoint {
        public RuleExtendsPoint5(Rule rule) {
            super(rule, 5);
        }
    }

    public static class RuleExtensionPoint6<DS, B, C, D, E, F> extends RuleExtendsPoint {
        public RuleExtensionPoint6(Rule rule) {
            super(rule, 6);
        }
    }
}

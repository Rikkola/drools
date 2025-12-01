package org.drools.core;

import org.drools.core.RuleBuilder.BaseRuleBuilder;

public class RuleBase<DS> {
    public RuleBase() {

    }

    public ChangeSetSelector<DS> modify() {
        return new ChangeSetSelector();
    }

    public class ChangeSetSelector<DS> {
        public ChangeSet<DS> with(String pkgName) {
            return new ChangeSet(pkgName);
        }
    }

    public class ChangeSet<DS> {

        public ChangeSet(String pkgName) {

        }

        public ChangeSet<DS> with(String pkgName) {
            return new ChangeSet(pkgName);
        }

        public ChangeSet<DS> add(BaseRuleBuilder rule) {
            return this;
        }

        public ChangeSet<DS> remove(String rule) {
            return this;
        }

        public void apply() {

        }
        
    }
}

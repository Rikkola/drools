package org.drools.core;

import org.drools.core.RuleBuilder.BaseRuleBuilder;

public class RuleBase<DS> {

    public RuleBase() {

    }

    public ChangeSetSelector<DS> modify() {
        return new ChangeSetSelector();
    }

    public class ChangeSetSelector<DS> {
        public ChangeSet<DS> selectPackage(String packageName) {
            return new ChangeSet(packageName);
        }
    }

    public class ChangeSet<DS> {

        public ChangeSet(String packageName) {

        }

        public ChangeSet<DS> selectPackage(String packageName) {
            return null;
        }

        public ChangeSet<DS> selectUnit(String unitName) {
            return null;
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

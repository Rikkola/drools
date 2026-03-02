package org.drools.core;

import org.drools.core.RuleBaseModifier.ChangeSet;
import org.drools.core.RuleBaseModifier.ChangeSetBuilder;
import org.drools.core.RuleBaseModifier.PackageChangeSet;
import org.drools.core.RuleBaseModifier.UnitChangeSet;
import org.drools.core.RuleBuilder.BaseRuleBuilder;
import org.kie.api.definition.rule.Rule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RuleBase<DS> {

    //private transient Rete rete;
    //private ReteooBuilder reteooBuilder;

    public RuleBase() {

    }

    public <DS> void apply(ChangeSetBuilder<DS> changeSetBuilder) {
        ChangeSet<DS> changeSet = changeSetBuilder.getChangeSet();

//        for(PackageChangeSet<DS> packages : changeSet.getRemoved().values()) {
//            for(UnitChangeSet<DS> units : packages.getRemoved().values()) {
//                for(Rule rule : units.getRemoved()) {
//
//
//                }
//            }
//        }

        for(PackageChangeSet<DS> packages : changeSet.getAdded().values()) {
            for(UnitChangeSet<DS> units : packages.getAdded().values()) {
                for(Rule rule : units.getAdded().values()) {


                }
            }
        }
    }


}

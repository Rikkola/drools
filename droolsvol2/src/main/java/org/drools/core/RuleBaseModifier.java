package org.drools.core;

import org.drools.core.RuleBuilder.BaseRuleBuilder;
import org.kie.api.definition.rule.Rule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RuleBaseModifier {

    public static <DS> ChangeSet<DS> changeSet() {
        return new ChangeSet<>();
    }

    public static <DS> ChangeSetModifier<DS> with(RuleBase<DS> ruleBase) {
        return new ChangeSetModifier<>(ruleBase);
    }

    public static class ChangeSetModifier<DS> {
        RuleBase<DS> ruleBase;

        public ChangeSetModifier(RuleBase<DS> ruleBase) {
            this.ruleBase = ruleBase;
        }

        public <DS> void apply(ChangeSetBuilder<DS> changeSetBuilder) {
            ChangeSet<DS> changeSet = changeSetBuilder.getChangeSet();
            for(PackageChangeSet<DS> packages : changeSet.added.values()) {
               for(UnitChangeSet<DS> units : packages.added.values()) {
                   for(Rule rule : units.added.values()) {


                   }
               }
            }
        }
    }

    public interface ChangeSetBuilder<DS> {
        ChangeSet<DS>  getChangeSet();
    }

    public static class ChangeSet<DS> implements ChangeSetBuilder<DS> {
        private Map<String, PackageChangeSet<DS>> added   = new HashMap<>();
        private Set<String>                       removed = new HashSet<>();
        private PackageChangeSet<DS>              packageChangeSet;

        public PackageChangeSet<DS> selectPackage(String packageName) {
            packageChangeSet = added.get(packageName);
            if (packageChangeSet == null) {
                packageChangeSet = new PackageChangeSet<DS>();
                added.put(packageName, packageChangeSet);
            }
            return packageChangeSet;
        }

        public ChangeSet<DS> remove(String packageName) {
            this.added.remove(packageName);
            this.removed.add(packageName);
            return this;
        }

        @Override
        public ChangeSet<DS> getChangeSet() {
            return this;
        }

        public Map<String, PackageChangeSet<DS>> getAdded() {
            return added;
        }

        public Set<String> getRemoved() {
            return removed;
        }
    }


    public static class PackageChangeSet<DS> implements ChangeSetBuilder<DS> {
        private ChangeSet<DS>              changeSet;
        private String                     packageName;
        private Map<String, UnitChangeSet> added = new HashMap<>();
        private Set<String>                removed = new HashSet<>();
        private UnitChangeSet<DS>          unitChangeSet;

        public UnitChangeSet<DS> selectUnit(String unitName) {
            unitChangeSet = added.get(unitName);
            if (unitChangeSet == null) {
                unitChangeSet =  new UnitChangeSet<DS>(this);
                added.put(unitName, unitChangeSet);
            }

            return unitChangeSet;
        }

        public PackageChangeSet<DS> remove(String unitName) {
            this.added.remove(unitName);
            this.removed.add(unitName);
            return this;
        }

        public String getPackageName() {
            return packageName;
        }

        public Map<String, UnitChangeSet> getAdded() {
            return added;
        }

        public Set<String> getRemoved() {
            return removed;
        }

        @Override
        public ChangeSet<DS> getChangeSet() {
            return changeSet;
        }
    }

    public static class UnitChangeSet<DS> implements ChangeSetBuilder<DS>  {
        private PackageChangeSet<DS> packageChangeSet;
        private Map<String, Rule>    added   = new HashMap<String, Rule>();
        private Set<String>          removed = new HashSet<>();

        public UnitChangeSet(PackageChangeSet<DS> packageChangeSet) {
            this.packageChangeSet = packageChangeSet;
        }

        public PackageChangeSet<DS> selectPackage(String packageName) {
            return packageChangeSet.changeSet.selectPackage(packageName);
        }

        public UnitChangeSet<DS> selectUnit(String unitName) {
            return packageChangeSet.selectUnit(unitName);
        }

        public UnitChangeSet<DS> add(BaseRuleBuilder builder) {
            Rule rule = builder.build();
            added.put(rule.getName(), rule);
            return this;
        }

        public UnitChangeSet<DS> remove(String rule) {
            added.remove(rule);
            removed.remove(rule);
            return this;
        }

        public Map<String, Rule> getAdded() {
            return added;
        }

        public Set<String> getRemoved() {
            return removed;
        }

        @Override
        public ChangeSet<DS> getChangeSet() {
            return packageChangeSet.changeSet;
        }
    }
}

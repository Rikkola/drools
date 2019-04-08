/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.verifier.core.checks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.drools.verifier.api.reporting.CheckType;
import org.drools.verifier.api.reporting.Issue;
import org.drools.verifier.api.reporting.Severity;
import org.drools.verifier.core.checks.base.CheckBase;
import org.drools.verifier.core.configuration.AnalyzerConfiguration;
import org.drools.verifier.core.index.Index;
import org.drools.verifier.core.index.keys.Values;
import org.drools.verifier.core.index.matchers.UUIDMatcher;
import org.drools.verifier.core.index.model.Condition;
import org.drools.verifier.core.index.model.FieldCondition;
import org.drools.verifier.core.index.model.ObjectField;
import org.drools.verifier.core.index.model.Rule;

public class TheoryCheck extends CheckBase {

    private final Index index;

    public TheoryCheck(final Index index,
                       final AnalyzerConfiguration configuration) {
        super(configuration);
        this.index = index;
    }

    @Override
    public boolean check() {

        Iterator<ObjectField> iterator = index.getObjectTypes().where(UUIDMatcher.uuid()
                                                                              .any()).select().fields().where(UUIDMatcher.uuid()
                                                                                                                      .any()).select().all().iterator();
        final Tree node = new Tree(iterator);

        for (final Rule rule : index.getRules().where(UUIDMatcher.uuid()
                                                              .any()).select().all()) {

            node.append(rule);
        }

        return false;
    }

    @Override
    protected List<Issue> makeIssues(Severity severity, CheckType checkType) {
        return new ArrayList<Issue>();
    }

    @Override
    protected CheckType getCheckType() {
        return CheckType.MISSING_RANGE;
    }

    @Override
    protected Severity getDefaultSeverity() {
        return Severity.NOTE;
    }

    private class Tree {

        private final Tree next;
        private final ObjectField objectField;

        private Map<Values, Tree> map = new HashMap<Values, Tree>();
        private List<Rule> rules = new ArrayList<Rule>();

        public Tree(final Iterator<ObjectField> iterator) {
            if (iterator.hasNext()) {
                this.objectField = iterator.next();
                this.next = new Tree(iterator);

                for (Rule rule : index.getRules().where(UUIDMatcher.uuid().any()).select().all()) {
                    for (Condition condition : rule.getConditions().where(UUIDMatcher.uuid().any()).select().all()) {
                        if (condition instanceof FieldCondition) {
                            if (((FieldCondition) condition).getField().getObjectField().equals(objectField)) {

                                if (!map.containsKey(condition.getValues())) {
                                    map.put(condition.getValues(),
                                            new Tree(iterator));
                                }

                                // FOUND IT
                            }
                        }
                    }
                }
            } else {
                this.next = null;
                this.objectField = null;
            }
        }

        public void append(final Rule rule) {
            Collection<Condition> all = rule.getConditions().where(UUIDMatcher.uuid().any()).select().all();

            for (Condition condition : all) {
                if (condition instanceof FieldCondition) {
                    if (((FieldCondition) condition).getField().getObjectField().equals(objectField)) {

                        if (next == null) {
                            rules.add(rule);
                        } else {
                            //TODO : Append to all values that are equal
                            map.get(condition.getValues()).append(rule);
                        }
                    }
                }
            }

            if (all.isEmpty()) {
                for (final Tree tree : map.values()) {
                    tree.append(rule);
                }
            }
        }
    }
}

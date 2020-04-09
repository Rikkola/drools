/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.drools.verifier.api.reporting.CheckType;
import org.drools.verifier.api.reporting.Issue;
import org.drools.verifier.api.reporting.OverlappingIssue;
import org.drools.verifier.api.reporting.Severity;
import org.drools.verifier.api.reporting.model.Interval;
import org.drools.verifier.core.cache.inspectors.RuleInspector;
import org.drools.verifier.core.cache.inspectors.RuleInspectorDumper;
import org.drools.verifier.core.cache.inspectors.condition.ComparableConditionInspector;
import org.drools.verifier.core.checks.base.PairCheck;
import org.drools.verifier.core.configuration.AnalyzerConfiguration;
import org.drools.verifier.core.index.Index;
import org.drools.verifier.core.index.model.Action;
import org.drools.verifier.core.index.model.Column;
import org.drools.verifier.core.index.model.ColumnType;
import org.drools.verifier.core.index.model.Condition;
import org.drools.verifier.core.util.PortablePreconditions;

public class OverlappingRowsCheck
        extends PairCheck {

    private final Index index;

    public OverlappingRowsCheck(final RuleInspector ruleInspector,
                                final RuleInspector other,
                                final Index index,
                                final AnalyzerConfiguration configuration) {
        super(ruleInspector,
              other,
              configuration);
        this.index = PortablePreconditions.checkNotNull("index",
                                                        index);
    }

    @Override
    protected CheckType getCheckType() {
        return CheckType.OVERLAPPING_ROWS;
    }

    @Override
    protected Severity getDefaultSeverity() {
        return Severity.WARNING;
    }

    @Override
    public boolean check() {
        hasIssues = ruleInspector.overlaps(other);
        return hasIssues;
    }

    @Override
    protected List<Issue> makeIssues(final Severity severity,
                                     final CheckType checkType) {

        final List<Interval> intervals = getLongerConditionList();

        final boolean containsAnyValueField = getContainsAnyValueCell();
        final OverlappingIssue issue = new OverlappingIssue(severity,
                                                            checkType,
                                                            intervals,
                                                            containsAnyValueField,
                                                            getRHSValues(),
                                                            Arrays.asList(ruleInspector.getRowIndex() + 1,
                                                                          other.getRowIndex() + 1)
        );
        issue.setDebugMessage(new RuleInspectorDumper(ruleInspector).dump() + " ## " + new RuleInspectorDumper(other).dump());

        return Collections.singletonList((Issue) issue);
    }

    private boolean getContainsAnyValueCell() {
        for (final Column column : index.getColumns().where(Column.columnType().is(ColumnType.LHS)).select().all()) {
            if (!ruleInspector.getRule().getConditions().where(Condition.columnUUID().is(column.getUuidKey())).select().exists()) {
                return true;
            }
        }

        return false;
    }

    private Map<Integer, String> getRHSValues() {
        final Map<Integer, String> result = new HashMap<>();

        for (final Column column : index.getColumns().where(Column.columnType().is(ColumnType.RHS)).select().all()) {
            for (final Action action : ruleInspector.getRule().getActions().where(Action.columnUUID().is(column.getUuidKey())).select().all()) {
                result.put(column.getIndex(), action.getValues().stream().map(Object::toString).collect(Collectors.joining(",")));
            }
        }

        return result;
    }

    private List<Interval> getLongerConditionList() {

        final List<Interval> intervalsOther = getIntervals(other);
        final List<Interval> intervals = getIntervals(ruleInspector);

        if (intervals.size() > intervalsOther.size()) {
            return intervals;
        } else {
            return intervalsOther;
        }
    }

    private List<Interval> getIntervals(final RuleInspector other) {
        final List<Interval> intervals = new ArrayList<Interval>();

        for (Object o : other.getConditionsInspectors()
                .stream().flatMap(x -> x.allValues().stream())
                .collect(Collectors.toList())) {
            if (o instanceof ComparableConditionInspector) {
                intervals.add(((ComparableConditionInspector) o).getInterval());
            }
        }

        return intervals;
    }
}

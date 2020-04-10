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
import org.drools.verifier.api.reporting.model.Bound;
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
import org.drools.verifier.core.index.model.meta.ConditionParentType;
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

    private List<Interval> getLongerConditionList() {
        final ArrayList<Interval> result = new ArrayList<Interval>();

        final Map<ConditionParentType, Interval> intervalsOther = getIntervals(other);
        final Map<ConditionParentType, Interval> intervals = getIntervals(ruleInspector);

        for (ConditionParentType key : intervalsOther.keySet()) {
            if (intervals.containsKey(key)) {
                final Interval other = intervalsOther.get(key);
                final Interval interval = intervals.get(key);
                if (areIntervalsOverlapping(other, interval)) {
                    Interval e = Interval.newFromBounds(
                            getLowerBound(other.getLowerBound(),
                                          interval.getLowerBound()),
                            getHigherBound(other.getUpperBound(),
                                           interval.getUpperBound())
                    );
                    result.add(e);
                }
            } else {
                result.add(intervalsOther.get(key));
            }
        }

        for (ConditionParentType key : intervals.keySet()) {
            if (!intervalsOther.containsKey(key)) {
                result.add(intervals.get(key));
            }
        }

        return result;
    }

    private Bound getHigherBound(final Bound other,
                                 final Bound interval) {
        if (other.compareTo(interval) >= 0) {
            return other;
        } else {
            return interval;
        }
    }

    private Bound getLowerBound(final Bound other,
                                final Bound interval) {
        if (other.compareTo(interval) >= 0) {
            return interval;
        } else {
            return other;
        }
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

    private Map<ConditionParentType, Interval> getIntervals(final RuleInspector other) {
        final Map<ConditionParentType, Interval> intervals = new HashMap<>();

        List<Object> collect = other.getConditionsInspectors()
                .stream().flatMap(x -> x.allValues().stream())
                .collect(Collectors.toList());

        for (Object o : collect) {
            if (o instanceof ComparableConditionInspector) {
                final ComparableConditionInspector conditionInspector = (ComparableConditionInspector) o;

                if (intervals.containsKey(conditionInspector.getField().getConditionParentType())) {

                    final Interval first = intervals.get(conditionInspector.getField().getConditionParentType());
                    final Interval second = conditionInspector.getInterval();
                    final Bound firstLowerBound = first.getLowerBound();
                    final Bound secondUpperBound = second.getUpperBound();

                    if (areIntervalsOverlapping(first, second)) {
                        final Interval interval = Interval.newFromBounds(firstLowerBound,
                                                                         secondUpperBound);
                        intervals.put(conditionInspector.getField().getConditionParentType(),
                                      interval);
                    }
                } else {
                    Interval interval = conditionInspector.getInterval();
                    intervals.put(conditionInspector.getField().getConditionParentType(),
                                  interval);
                }
            }
        }

        return intervals;
    }

    private boolean areIntervalsOverlapping(final Interval first,
                                            final Interval second) {
        final Bound firstLowerBound = first.getLowerBound();
        final Bound secondUpperBound = second.getUpperBound();
        return firstLowerBound.compareTo(secondUpperBound) < 0;
    }
}

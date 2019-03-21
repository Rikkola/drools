/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
package org.drools.verifier.core.checks.gaps;

import java.util.Collection;
import java.util.stream.Collectors;

import org.drools.verifier.api.reporting.CheckType;
import org.drools.verifier.api.reporting.Issue;
import org.drools.verifier.api.reporting.Severity;
import org.drools.verifier.api.reporting.gaps.MissingRange;
import org.drools.verifier.api.reporting.gaps.MissingRangeIssue;

public class RangeError {

    private final PartitionKey partitionKey;
    private final Collection<MissingRange> uncoveredRanges;

    public RangeError(final PartitionKey partitionKey,
                      final Collection<MissingRange> uncoveredRanges) {
        this.partitionKey = partitionKey;
        this.uncoveredRanges = uncoveredRanges;
    }

    public Issue toIssue(final Severity severity,
                         final CheckType checkType) {
        return new MissingRangeIssue(severity,
                                     checkType,
                                     partitionKey.getConditions(),
                                     uncoveredRanges
        ).setDebugMessage(getMessage());
    }

    private String getMessage() {
        return "Uncovered range found " + toString();
    }

    @Override
    public String toString() {
        return "RangeError{" +
                ", uncoveredRanges=" + uncoveredRangesToString() +
                '}';
    }

    private String uncoveredRangesToString() {
        return uncoveredRanges.stream()
                .map(r -> r.toString())
                .collect(Collectors.joining(", "));
    }
}

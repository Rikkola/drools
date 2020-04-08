/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.validation.dtanalysis;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.DMNMessageType;
import org.kie.dmn.feel.runtime.Range.RangeBoundary;
import org.kie.dmn.validation.dtanalysis.model.Bound;
import org.kie.dmn.validation.dtanalysis.model.DTAnalysis;
import org.kie.dmn.validation.dtanalysis.model.Hyperrectangle;
import org.kie.dmn.validation.dtanalysis.model.Interval;
import org.kie.dmn.validation.dtanalysis.model.Overlap;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.kie.dmn.validation.DMNValidator.Validation.ANALYZE_DECISION_TABLE;
import static org.kie.dmn.validation.DMNValidator.Validation.VALIDATE_COMPILATION;
import static org.kie.dmn.validation.DMNValidator.Validation.VALIDATE_MODEL;
import static org.kie.dmn.validation.dtanalysis.utils.IssueCounter.collect;
import static org.kie.dmn.validation.dtanalysis.utils.IssueCounter.collectOverlaps;

public class StringWithoutEnumNoGapTest extends AbstractDTAnalysisTest {

    @Test
    public void test() {
        List<DMNMessage> validate = validator.validate(getReader("stringWithoutEnumNoGap.dmn"), VALIDATE_COMPILATION, VALIDATE_MODEL, ANALYZE_DECISION_TABLE);
        assertThat(validate, hasSize(7)); // no gap, 2 overlaps, 2 masked, 2 misleading.
        debugValidatorMsg(validate);

        DTAnalysis analysis = getAnalysis(validate, "_8b48d1c9-265c-47aa-9378-7f11d55dfe55");

        assertThat(analysis.getGaps(), hasSize(0));

        // assert OVERLAPs count.
        assertThat(collectOverlaps(analysis), hasSize(2));

        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Overlap> overlaps = Arrays.asList(new Overlap(Arrays.asList(1,
                                                                         3),
                                                           new Hyperrectangle(2,
                                                                              Arrays.asList(Interval.newFromBounds(new Bound("EU",
                                                                                                                             RangeBoundary.CLOSED,
                                                                                                                             null),
                                                                                                                   new Bound("EU",
                                                                                                                             RangeBoundary.CLOSED,
                                                                                                                             null)),
                                                                                            Interval.newFromBounds(new Bound(new BigDecimal("18"),
                                                                                                                             RangeBoundary.CLOSED,
                                                                                                                             null),
                                                                                                                   new Bound(Interval.POS_INF,
                                                                                                                             RangeBoundary.CLOSED,
                                                                                                                             null))))),
                                               new Overlap(Arrays.asList(3,
                                                                         2),
                                                           new Hyperrectangle(2,
                                                                              Arrays.asList(Interval.newFromBounds(new Bound("US",
                                                                                                                             RangeBoundary.CLOSED,
                                                                                                                             null),
                                                                                                                   new Bound("US",
                                                                                                                             RangeBoundary.CLOSED,
                                                                                                                             null)),
                                                                                            Interval.newFromBounds(new Bound(new BigDecimal("21"),
                                                                                                                             RangeBoundary.CLOSED,
                                                                                                                             null),
                                                                                                                   new Bound(Interval.POS_INF,
                                                                                                                             RangeBoundary.CLOSED,
                                                                                                                             null))))));
        assertThat(overlaps, hasSize(2));

        // Assert OVERLAPs same values
        assertThat(analysis.getOverlaps(), contains(overlaps.toArray()));

        // MaskedRules count.
        List<DMNMessage> foundMaskedRules = collect(DMNMessageType.DECISION_TABLE_MASKED_RULE, analysis);
        List<String> foundMaskedRuleStrings = foundMaskedRules.stream().map(x -> x.getText()).collect(Collectors.toList());
        assertThat(foundMaskedRuleStrings, hasSize(2));
//        List<String> maskedRules = Arrays.asList(
//                "DMN: Rule 3 is masked by rule: 1 (DMN id: _8b48d1c9-265c-47aa-9378-7f11d55dfe55, DMN Validation, Decision Table Analysis, Masked Rule Analysis) ",
//                "DMN: Rule 3 is masked by rule: 2 (DMN id: _8b48d1c9-265c-47aa-9378-7f11d55dfe55, DMN Validation, Decision Table Analysis, Masked Rule Analysis) "
//        );
//        assertThat(foundMaskedRuleStrings, contains(maskedRules.toArray()));

        // MisleadingRules count.
        List<DMNMessage> foundMisleadingRules = collect(DMNMessageType.DECISION_TABLE_MISLEADING_RULE, analysis);
        List<String> foundMisleadingRuleStrings = foundMisleadingRules.stream().map(x -> x.getText()).collect(Collectors.toList());
        assertThat(foundMisleadingRuleStrings, hasSize(2));
//        List<String> misleadingRules = Arrays.asList(
//                "DMN: Rule 3 is a misleading rule. It could be misleading over other rules, such as rule: 2 (DMN id: _8b48d1c9-265c-47aa-9378-7f11d55dfe55, DMN Validation, Decision Table Analysis, Misleading Rule Analysis) ",
//                "DMN: Rule 3 is a misleading rule. It could be misleading over other rules, such as rule: 1 (DMN id: _8b48d1c9-265c-47aa-9378-7f11d55dfe55, DMN Validation, Decision Table Analysis, Misleading Rule Analysis) "
//        );
//        assertThat(foundMisleadingRuleStrings, contains(misleadingRules.toArray()));
    }
}

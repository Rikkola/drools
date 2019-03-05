/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

import org.junit.Test;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.feel.runtime.Range.RangeBoundary;
import org.kie.dmn.validation.ValidatorUtil;
import org.kie.dmn.validation.dtanalysis.model.Bound;
import org.kie.dmn.validation.dtanalysis.model.DMNDTAnalysisMessage;
import org.kie.dmn.validation.dtanalysis.model.DTAnalysis;
import org.kie.dmn.validation.dtanalysis.model.Hyperrectangle;
import org.kie.dmn.validation.dtanalysis.model.Interval;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.kie.dmn.validation.DMNValidator.Validation.DECISION_TABLE_ANALYSIS;
import static org.kie.dmn.validation.DMNValidator.Validation.VALIDATE_COMPILATION;

public class AK0100v2_domainOnTable_Test extends AbstractDTAnalysisTest {

    @Test
    public void test() {
        List<DMNMessage> validate = validator.validate(getReader("AK0100v2-domainOnTable.dmn"), VALIDATE_COMPILATION, DECISION_TABLE_ANALYSIS);
        assertThat(ValidatorUtil.formatMessages(validate), validate.size(), is(1));

        DMNMessage dmnMessage = validate.get(0);
        assertThat(dmnMessage, is(instanceOf(DMNDTAnalysisMessage.class)));

        DMNDTAnalysisMessage dtMsg = (DMNDTAnalysisMessage) dmnMessage;
        DTAnalysis analysis = dtMsg.getAnalysis();
        assertThat(analysis.getGaps(), hasSize(2));

        debugAnalysis(analysis);
        
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Hyperrectangle> gaps = Arrays.asList(new Hyperrectangle(2,
                                                                     Arrays.asList(Interval.newFromBounds(new Bound(new BigDecimal("0"),
                                                                                                                    RangeBoundary.CLOSED,
                                                                                                                    null),
                                                                                                          new Bound(new BigDecimal("12"),
                                                                                                                    RangeBoundary.OPEN,
                                                                                                                    null)))),
                                                  new Hyperrectangle(2,
                                                                     Arrays.asList(Interval.newFromBounds(new Bound(new BigDecimal("15"),
                                                                                                                    RangeBoundary.CLOSED,
                                                                                                                    null),
                                                                                                          new Bound(new BigDecimal("18"),
                                                                                                                    RangeBoundary.OPEN,
                                                                                                                    null)),
                                                                                   Interval.newFromBounds(new Bound(new BigDecimal("100"),
                                                                                                                    RangeBoundary.CLOSED,
                                                                                                                    null),
                                                                                                          new Bound(new BigDecimal("100"),
                                                                                                                    RangeBoundary.CLOSED,
                                                                                                                    null)))));
        assertThat(gaps, hasSize(2));

        // Assert GAPS
        assertThat(analysis.getGaps(), contains(gaps.toArray()));
    }


}
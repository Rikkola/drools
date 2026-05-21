/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.modelcompiler;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.within;
import org.drools.model.Variable;
import org.drools.modelcompiler.domain.SensorEvents.OperatorAcknowledged;

public class PatternDSLSequenceWithinTest {

    private final Variable<OperatorAcknowledged> ack = declarationOf(OperatorAcknowledged.class);

    @Test
    public void withinRejectsNullTimeout() {
        assertThatThrownBy(() -> within(null, pattern(ack)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive timeout");
    }

    @Test
    public void withinRejectsZeroTimeout() {
        assertThatThrownBy(() -> within(Duration.ZERO, pattern(ack)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive timeout");
    }

    @Test
    public void withinRejectsNegativeTimeout() {
        assertThatThrownBy(() -> within(Duration.ofSeconds(-1), pattern(ack)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive timeout");
    }
}

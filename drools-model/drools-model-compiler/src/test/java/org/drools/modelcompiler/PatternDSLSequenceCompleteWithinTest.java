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
import java.util.ArrayList;
import java.util.List;

import org.drools.model.PatternDSL.SequenceViewItem;
import org.drools.model.Rule;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.domain.SensorEvents.CalibrationPassed;
import org.drools.modelcompiler.domain.SensorEvents.HeartbeatOk;
import org.drools.modelcompiler.domain.SensorEvents.MonitoringStation;
import org.drools.modelcompiler.domain.SensorEvents.OperatorAcknowledged;
import org.drools.modelcompiler.domain.SensorEvents.SensorActivated;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.or;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;
import static org.drools.model.PatternDSL.within;

public class PatternDSLSequenceCompleteWithinTest {

    private final Variable<MonitoringStation>    station         = declarationOf(MonitoringStation.class);
    private final Variable<SensorActivated>      sensorActivated = declarationOf(SensorActivated.class);
    private final Variable<HeartbeatOk>          heartbeat       = declarationOf(HeartbeatOk.class);
    private final Variable<CalibrationPassed>    calibration     = declarationOf(CalibrationPassed.class);
    private final Variable<OperatorAcknowledged> ack             = declarationOf(OperatorAcknowledged.class);

    private KieSession ksession;

    @AfterEach
    public void tearDown() {
        if (ksession != null) {
            ksession.dispose();
        }
    }

    private void insertAndFire(Object... facts) {
        for (Object fact : facts) {
            ksession.insert(fact);
        }
        ksession.fireAllRules();
    }

    // ---- validation (no KieBase needed) ----

    @Test
    public void completeWithinRejectsNullDuration() {
        assertThatThrownBy(() -> sequence(pattern(sensorActivated)).completeWithin(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void completeWithinRejectsZeroDuration() {
        assertThatThrownBy(() -> sequence(pattern(sensorActivated)).completeWithin(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void completeWithinRejectsNegativeDuration() {
        assertThatThrownBy(() -> sequence(pattern(sensorActivated)).completeWithin(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    public void completeWithinRejectsDoubleSet() {
        assertThatThrownBy(() -> sequence(pattern(sensorActivated))
                        .completeWithin(Duration.ofSeconds(10))
                        .completeWithin(Duration.ofSeconds(20)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already set");
    }

    @Test
    public void completeWithinStoresDeadlineMillis() {
        SequenceViewItem sv = sequence(pattern(sensorActivated)).completeWithin(Duration.ofMinutes(10));
        assertThat(sv.getDeadlineMillis()).isEqualTo(600_000L);
    }

    // ---- top-level behaviour ----

    @Test
    public void sequenceFiresWhenItCompletesBeforeTheDeadline() {
        final List<String> results = new ArrayList<>();

        Rule r = rule("complete-within-met").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        pattern(heartbeat).expr("hb", h -> h.getSensorId().equals("sensor-1")),
                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                ).completeWithin(Duration.ofSeconds(30)),
                execute(() -> results.add("done"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // step 0 → sequence starts, 30s deadline scheduled
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(10)); // still inside the window
        assertThat(results).isEmpty(); // whole-sequence timer must NOT abort a still-progressing sequence
        insertAndFire(new HeartbeatOk("sensor-1"));          // step 1
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // step 2 → final → fire

        assertThat(results).containsExactly("done");
    }

    @Test
    public void sequenceAbortsWhenTheWholeSequenceDeadlineExpires() {
        final List<String> results = new ArrayList<>();

        Rule r = rule("complete-within-expired").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        pattern(heartbeat).expr("hb", h -> h.getSensorId().equals("sensor-1")),
                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                ).completeWithin(Duration.ofSeconds(30)),
                execute(() -> results.add("done"))
        );

        KieBase kbase = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r));
        ksession = TemporalSequenceTestHarness.newPseudoClockSession(kbase);

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));      // sequence starts, 30s deadline scheduled
        insertAndFire(new HeartbeatOk("sensor-1"));          // step 1 reached, but step 2 stalls
        TemporalSequenceTestHarness.advance(ksession, Duration.ofSeconds(31)); // blow past the whole-sequence deadline → abort
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // too late — sequence already aborted

        assertThat(results).isEmpty();
    }
}

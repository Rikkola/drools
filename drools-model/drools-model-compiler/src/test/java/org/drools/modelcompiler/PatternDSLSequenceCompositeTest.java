package org.drools.modelcompiler;

import java.util.ArrayList;
import java.util.List;

import org.drools.model.Model;
import org.drools.model.Rule;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.domain.SensorEvents.AlarmRaised;
import org.drools.modelcompiler.domain.SensorEvents.CalibrationPassed;
import org.drools.modelcompiler.domain.SensorEvents.HeartbeatOk;
import org.drools.modelcompiler.domain.SensorEvents.MonitoringStation;
import org.drools.modelcompiler.domain.SensorEvents.OperatorAcknowledged;
import org.drools.modelcompiler.domain.SensorEvents.SensorActivated;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.and;
import static org.drools.model.PatternDSL.nor;
import static org.drools.model.PatternDSL.or;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;

public class PatternDSLSequenceCompositeTest {

    private final Variable<MonitoringStation>     station         = declarationOf(MonitoringStation.class);
    private final Variable<SensorActivated>       sensorActivated = declarationOf(SensorActivated.class);
    private final Variable<HeartbeatOk>           heartbeat       = declarationOf(HeartbeatOk.class);
    private final Variable<AlarmRaised>           alarm           = declarationOf(AlarmRaised.class);
    private final Variable<CalibrationPassed>     calibration     = declarationOf(CalibrationPassed.class);
    private final Variable<OperatorAcknowledged>  ack             = declarationOf(OperatorAcknowledged.class);

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

    @Test
    public void sequenceRejectsBareNorAsStep() {
        Rule r =
                rule("nor-rejected").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                nor(pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1"))),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> { })
                );

        Model model = new ModelImpl().addRule(r);

        assertThatThrownBy(() -> KieBaseBuilder.createKieBaseFromModel(model))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support nor(...) as a top-level step")
                .hasMessageContaining("ADR 0001");
    }

    @Test
    public void sequenceFiresWithOrOfTwo() {
        // Step 1 is or(heartbeat, alarm). Either signal is enough to advance.
        final List<String> results = new ArrayList<>();

        Rule orRule =
                rule("or-rule").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                or(
                                        pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                        pattern(alarm).expr("hi", al -> al.getSeverity().equals("high"))
                                ),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(orRule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));            // OR child #1 fires → step advances
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceFiresWithAndOfTwo() {
        // Step 1 is and(heartbeat, alarm). Both signals must arrive before step advances.
        final List<String> results = new ArrayList<>();

        Rule andRule =
                rule("and-rule").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                        pattern(alarm).expr("hi", al -> al.getSeverity().equals("high"))
                                ),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(andRule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));               // AND child #1 fires → step still waiting
        insertAndFire(new AlarmRaised("sensor-1", "high"));       // AND child #2 fires → step advances
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceFiresWithNestedAndInsideOr() {
        // Step 1 is or(heartbeat, and(calibration, alarm)).
        // Either branch suffices: a heartbeat alone, or calibration + alarm together.
        final List<String> results = new ArrayList<>();

        Rule nestedRule =
                rule("nested-rule").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                or(
                                        pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                        and(
                                                pattern(calibration).expr("calibrated", c -> c.getSensorId().equals("sensor-1")),
                                                pattern(alarm).expr("hi", al -> al.getSeverity().equals("high"))
                                        )
                                ),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(nestedRule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        // The AND branch matches: calibration + alarm together, no heartbeat needed.
        insertAndFire(new CalibrationPassed("sensor-1"));
        insertAndFire(new AlarmRaised("sensor-1", "high"));
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceDoesNotFireWhenAndPartial() {
        // Same shape as sequenceFiresWithAndOfTwo, but only one of the AND children fires.
        // AND requires every child matched; one match is not enough; step never advances.
        final List<String> results = new ArrayList<>();

        Rule andRule =
                rule("and-partial-rule").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                        pattern(alarm).expr("hi", al -> al.getSeverity().equals("high"))
                                ),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(andRule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));               // AND child #1 fires; child #2 never does.
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));   // Step 1 still active; ack is ignored.

        assertThat(results).isEmpty();
    }
}

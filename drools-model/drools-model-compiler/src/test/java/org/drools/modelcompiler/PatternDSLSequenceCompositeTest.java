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
import static org.drools.model.PatternDSL.nand;
import static org.drools.model.PatternDSL.nor;
import static org.drools.model.PatternDSL.not;
import static org.drools.model.PatternDSL.or;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.xor;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;

public class PatternDSLSequenceCompositeTest {

    private final Variable<MonitoringStation>     station         = declarationOf(MonitoringStation.class);
    private final Variable<SensorActivated>       sensorActivated = declarationOf(SensorActivated.class);
    private final Variable<HeartbeatOk>           heartbeat       = declarationOf(HeartbeatOk.class);
    private final Variable<AlarmRaised>           alarm           = declarationOf(AlarmRaised.class);
    private final Variable<AlarmRaised>           alarmA          = declarationOf(AlarmRaised.class);
    private final Variable<AlarmRaised>           alarmB          = declarationOf(AlarmRaised.class);
    private final Variable<AlarmRaised>           alarmC          = declarationOf(AlarmRaised.class);
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
                .hasMessageContaining("does not support absence-based steps at the top level")
                .hasMessageContaining("ADR 0001");
    }

    @Test
    public void sequenceRejectsBareNotAsStep() {
        Rule r =
                rule("not-rejected").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                not(pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1"))),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> { })
                );

        Model model = new ModelImpl().addRule(r);

        assertThatThrownBy(() -> KieBaseBuilder.createKieBaseFromModel(model))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support absence-based steps at the top level")
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

    @Test
    public void sequenceFiresWhenNoAlarmBeforeAck() {
        // Step 1 is and(nor(alarm), ack). The NOR child is vacuously matched
        // at activation; ack arriving with no preceding alarm satisfies the AND.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("no-alarm-before-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        nor(pattern(alarm).expr("alarm", al -> al.getSeverity().equals("high"))),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                )
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceDoesNotFireWhenAlarmRaisedBeforeAck() {
        // Step 1 is and(nor(alarm), ack). An alarm arriving in the gap
        // flips the NOR child to UNMATCHED via status rollback; the AND
        // never reaches MATCHED; the step never advances; the rule does
        // not fire.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("alarm-vetoes-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        nor(pattern(alarm).expr("alarm", al -> al.getSeverity().equals("high"))),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                )
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new AlarmRaised("sensor-1", "high"));   // veto
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).isEmpty();
    }

    @Test
    public void sequenceFiresEvenWhenAlarmArrivesAfterAck() {
        // Documents the gap-spanning limitation: the runtime has no mechanism
        // to roll back step advancement, so an alarm arriving AFTER ack (which
        // closes the NOR window inside the AND) cannot veto. If a future
        // change adds CEP-style watermark semantics, this test must flip to
        // assertThat(results).isEmpty(). See ADR 0001.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("alarm-after-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        nor(pattern(alarm).expr("alarm", al -> al.getSeverity().equals("high"))),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                )
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));   // step advances here
        insertAndFire(new AlarmRaised("sensor-1", "high"));             // too late — NOR not listening

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceDoesNotFireWhenAlarmRaisedBeforeAckUsingNot() {
        // Mirrors sequenceDoesNotFireWhenAlarmRaisedBeforeAck but uses
        // not(alarm) instead of nor(alarm). Proves PatternDSL.not(...)
        // aliases to single-child nor(...) at the runtime layer, not just
        // the AST layer. The veto chain (vacuousMatchAtActivation /
        // statusCanRevert split) is exercised end-to-end.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("alarm-vetoes-ack-using-not").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        not(pattern(alarm).expr("alarm", al -> al.getSeverity().equals("high"))),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                )
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new AlarmRaised("sensor-1", "high"));   // veto
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).isEmpty();
    }

    @Test
    public void sequenceFiresWhenNeitherEventArrivesBeforeAck() {
        // and(nand(heartbeat, alarm), ack) — neither event inserted →
        // NAND is vacuously matched at activation, ack closes the AND,
        // rule fires.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("neither-event-before-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        nand(
                                                pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                                pattern(alarm).expr("hi", al -> al.getSeverity().equals("high"))
                                        ),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                )
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceFiresWhenOnlyOneEventArrivesBeforeAck() {
        // and(nand(heartbeat, alarm), ack) — only the heartbeat arrives
        // before ack → NAND stays matched (not all children fired) → AND
        // fires when ack arrives. Proves NAND's rollback triggers only
        // when EVERY child has fired, not on any child.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("only-one-event-before-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        nand(
                                                pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                                pattern(alarm).expr("hi", al -> al.getSeverity().equals("high"))
                                        ),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                )
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));                       // one NAND child fires, NAND still matched
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceDoesNotFireWhenBothEventsArriveBeforeAck() {
        // and(nand(heartbeat, alarm), ack) — both NAND children arrive
        // before ack → NAND rolls back to UNMATCHED → AND vetoes → step
        // does not advance → rule does not fire.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("both-events-before-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        nand(
                                                pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                                pattern(alarm).expr("hi", al -> al.getSeverity().equals("high"))
                                        ),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                )
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));                       // first NAND child fires
        insertAndFire(new AlarmRaised("sensor-1", "high"));               // second NAND child fires → veto
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).isEmpty();
    }

    @Test
    public void sequenceRejectsBareNandAsStep() {
        Rule r =
                rule("nand-rejected").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                nand(
                                        pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                        pattern(alarm).expr("hi", al -> al.getSeverity().equals("high"))
                                ),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> { })
                );

        Model model = new ModelImpl().addRule(r);

        assertThatThrownBy(() -> KieBaseBuilder.createKieBaseFromModel(model))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support absence-based steps at the top level")
                .hasMessageContaining("ADR 0001");
    }

    @Test
    public void sequenceFiresWhenExactlyOneAlarmBeforeAck() {
        // and(xor(alarmA, alarmB, alarmC), ack) — exactly one of three alarms
        // arrives before ack → XOR matched (one bit set in a&allMatched) → AND
        // fires when ack arrives. Drives XOR wiring through predicateFor and
        // statusCanRevertFor.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("xor-one-alarm-before-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        xor(
                                                pattern(alarmA).expr("a", al -> al.getSensorId().equals("sensor-A")),
                                                pattern(alarmB).expr("b", al -> al.getSensorId().equals("sensor-B")),
                                                pattern(alarmC).expr("c", al -> al.getSensorId().equals("sensor-C"))
                                        ),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                )
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new AlarmRaised("sensor-A", "high"));               // exactly one XOR child fires → MATCHED
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceDoesNotFireWhenNoAlarmsBeforeAck() {
        // and(xor(alarmA, alarmB, alarmC), ack) — no alarms arrive before ack.
        // XOR is predicate-false at zero (xor(0, allMatched) = false) and
        // vacuousMatchAtActivation=false, so XOR stays UNMATCHED → AND never
        // reaches all-bits-set → rule does not fire. Mirror-inverts NAND's
        // sequenceFiresWhenNoAlarmsBeforeAck.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("xor-no-alarms-before-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        xor(
                                                pattern(alarmA).expr("a", al -> al.getSensorId().equals("sensor-A")),
                                                pattern(alarmB).expr("b", al -> al.getSensorId().equals("sensor-B")),
                                                pattern(alarmC).expr("c", al -> al.getSensorId().equals("sensor-C"))
                                        ),
                                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                )
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(rule)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).isEmpty();
    }
}

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
import static org.drools.model.PatternDSL.never;
import static org.drools.model.PatternDSL.or;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;
import static org.drools.model.PatternDSL.xnor;
import static org.drools.model.PatternDSL.xor;

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
                .hasMessageContaining("does not support self-reverting steps at the top level")
                .hasMessageContaining("ADR 0001");
    }

    @Test
    public void sequenceRejectsBareNeverAsStep() {
        Rule r =
                rule("never-rejected").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                never(pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1"))),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> { })
                );

        Model model = new ModelImpl().addRule(r);

        assertThatThrownBy(() -> KieBaseBuilder.createKieBaseFromModel(model))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support self-reverting steps at the top level")
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
    public void sequenceDoesNotFireWhenAlarmRaisedBeforeAckUsingNever() {
        // Mirrors sequenceDoesNotFireWhenAlarmRaisedBeforeAck but uses
        // never(alarm) instead of nor(alarm). Proves PatternDSL.never(...)
        // aliases to single-child nor(...) at the runtime layer, not just
        // the AST layer. The veto chain (vacuousMatchAtActivation /
        // statusCanRevert split) is exercised end-to-end.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("alarm-vetoes-ack-using-never").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        never(pattern(alarm).expr("alarm", al -> al.getSeverity().equals("high"))),
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
                .hasMessageContaining("does not support self-reverting steps at the top level")
                .hasMessageContaining("ADR 0001");
    }

    @Test
    public void sequenceRejectsBareXorAsStep() {
        Rule r =
                rule("xor-rejected").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                xor(
                                        pattern(alarmA).expr("a", al -> al.getSensorId().equals("sensor-A")),
                                        pattern(alarmB).expr("b", al -> al.getSensorId().equals("sensor-B")),
                                        pattern(alarmC).expr("c", al -> al.getSensorId().equals("sensor-C"))
                                ),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> { })
                );

        Model model = new ModelImpl().addRule(r);

        assertThatThrownBy(() -> KieBaseBuilder.createKieBaseFromModel(model))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support self-reverting steps at the top level")
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

    @Test
    public void sequenceDoesNotFireWhenTwoAlarmsBeforeAck() {
        // and(xor(alarmA, alarmB, alarmC), ack) — two of three alarms arrive
        // before ack. XOR matches on the first arrival (one bit set), then
        // statusCanRevert=true rolls it back to UNMATCHED on the second
        // (two bits set, predicate false). AND bit 1 clears → step does not
        // advance → rule does not fire.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("xor-two-alarms-before-ack").build(
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
        insertAndFire(new AlarmRaised("sensor-A", "high"));               // XOR → MATCHED
        insertAndFire(new AlarmRaised("sensor-B", "high"));               // XOR → UNMATCHED (rollback)
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).isEmpty();
    }

    @Test
    public void sequenceDoesNotFireWhenAllThreeAlarmsBeforeAck() {
        // and(xor(alarmA, alarmB, alarmC), ack) — all three alarms arrive
        // before ack. XOR matches on the first, reverts on the second, stays
        // UNMATCHED on the third (three bits set, predicate false, prior
        // already UNMATCHED so no propagate). AND bit 1 stays cleared.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("xor-three-alarms-before-ack").build(
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
        insertAndFire(new AlarmRaised("sensor-A", "high"));               // XOR → MATCHED
        insertAndFire(new AlarmRaised("sensor-B", "high"));               // XOR → UNMATCHED (rollback)
        insertAndFire(new AlarmRaised("sensor-C", "high"));               // XOR → UNMATCHED (stays)
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).isEmpty();
    }

    @Test
    public void sequenceFiresWhenNoAlarmsBeforeAck() {
        // and(xnor(alarmA, alarmB, alarmC), ack) — no alarms before ack.
        // XNOR is vacuousMatchAtActivation=true (predicate.test(0, allMatched)
        // = (0 == 0) → true), so XNOR is MATCHED at activation and AND fires
        // once ack arrives. Drives XNOR wiring through predicateFor and
        // vacuousMatchAtActivationFor. Distinguishes XNOR from XOR
        // (XOR is predicate-false at zero and would not fire here).
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("xnor-no-alarms-before-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        xnor(
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
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));     // no alarms, just ack

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceDoesNotFireWhenOneAlarmBeforeAck() {
        // and(xnor(alarmA, alarmB, alarmC), ack) — one alarm before ack.
        // XNOR starts MATCHED (vacuous-true), then alarmA arrives:
        // predicate.test(0b001, 0b111) = (0b001 != 0 && 0b001 != 0b111)
        // = false. statusCanRevert=true → MATCHED → UNMATCHED → AND bit 1
        // clears → AND never reaches all-bits-set → rule does not fire.
        // Drives statusCanRevertFor(XNOR)=true.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("xnor-one-alarm-before-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        xnor(
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
        insertAndFire(new AlarmRaised("sensor-A", "high"));               // one alarm — XNOR should roll back to UNMATCHED
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).isEmpty();
    }

    @Test
    public void sequenceDoesNotFireWhenTwoAlarmsBeforeAckXnor() {
        // and(xnor(alarmA, alarmB, alarmC), ack) — two alarms before ack.
        // After Task 2, XNOR rolls back to UNMATCHED on the first alarm.
        // alarmB arrives next: predicate.test(0b011, 0b111) =
        // (0b011 != 0 && 0b011 != 0b111) = false → status stays UNMATCHED
        // (no transition, no re-propagate). Rule does not fire.
        // Pins all-or-none vs even-count: a naive "no odd bits" XNOR
        // (NXOR) would match on popcount=2 and flip this test.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("xnor-two-alarms-before-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        xnor(
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
        insertAndFire(new AlarmRaised("sensor-A", "high"));
        insertAndFire(new AlarmRaised("sensor-B", "high"));               // two alarms — even count, but XNOR stays UNMATCHED
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).isEmpty();
    }

    @Test
    public void sequenceFiresWhenAllThreeAlarmsBeforeAck() {
        // and(xnor(alarmA, alarmB, alarmC), ack) — all three alarms before
        // ack. After two alarms XNOR is UNMATCHED (Task 3). alarmC arrives:
        // predicate.test(0b111, 0b111) = (0b111 != 0 && 0b111 == 0b111)
        // = true. statusCanRevert=true → UNMATCHED → MATCHED → AND bit 1
        // set → AND reaches 0b11 on ack → rule fires.
        // Pins the "all" half of all-or-none. Distinguishes XNOR from NAND:
        // NAND stays vetoed on all-bits-set (predicate a != b is false);
        // XNOR re-matches.
        final List<String> results = new ArrayList<>();

        Rule rule =
                rule("xnor-all-three-alarms-before-ack").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                and(
                                        xnor(
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
        insertAndFire(new AlarmRaised("sensor-A", "high"));
        insertAndFire(new AlarmRaised("sensor-B", "high"));
        insertAndFire(new AlarmRaised("sensor-C", "high"));               // all three — XNOR re-matches on all-bits-set
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void sequenceRejectsBareXnorAsStep() {
        Rule r =
                rule("xnor-rejected").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                xnor(
                                        pattern(alarmA).expr("a", al -> al.getSensorId().equals("sensor-A")),
                                        pattern(alarmB).expr("b", al -> al.getSensorId().equals("sensor-B"))
                                ),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> { })
                );

        Model model = new ModelImpl().addRule(r);

        assertThatThrownBy(() -> KieBaseBuilder.createKieBaseFromModel(model))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support self-reverting steps at the top level")
                .hasMessageContaining("ADR 0001");
    }
}

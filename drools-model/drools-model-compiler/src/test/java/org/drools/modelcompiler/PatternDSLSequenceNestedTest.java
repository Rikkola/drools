package org.drools.modelcompiler;

import java.util.ArrayList;
import java.util.List;

import org.drools.model.Model;
import org.drools.model.Rule;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.domain.SensorEvents.AlarmRaised;
import org.drools.modelcompiler.domain.SensorEvents.HeartbeatOk;
import org.drools.modelcompiler.domain.SensorEvents.MonitoringStation;
import org.drools.modelcompiler.domain.SensorEvents.OperatorAcknowledged;
import org.drools.modelcompiler.domain.SensorEvents.SensorActivated;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;

public class PatternDSLSequenceNestedTest {

    private final Variable<MonitoringStation>     station         = declarationOf(MonitoringStation.class);
    private final Variable<SensorActivated>       sensorActivated = declarationOf(SensorActivated.class);
    private final Variable<HeartbeatOk>           heartbeat       = declarationOf(HeartbeatOk.class);
    private final Variable<AlarmRaised>           alarm           = declarationOf(AlarmRaised.class);
    private final Variable<OperatorAcknowledged>  ack             = declarationOf(OperatorAcknowledged.class);

    private KieSession ksession;

    @AfterEach
    public void dispose() {
        if (ksession != null) {
            ksession.dispose();
        }
    }

    private void insertAndFire(Object fact) {
        ksession.insert(fact);
        ksession.fireAllRules();
    }

    @Test
    public void nestedSequenceFiresInOrder() {
        // sequence(anchor, sequence(heartbeat, alarm), ack):
        // after the anchor, the nested sequence must run heartbeat THEN alarm, then ack fires the rule.
        final List<String> results = new ArrayList<>();

        Rule r =
                rule("nested-ordered").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                sequence(
                                        pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                        pattern(alarm).expr("hi", al -> al.getSeverity().equals("high"))
                                ),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        Model model = new ModelImpl().addRule(r);
        ksession = KieBaseBuilder.createKieBaseFromModel(model).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));                       // nested step 1
        insertAndFire(new AlarmRaised("sensor-1", "high"));              // nested step 2 → nested seq completes
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));    // parent advances → fires

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void nestedSequenceDoesNotFireWhenInnerRunIncomplete() {
        // Nested sequence(heartbeat, alarm): if alarm never arrives, the nested run never
        // completes, so the parent never advances to ack and the rule never fires.
        final List<String> results = new ArrayList<>();

        Rule r =
                rule("nested-incomplete").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                sequence(
                                        pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                        pattern(alarm).expr("hi", al -> al.getSeverity().equals("high"))
                                ),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));                       // nested step 1 only; no alarm
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));    // ack arrives but parent still inside nested run

        assertThat(results).isEmpty();
    }

    @Test
    public void nestedSequenceWorksAtDepthThree() {
        // sequence(anchor, sequence(heartbeat, sequence(alarm, ack))) — three levels deep.
        final List<String> results = new ArrayList<>();

        Rule r =
                rule("nested-depth-3").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                sequence(
                                        pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                        sequence(
                                                pattern(alarm).expr("hi", al -> al.getSeverity().equals("high")),
                                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                                        )
                                )
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(r)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));
        insertAndFire(new AlarmRaised("sensor-1", "high"));
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void nestedSequenceMatchesFlatSequenceOnInOrderTrace() {
        // sequence(A, sequence(B,C), D) and a flat sequence(A,B,C,D) fire identically
        // on a strictly in-order trace. (They diverge only where scoping to the nested
        // run matters — out of scope for Stage 1.)
        final List<String> results = new ArrayList<>();

        Rule flat =
                rule("flat").build(
                        pattern(station),
                        sequence(
                                pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                                pattern(heartbeat).expr("ok", h -> h.getSensorId().equals("sensor-1")),
                                pattern(alarm).expr("hi", al -> al.getSeverity().equals("high")),
                                pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                        ),
                        execute(() -> results.add("acknowledged"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(flat)).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));
        insertAndFire(new AlarmRaised("sensor-1", "high"));
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));

        assertThat(results).containsExactly("acknowledged");
    }
}

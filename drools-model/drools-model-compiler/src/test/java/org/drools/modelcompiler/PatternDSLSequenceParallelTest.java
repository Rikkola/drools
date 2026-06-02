package org.drools.modelcompiler;

import java.util.ArrayList;
import java.util.List;

import org.drools.model.Model;
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
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.and;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;

public class PatternDSLSequenceParallelTest {

    // Compile-only negatives (do NOT uncomment — they must fail to compile, which
    // is the type-system enforcement of the "reject mixed / no top-level parallel" rules):
    //   and(pattern(sensorActivated), sequence(pattern(heartbeat)))  // mixed → neither overload applies
    //   rule("x").build(and(sequence(pattern(heartbeat)), sequence(pattern(calibration))), execute(() -> {}))
    //                                                                // top-level bare parallel → ParallelViewItem is not a top-level builder

    private final Variable<MonitoringStation>     station         = declarationOf(MonitoringStation.class);
    private final Variable<SensorActivated>       sensorActivated = declarationOf(SensorActivated.class);
    private final Variable<HeartbeatOk>           heartbeat       = declarationOf(HeartbeatOk.class);
    private final Variable<CalibrationPassed>     calibration     = declarationOf(CalibrationPassed.class);
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

    // sequence(anchor, and(sequence(heartbeat), sequence(calibration)), ack):
    // after the anchor, BOTH parallel branches must complete before the parent
    // advances to ack and the rule fires.
    private Rule buildParallelRule(List<String> results) {
        return rule("parallel-join").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        and(
                                sequence(pattern(heartbeat).expr("hb", h -> h.getSensorId().equals("sensor-1"))),
                                sequence(pattern(calibration).expr("cal", c -> c.getSensorId().equals("sensor-1")))
                        ),
                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                ),
                execute(() -> results.add("acknowledged"))
        );
    }

    @Test
    public void parallelBranchesJoinBeforeParentAdvances() {
        final List<String> results = new ArrayList<>();
        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(buildParallelRule(results))).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));                    // branch 1 completes
        assertThat(results).isEmpty();                                 // join NOT satisfied — calibration still pending
        insertAndFire(new CalibrationPassed("sensor-1"));             // branch 2 completes → join fires → parent advances to ack
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // ack step matches → root sequence ends → fire
        assertThat(results).containsExactly("acknowledged");
    }
}

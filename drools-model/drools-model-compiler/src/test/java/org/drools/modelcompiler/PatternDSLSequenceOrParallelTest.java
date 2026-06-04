package org.drools.modelcompiler;

import java.time.Duration;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.or;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;
import static org.drools.model.PatternDSL.within;

public class PatternDSLSequenceOrParallelTest {

    // Compile-only negatives (do NOT uncomment — they must fail to compile, which
    // is the type-system enforcement of the "reject mixed / no top-level parallel" rules):
    //   or(pattern(sensorActivated), sequence(pattern(heartbeat)))  // mixed → neither overload applies
    //   rule("x").build(or(sequence(pattern(heartbeat)), sequence(pattern(calibration))), execute(() -> {}))
    //                                                               // top-level bare or-parallel → OrParallelViewItem is not a top-level builder

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

    // sequence(anchor, or(sequence(heartbeat), sequence(calibration)), ack):
    // after the anchor, the FIRST branch to complete advances the parent to ack;
    // the other branch is torn down and need never complete.
    private Rule buildOrParallelRule(List<String> results) {
        return rule("or-parallel-join").build(
                pattern(station),
                sequence(
                        pattern(sensorActivated).expr("anchor", a -> a.getSensorId().equals("sensor-1")),
                        or(
                                sequence(pattern(heartbeat).expr("hb", h -> h.getSensorId().equals("sensor-1"))),
                                sequence(pattern(calibration).expr("cal", c -> c.getSensorId().equals("sensor-1")))
                        ),
                        pattern(ack).expr("ack", a -> a.getOperator().equals("alice"))
                ),
                execute(() -> results.add("acknowledged"))
        );
    }

    @Test
    public void orFirstBranchAdvancesParent() {
        final List<String> results = new ArrayList<>();
        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(buildOrParallelRule(results))).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));                    // branch 1 completes → parent advances (calibration never delivered)
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // ack step matches → root sequence ends → fire
        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void orAdvancesRegardlessOfWhichBranchWins() {
        // Calibration (branch 2) completes; heartbeat (branch 1) never does. The OR-join
        // still advances the parent — any branch winning first is sufficient.
        final List<String> results = new ArrayList<>();
        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(buildOrParallelRule(results))).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new CalibrationPassed("sensor-1"));            // branch 2 wins (heartbeat never delivered)
        assertThat(results).isEmpty();                              // or-join advanced parent to ack; rule not fired yet
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice"));
        assertThat(results).containsExactly("acknowledged");
    }

    @Test
    public void orLosingBranchEventIsInertAfterWin() {
        // Heartbeat (branch 1) wins and the parent advances; the rule fires once. Delivering
        // the losing branch's event (calibration) afterward must be inert — no second fire —
        // confirming the losing branch was torn down.
        final List<String> results = new ArrayList<>();
        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(buildOrParallelRule(results))).newKieSession();

        insertAndFire(new MonitoringStation("station-1"));
        insertAndFire(new SensorActivated("sensor-1"));
        insertAndFire(new HeartbeatOk("sensor-1"));                   // branch 1 wins
        assertThat(results).isEmpty();                               // or-join advanced parent to ack; rule not fired yet
        insertAndFire(new OperatorAcknowledged("sensor-1", "alice")); // root ends → fire once
        assertThat(results).containsExactly("acknowledged");
        insertAndFire(new CalibrationPassed("sensor-1"));            // losing branch event — must be inert
        assertThat(results).containsExactly("acknowledged");         // still exactly one fire
    }

    @Test
    public void temporalDecoratorOnOrParallelStepIsRejected() {
        // Mirror of PatternDSLSequenceParallelTest#temporalDecoratorOnParallelStepIsRejected
        // with the step changed to or(...). within(...) rejects a temporal decorator on an
        // or-parallel step at construction time (in validateTimedArgs), not at KieBase build.
        assertThatThrownBy(() ->
                within(Duration.ofSeconds(1),
                        or(sequence(pattern(heartbeat).expr("hb", h -> h.getSensorId().equals("sensor-1"))),
                           sequence(pattern(calibration).expr("cal", c -> c.getSensorId().equals("sensor-1"))))))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("parallel");
    }
}

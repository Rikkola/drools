package org.drools.base.reteoo.sequencing.steps;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicCircuit;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;

public class LogicCircuitStep extends AbstractStep implements Step {
    private final LogicCircuit circuit;

    public LogicCircuitStep(int index, Sequence sequence, LogicCircuit circuit) {
        super(StepType.LOGIC_CIRCUIT, index, sequence);
        this.circuit = circuit;
    }

    public LogicCircuit getCircuit() {
        return circuit;
    }

    public void activate(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
        for (LogicGate gate : circuit.getGates()) {
            gate.activate(sequenceMemory, valueResolver);
        }
    }

    public void deactivate(SequenceMemory sequenceMemory, ValueResolver valueResolver) {
        for (LogicGate gate : circuit.getGates()) {
            gate.deactivate(sequenceMemory, valueResolver);
        }
    }
}

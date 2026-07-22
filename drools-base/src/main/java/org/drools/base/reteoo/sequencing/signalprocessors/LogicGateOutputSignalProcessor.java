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
package org.drools.base.reteoo.sequencing.signalprocessors;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;

public class LogicGateOutputSignalProcessor extends SignalProcessor {
    private final SignalIndex[] gates;

    private LogicGate gate1;
    private int       index1;
    private LogicGate gate2;
    private int       index2;
    private LogicGate gate3;
    private int       index3;
    private LogicGate gate4;
    private int       index4;


    public LogicGateOutputSignalProcessor(SignalIndex... gates) {
        this.gates       = gates;

        switch (gates.length) {
            case 4:
                gate4 = gates[3].getGate();
                index4 = gates[3].getBitIndex();
            case 3:
                gate3 = gates[2].getGate();
                index3 = gates[2].getBitIndex();
            case 2:
                gate2 = gates[1].getGate();
                index2 = gates[1].getBitIndex();
            case 1:
                gate1 = gates[0].getGate();
                index1 = gates[0].getBitIndex();
                break;
        }
    }

    public void consume(SignalStatus signalStatus, SequenceMemory memory, ValueResolver valueResolver) {
        switch (gates.length) {
            case 4:
                gate4.consume(index4, signalStatus, memory, valueResolver);
            case 3:
                gate3.consume(index3, signalStatus, memory, valueResolver);
            case 2:
                gate2.consume(index2, signalStatus, memory, valueResolver);
            case 1:
                gate1.consume(index1, signalStatus, memory, valueResolver);
                break;
            default:
                for (int i = gates.length - 1; i >= 0; i--) {
                    gates[i].getGate().consume(gates[i].getBitIndex(), signalStatus, memory, valueResolver);
                }
        }
    }

    @Override
    public void consume(int signalBitIndex, SignalStatus signalStatus, SequenceMemory memory, ValueResolver valueResolver) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void reset(SequenceMemory memory, ValueResolver valueResolver) {
        // Do nothing
    }
}

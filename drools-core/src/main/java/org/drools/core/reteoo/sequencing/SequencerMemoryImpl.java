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
package org.drools.core.reteoo.sequencing;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.DynamicFilter;
import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequencer;
import org.drools.base.reteoo.sequencing.SequencerMemory;
import org.drools.base.reteoo.sequencing.steps.ParallelStep;
import org.drools.base.reteoo.sequencing.steps.Step;
import org.drools.base.reteoo.sequencing.steps.Step.StepType;
import org.drools.base.reteoo.sequencing.steps.SubsequenceStep;
import org.drools.core.common.ActivationsManager;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.TupleSets;
import org.drools.core.reteoo.LeftTuple;
import org.drools.core.reteoo.LeftTupleSink;
import org.drools.core.reteoo.PathMemory;
import org.drools.core.reteoo.SegmentMemory;
import org.drools.core.reteoo.SequenceNode;
import org.drools.core.reteoo.SequenceNode.SequenceNodeMemory;
import org.drools.base.reteoo.SignalAdapter;
import org.drools.core.reteoo.TupleFactory;
import org.drools.core.reteoo.TupleImpl;
import org.drools.base.reteoo.sequencing.Sequence.SequenceMemory;
import org.drools.base.reteoo.sequencing.signalprocessors.LogicGate;
import org.drools.base.util.CircularArrayList;
import org.kie.api.runtime.rule.FactHandle;

import java.util.ArrayList;
import java.util.List;

import static org.drools.core.phreak.TupleEvaluationUtil.flushLeftTupleIfNecessary;

public class SequencerMemoryImpl implements SequencerMemory {

    private final TupleImpl lt;

    private final CircularArrayList<Object> events;

    private final SequenceMemory[] sequenceMemories;

    private final LeftTupleSink sink;

    private Sequencer sequencer;

    private SequenceNode node;

    private SequenceNodeMemory nodeMemory;

    private SequenceMemory childSequenceMemory;

    public SequencerMemoryImpl(Sequencer sequencer, TupleImpl lt, LeftTupleSink sink, SequenceNode node, SequenceNodeMemory nodeMemory) {
        this.sequencer        = sequencer;
        this.lt               = lt;
        this.events           = new CircularArrayList<>(Object.class, 100);
        this.sink             = sink;
        this.sequenceMemories = new SequenceMemory[sequencer.getSequences().length];
        this.node   = node;
        this.nodeMemory = nodeMemory;
    }

    @Override
    public TupleImpl getLeftTuple() {
        return lt;
    }

    @Override
    public CircularArrayList<Object> getData() {
        return events;
    }

    @Override
    public Sequencer getSequencer() {
        return sequencer;
    }

    @Override
    public SequenceMemory getChildSequenceMemory() {
        return childSequenceMemory;
    }

    @Override
    public void setChildSequenceMemory(SequenceMemory childSequenceMemory) {
        this.childSequenceMemory = childSequenceMemory;
    }

    public SequenceMemory getOrCreateSequenceMemory(SequenceMemory parent, Sequence sequence, CircularArrayList<Object> newData) {
        SequenceMemory sequenceMemory = sequenceMemories[sequence.getSequenceIndex()];
        if (sequenceMemory == null) {

            int signalAdapters = 0;

            for (LogicGate gate : sequence.getGates()) {
                signalAdapters = signalAdapters + gate.getSignalAdapterIndexes().length;
            }

            long[] gateMemory    = new long[sequence.getGates().length];
            long[] counterMemory = new long[0];

            CircularArrayList<Object> data = newData == null ? new CircularArrayList<>(1000) : newData;

            sequenceMemory  = new SequenceMemory(this, parent, sequence, data,
                                                  new SignalAdapter[signalAdapters], new SignalAdapter[signalAdapters],
                                                  gateMemory, counterMemory);

            sequenceMemories[sequence.getSequenceIndex()] = sequenceMemory;
        }

        return sequenceMemory;
    }

    @Override
    public SequenceMemory getSequenceMemory(Sequence sequence) {
        SequenceMemory sequenceMemory = sequenceMemories[sequence.getSequenceIndex()];
        return sequenceMemory;
    }


    @Override
    public void match(ValueResolver valueResolver) {
        boolean wasEmpty = nodeMemory.getStagedChildTuples().isEmpty();
        // leftTupleMemoryEnabled=true so the child is linked to the anchor left-tuple via
        // setFirstChild/setLastChild. Without this link, deleteChildren() cannot find and
        // clean up the child when the anchor is retracted after sequence completion, causing
        // a stale agenda activation to fire a second time.
        TupleImpl child = TupleFactory.createLeftTuple(lt, sink,  lt.getPropagationContext(), true);
        nodeMemory.getStagedChildTuples().addInsert(child);

        long          nodePosMaskBit = nodeMemory.getNodePosMaskBit();
        SegmentMemory smem           = nodeMemory.getSegmentMemory();
        boolean       shouldFlush    = node.isStreamMode();

        if (wasEmpty) {
            shouldFlush = smem.notifyRuleLinkSegment(nodePosMaskBit)  | shouldFlush;
        } else {
            shouldFlush = smem.linkSegmentWithoutRuleNotify(nodePosMaskBit) | shouldFlush;
        }

        if (shouldFlush) {
            flushLeftTupleIfNecessary((ReteEvaluator) valueResolver, smem, node.isStreamMode() );
        }
    }

    @Override
    public DynamicFilter getActiveDynamicFilter(int filterIndex) {
        return nodeMemory.getActiveDynamicFilter(filterIndex);
    }

    @Override
    public void removeActiveFilter(DynamicFilter filter) {
        nodeMemory.removeActiveFilter(filter);
    }
}

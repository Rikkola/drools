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
import org.drools.base.reteoo.BaseTuple;
import org.drools.base.reteoo.DynamicFilter;
import org.drools.base.reteoo.DynamicFilterProto;
import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.Sequencer;
import org.drools.base.reteoo.sequencing.SequencerMemory;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.TupleSetsImpl;
import org.drools.core.reteoo.LeftTupleSink;
import org.drools.core.reteoo.ObjectTypeNode;
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
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.ObjectStore;

import java.util.Iterator;

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
        int filterCount = sequencer.getSequence().getFilters().length;
        this.events           = new CircularArrayList<>(Object.class, Math.max(filterCount, 4));
        this.sink             = sink;
        this.sequenceMemories = new SequenceMemory[sequencer.getSequences().length];
        this.node       = node;
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

    public SequenceMemory getOrCreateSequenceMemory(Sequence sequence, CircularArrayList<Object> newData) {
        SequenceMemory sequenceMemory = sequenceMemories[sequence.getSequenceIndex()];
        if (sequenceMemory == null) {

            int signalAdapters = 0;

            for (LogicGate gate : sequence.getGates()) {
                signalAdapters = signalAdapters + gate.getSignalAdapterIndexes().length;
            }

            long[] gateMemory    = new long[sequence.getGates().length];
            long[] counterMemory = new long[0];

            sequenceMemory  = new SequenceMemory(this, sequence, newData,
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
        TupleImpl child = TupleFactory.createLeftTuple(lt, sink, lt.getPropagationContext(), true);
        nodeMemory.getStagedChildTuples().addInsert(child);

        long          nodePosMaskBit = nodeMemory.getNodePosMaskBit();
        SegmentMemory smem           = nodeMemory.getSegmentMemory();

        if (wasEmpty) {
            smem.notifyRuleLinkSegment(nodePosMaskBit);
        } else {
            smem.linkSegmentWithoutRuleNotify(nodePosMaskBit);
        }

        ReteEvaluator reteEvaluator = (ReteEvaluator) valueResolver;
        PathMemory pmem = smem.getPathMemories().get(0);
        reteEvaluator.getRuleNetworkEvaluator().forceFlushLeftTuple(pmem, smem, new TupleSetsImpl());
        reteEvaluator.getRuleNetworkEvaluator().forceFlushWhenSubnetwork(pmem);
    }

    @Override
    public DynamicFilter getActiveDynamicFilter(int filterIndex) {
        return nodeMemory.getActiveDynamicFilter(filterIndex);
    }

    @Override
    public void removeActiveFilter(DynamicFilter filter) {
        nodeMemory.removeActiveFilter(filter);
    }

    @Override
    public boolean hasActiveMatch(int filterIndex, BaseTuple leftTuple, ValueResolver valueResolver) {
        // Build the DynamicFilter without activating it in the signal chain.
        // We only need the constraint from the proto; we do NOT call getActiveDynamicFilter()
        // because that adds the filter to activeFilters, wiring future inserts unnecessarily.
        // proto is always non-null: PhreakNodeFactory fills every slot or throws.
        DynamicFilterProto proto = node.getDynamicFilters()[filterIndex];
        DynamicFilter filter = new DynamicFilter(proto);
        ReteEvaluator reteEvaluator = (ReteEvaluator) valueResolver;
        ObjectStore objectStore = reteEvaluator.getDefaultEntryPoint().getObjectStore();

        // Resolve the concrete Java class for this filter's pattern type so we
        // iterate only the right-typed fact handles rather than all WM objects.
        // AlphaAdapter.getParent() walks up to the ObjectTypeNode.
        SequenceNode.AlphaAdapter adapter = node.getAlphaAdapters()[proto.getAdapterIndex()];
        Class<?> filterClass = ((org.drools.base.base.ClassObjectType) ((ObjectTypeNode) adapter.getParent()).getObjectType()).getClassType();

        Iterator<InternalFactHandle> it = objectStore.iterateFactHandles(filterClass);
        while (it.hasNext()) {
            InternalFactHandle fh = it.next();
            if (filter.test(fh, leftTuple, valueResolver)) {
                return true;
            }
        }
        return false;
    }
}

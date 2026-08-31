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
package org.drools.mvel.integrationtests.phreak.sequencing;

import org.drools.base.reteoo.sequencing.Sequence;
import org.drools.base.reteoo.sequencing.steps.AbsenceStep;
import org.drools.base.rule.EntryPointId;
import org.drools.base.base.ClassObjectType;
import org.drools.core.ClockType;
import org.drools.core.SessionConfiguration;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.SequenceNode;
import org.drools.kiesession.session.StatefulKnowledgeSessionImpl;
import org.drools.mvel.integrationtests.phreak.A;
import org.drools.mvel.integrationtests.phreak.B;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.conf.ThreadSafeOption;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runtime-layer tests for AbsenceStep (the not() step in sequence()).
 *
 * These tests build a Sequence directly — bypassing the DSL compiler — so they
 * exercise the Phreak runtime in isolation.
 */
public class PhreakSequencerNotStepTest extends AbstractPhreakSequencerSubsequenceTest {

    @BeforeEach
    public void setup() {
        initKBaseWithEmptyRule();

        // One-step sequence: a single AbsenceStep watching filter index 0 (bpattern).
        // AbsenceStep advances immediately when bpattern has NO match in WM.
        seq0 = new Sequence(0, new AbsenceStep.Factory(0));
        seq0.setFilters(new org.drools.base.rule.Pattern[]{bpattern});
        rule.addSequence(seq0);
        kbase.addPackage(pkg);
    }

    @Test
    public void absenceStepAdvancesWhenFilterHasNoMatch() {
        // No B("b") in working memory when sequence starts → absence confirmed →
        // step advances immediately → sequence terminates.
        createSession();
        Sequence.SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);

        // getCurrentStep returns -1 when the sequence has stepped past all steps (terminated).
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(-1);
    }

    @Test
    public void absenceStepBlocksWhenFilterHasMatch() {
        // Arrange: insert a B("b") into WM *before* the sequence starts so that
        // hasActiveMatch returns true when AbsenceStep.activate() is called.
        // We do this by manually setting up the session — same steps as
        // createSession() but inserting B before fireAllRules().

        SessionConfiguration sessionConf = kbase.getSessionConfiguration();
        sessionConf.setOption(ThreadSafeOption.NO);
        sessionConf.setClockType(ClockType.PSEUDO_CLOCK);

        if (snode == null) {
            ObjectTypeNode aNode = kbase.getRete().getEntryPointNode(EntryPointId.DEFAULT)
                    .getObjectTypeNodes().get(new ClassObjectType(A.class));
            snode = (SequenceNode) aNode.getSinks()[0].getSinks()[0];
        }

        session = (StatefulKnowledgeSessionImpl) kbase.newKieSession(sessionConf, null);

        // Insert a matching B before the sequence activates.
        session.insert(new B(0, "b"));

        // Now insert the driving fact A; fireAllRules() starts the sequence.
        // AbsenceStep activates with B("b") already in WM → step blocks.
        fhA0 = (org.drools.core.common.InternalFactHandle) session.insert(new A(0));
        session.fireAllRules();

        nodeMemory = session.getNodeMemory(snode);
        pmem = nodeMemory.getSegmentMemory().getPathMemories().get(0);
        sequencerMemory = (org.drools.base.reteoo.sequencing.SequencerMemory) fhA0.getFirstLeftTuple().getContextObject();

        Sequence.SequenceMemory sequenceMemory = sequencerMemory.getSequenceMemory(seq0);

        // Step is still 0 — the AbsenceStep blocked because B("b") matched.
        assertThat(getCurrentStep(sequencerMemory)).isEqualTo(0);
    }
}

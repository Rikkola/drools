package org.drools.metric.phreak;

import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.TupleSets;
import org.drools.core.reteoo.AsyncSendNode;
import org.drools.core.reteoo.AsyncSendNode.AsyncSendMemory;
import org.drools.core.reteoo.LeftTupleSink;
import org.drools.core.reteoo.SequenceNode;
import org.drools.core.reteoo.SequenceNode.PhreakSequenceNode;
import org.drools.core.reteoo.SequenceNode.SequenceNodeMemory;
import org.drools.metric.util.MetricLogUtils;

public class PhreakSequenceNodeMetric extends PhreakSequenceNode {

    public PhreakSequenceNodeMetric(ReteEvaluator reteEvaluator) {
        super(reteEvaluator);
    }

    @Override
    public void doNode(SequenceNode node,
                       SequenceNodeMemory memory,
                       LeftTupleSink sink,
                       TupleSets srcLeftTuples,
                       TupleSets trgLeftTuples,
                       TupleSets stagedLeftTuples) {

        try {
            MetricLogUtils.getInstance().startMetrics(node);

            super.doNode(node, memory,
                         sink,
                         srcLeftTuples, trgLeftTuples, stagedLeftTuples);

        } finally {
            MetricLogUtils.getInstance().logAndEndMetrics();
        }
    }
}

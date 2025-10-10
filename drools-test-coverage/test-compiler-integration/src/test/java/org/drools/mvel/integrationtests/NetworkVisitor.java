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
package org.drools.mvel.integrationtests;

import org.drools.base.common.NetworkNode;
import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.TupleSets;
import org.drools.core.impl.InternalRuleBase;
import org.drools.core.phreak.RuleAgendaItem;
import org.drools.core.phreak.RuleExecutor;
import org.drools.core.reteoo.*;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.List;

public class NetworkVisitor {

    public void debugManualNetwork(String networkName, NetworkNode... startNodes) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                   MANUAL NETWORK DEBUG: " + String.format("%-19s", networkName) + "║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        System.out.println("\n📊 Manual network start nodes: " + startNodes.length);
        System.out.println("┌─────────────────────────────────────────────────────────────┐");

        for (int i = 0; i < startNodes.length; i++) {
            NetworkNode node = startNodes[i];
            System.out.println("│ 🚀 Start[" + i + "]: " + node.getClass().getSimpleName() + " (id=" + node.getId() + ")");
            
            visitManualNode(node, "│   ", new java.util.HashSet<>());
            System.out.println("│");
        }
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }

    public void debugNodeChain(String chainName, NetworkNode... nodes) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    NODE CHAIN DEBUG: " + String.format("%-22s", chainName) + "║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        System.out.println("\n🔗 Chain nodes: " + nodes.length);
        System.out.println("┌─────────────────────────────────────────────────────────────┐");

        for (int i = 0; i < nodes.length; i++) {
            NetworkNode node = nodes[i];
            String connector = (i < nodes.length - 1) ? "├─" : "└─";
            String nodeIcon = getSinkIcon(node);
            String nodeInfo = getDetailedNodeInfo(node);
            
            System.out.println("│ " + connector + " " + nodeIcon + " " + node.getClass().getSimpleName() + 
                    " (id=" + node.getId() + ")" + nodeInfo);
        }
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }

    public void debugNetworkStructure(KieBase kbase) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    NETWORK STRUCTURE DEBUG                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        Rete rete = ((InternalRuleBase) kbase).getRete();
        List<ObjectTypeNode> objectTypeNodes = rete.getObjectTypeNodes();

        System.out.println("\n📊 ObjectTypeNodes found: " + objectTypeNodes.size());
        System.out.println("┌─────────────────────────────────────────────────────────────┐");

        for (ObjectTypeNode otn : objectTypeNodes) {
            String className = otn.getObjectType().toString();
            if (className.contains("class=")) {
                className = className.substring(className.lastIndexOf('.') + 1, className.lastIndexOf(']'));
            }
            System.out.println("│ 🏭 OTN: " + className + " (id=" + otn.getId() + ")");

            debugSinks(otn.getObjectSinkPropagator().getSinks(), "│   ");
            System.out.println("│");
        }
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }

    private void visitManualNode(NetworkNode node, String indent, java.util.Set<NetworkNode> visited) {
        if (visited.contains(node)) {
            System.out.println(indent + "↺ [Already visited: " + node.getClass().getSimpleName() + " id=" + node.getId() + "]");
            return;
        }
        visited.add(node);

        if (node instanceof ObjectSource) {
            Object[] sinks = ((ObjectSource) node).getObjectSinkPropagator().getSinks();
            debugSinks(sinks, indent);
        } else if (node instanceof LeftTupleSource) {
            LeftTupleSink[] sinks = ((LeftTupleSource) node).getSinkPropagator().getSinks();
            if (sinks != null && sinks.length > 0) {
                for (int i = 0; i < sinks.length; i++) {
                    LeftTupleSink sink = sinks[i];
                    boolean isLast = (i == sinks.length - 1);
                    String branch = isLast ? "└─" : "├─";
                    String nextIndent = indent + (isLast ? "  " : "│ ");

                    String sinkIcon = getSinkIcon(sink);
                    String nodeInfo = getDetailedNodeInfo(sink);

                    System.out.println(indent + branch + " " + sinkIcon + " " + sink.getClass().getSimpleName() +
                            " (id=" + sink.getId() + ")" + nodeInfo);

                    if (!(sink instanceof RuleTerminalNode)) {
                        visitManualNode(sink, nextIndent, visited);
                    }
                }
            }
        }
    }

    private void debugSinks(Object[] sinks, String indent) {
        if (sinks == null) return;

        for (int i = 0; i < sinks.length; i++) {
            Object sink = sinks[i];
            boolean isLast = (i == sinks.length - 1);
            String branch = isLast ? "└─" : "├─";
            String nextIndent = indent + (isLast ? "  " : "│ ");

            String sinkIcon = getSinkIcon(sink);
            String nodeInfo = getDetailedNodeInfo(sink);

            System.out.println(indent + branch + " " + sinkIcon + " " + sink.getClass().getSimpleName() +
                    " (id=" + ((NetworkNode) sink).getId() + ")" + nodeInfo);

            visitSink(nextIndent, sink);
        }
    }

    private String getDetailedNodeInfo(Object sink) {
        if (sink instanceof AlphaNode alphaNode) {
            return " 🔍 [" + alphaNode.getConstraint() + "]";
        } else if (sink.getClass().getSimpleName().equals("BiLinearJoinNode")) {
            // Handle BiLinearJoinNode specifically
            try {
                java.lang.reflect.Method getConstraintsMethod = sink.getClass().getMethod("getConstraints");
                Object[] constraints = (Object[]) getConstraintsMethod.invoke(sink);
                if (constraints.length == 0) {
                    return " 🚀 [BiLinear: no constraints]";
                } else {
                    String constraintStr = String.join(", ",
                            java.util.Arrays.stream(constraints)
                                    .map(Object::toString)
                                    .toArray(String[]::new));
                    return " 🚀 [BiLinear: " + constraintStr + "]";
                }
            } catch (Exception e) {
                return " 🚀 [BiLinear: error getting constraints]";
            }
        } else if (sink instanceof JoinNode joinNode) {
            if (joinNode.getConstraints().length == 0) {
                return " 🔗 [no constraints]";
            } else {
                String constraints = String.join(", ",
                        java.util.Arrays.stream(joinNode.getConstraints())
                                .map(Object::toString)
                                .toArray(String[]::new));
                return " 🔗 [" + constraints + "]";
            }
        } else if (sink instanceof RuleTerminalNode rtn) {
            return " 🏁 [" + rtn.getRule().getName() + "]";
        }
        return "";
    }

    private void visitSink(String indent, Object sink) {
        if (sink instanceof AlphaNode) {
            AlphaNode alphaNode = (AlphaNode) sink;
            debugSinks(alphaNode.getObjectSinkPropagator().getSinks(), indent);
        } else if (sink instanceof JoinNode) {
            JoinNode joinNode = (JoinNode) sink;
            LeftTupleSink[] joinSinks = joinNode.getSinkPropagator().getSinks();
            if (joinSinks != null && joinSinks.length > 0) {
                for (int j = 0; j < joinSinks.length; j++) {
                    LeftTupleSink js = joinSinks[j];
                    boolean isLastSink = (j == joinSinks.length - 1);
                    String branch = isLastSink ? "└─" : "├─";
                    String nextIndent = indent + (isLastSink ? "  " : "│ ");

                    String jsIcon = getSinkIcon(js);
                    String jsInfo = getDetailedNodeInfo(js);

                    System.out.println(indent + branch + " " + jsIcon + " " + js.getClass().getSimpleName() +
                            " (id=" + js.getId() + ")" + jsInfo);

                    if (!(js instanceof RuleTerminalNode)) {
                        visitSink(nextIndent, js);
                    }
                }
            }
        } else if (sink instanceof RightInputAdapterNode right) {
            System.out.println(indent + "├─ ➡️  Aapts to: " + right.getBetaNode().getClass().getSimpleName() +
                    " (id=" + right.getBetaNode().getId() + ")");
        } else if (sink instanceof LeftInputAdapterNode left) {
            LeftTupleSink[] leftSinks = left.getSinkPropagator().getSinks();
            if (leftSinks != null && leftSinks.length > 0) {
                for (int k = 0; k < leftSinks.length; k++) {
                    LeftTupleSink ls = leftSinks[k];
                    boolean isLastLeftSink = (k == leftSinks.length - 1);
                    String branch = isLastLeftSink ? "└─" : "├─";
                    String nextIndent = indent + (isLastLeftSink ? "  " : "│ ");

                    String lsIcon = getSinkIcon(ls);
                    String lsInfo = getDetailedNodeInfo(ls);

                    System.out.println(indent + branch + " " + lsIcon + " " + ls.getClass().getSimpleName() +
                            " (id=" + ls.getId() + ")" + lsInfo);

                    if (!(ls instanceof RuleTerminalNode)) {
                        visitSink(nextIndent, ls);
                    }
                }
            }
        }
    }

    private String getSinkIcon(Object sink) {
        if (sink instanceof LeftInputAdapterNode) return "⬅️";
        if (sink.getClass().getSimpleName().equals("BiLinearJoinNode")) return "🚀";
        if (sink instanceof JoinNode) return "🔗";
        if (sink instanceof RightInputAdapterNode) return "➡️";
        if (sink instanceof AlphaNode) return "🔍";
        if (sink instanceof RuleTerminalNode) return "🏁";
        if (sink instanceof ObjectTypeNode) return "📦";
        return "⚪";
    }

    public void debugManualMemoryState(String networkName, InternalWorkingMemory wm, NetworkNode... nodes) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              MANUAL NETWORK MEMORY STATE: " + String.format("%-16s", networkName) + "║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        System.out.println("\n🧠 Memory Analysis:");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");

        for (int i = 0; i < nodes.length; i++) {
            NetworkNode node = nodes[i];
            System.out.println("│ 🔸 Node[" + i + "]: " + node.getClass().getSimpleName() + " (id=" + node.getId() + ")");
            
            try {
                if (node instanceof BetaNode) {
                    BetaMemory betaMemory = (BetaMemory) wm.getNodeMemory((BetaNode) node);
                    if (betaMemory != null) {
                        System.out.println("│   📊 BetaMemory:");
                        System.out.println("│     • Left memory size: " + betaMemory.getLeftTupleMemory().size());
                        System.out.println("│     • Right memory size: " + betaMemory.getRightTupleMemory().size());
                        
                        SegmentMemory segMem = betaMemory.getSegmentMemory();
                        if (segMem != null) {
                            System.out.println("│     • Segment linked: " + segMem.isSegmentLinked());
                            System.out.println("│     • Segment pos: " + segMem.getPos());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("│   ❌ Error accessing memory: " + e.getMessage());
            }
            System.out.println("│");
        }
        
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }

    public void debugPhreakRuntime(KieSession ksession) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                  PHREAK RUNTIME STRUCTURE                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        ReteEvaluator reteEvaluator = (ReteEvaluator) ksession;
        InternalRuleBase ruleBase = (InternalRuleBase) ksession.getKieBase();

        System.out.println("\n🔥 Runtime Analysis:");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");

        List<RuleTerminalNode> terminalNodes = findRuleTerminalNodes(ruleBase.getRete());

        System.out.println("│ 📊 Found " + terminalNodes.size() + " rule terminal nodes");
        System.out.println("│");

        for (RuleTerminalNode rtn : terminalNodes) {
            debugRuleRuntimeState(rtn, reteEvaluator);
            System.out.println("│");
        }

        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }

    private List<RuleTerminalNode> findRuleTerminalNodes(Rete rete) {
        List<RuleTerminalNode> terminals = new ArrayList<>();

        for (EntryPointNode entryPoint : rete.getEntryPointNodes().values()) {
            for (ObjectTypeNode otn : entryPoint.getObjectTypeNodes().values()) {
                collectTerminalNodes(otn, terminals);
            }
        }

        return terminals;
    }

    private void collectTerminalNodes(NetworkNode node, List<RuleTerminalNode> terminals) {
        if (node instanceof RuleTerminalNode) {
            terminals.add((RuleTerminalNode) node);
            return;
        }

        if (node instanceof ObjectSource) {
            Object[] sinks = ((ObjectSource) node).getObjectSinkPropagator().getSinks();
            if (sinks != null) {
                for (Object sink : sinks) {
                    collectTerminalNodes((NetworkNode) sink, terminals);
                }
            }
        } else if (node instanceof LeftTupleSource) {
            LeftTupleSink[] sinks = ((LeftTupleSource) node).getSinkPropagator().getSinks();
            if (sinks != null) {
                for (LeftTupleSink sink : sinks) {
                    collectTerminalNodes(sink, terminals);
                }
            }
        }
    }

    private void debugRuleRuntimeState(RuleTerminalNode rtn, ReteEvaluator reteEvaluator) {
        RuleImpl rule = rtn.getRule();
        System.out.println("│ 🎯 Rule: " + rule.getName() + " (RTN id=" + rtn.getId() + ")");

        try {
            PathMemory pathMemory = reteEvaluator.getNodeMemory(rtn);

            System.out.println("│   📍 PathMemory State:");
            System.out.println("│     • Rule linked: " + pathMemory.isRuleLinked());
            System.out.println("│     • Linked segment mask: 0b" + Long.toBinaryString(pathMemory.getLinkedSegmentMask()));
            System.out.println("│     • All linked mask test: 0b" + Long.toBinaryString(pathMemory.getAllLinkedMaskTest()));

            RuleAgendaItem ruleAgendaItem = pathMemory.getRuleAgendaItem();
            if (ruleAgendaItem != null) {
                RuleExecutor ruleExecutor = ruleAgendaItem.getRuleExecutor();
                System.out.println("│     • Rule executor dirty: " + ruleExecutor.isDirty());
                System.out.println("│     • Active matches: " + (ruleExecutor.getActiveMatches() != null ?
                        ruleExecutor.getActiveMatches().size() : 0));
                System.out.println("│     • Dormant matches: " + (ruleExecutor.getDormantMatches() != null ?
                        ruleExecutor.getDormantMatches().size() : 0));
            }

            SegmentMemory[] segmentMemories = pathMemory.getSegmentMemories();
            System.out.println("│   🧠 Segment Analysis (count=" + segmentMemories.length + "):");

            for (int i = 0; i < segmentMemories.length; i++) {
                SegmentMemory smem = segmentMemories[i];
                if (smem != null) {
                    debugSegmentMemory(smem, i);
                }
            }

        } catch (Exception e) {
            System.out.println("│   ❌ Error accessing runtime state: " + e.getMessage());
        }
    }

    private void debugSegmentMemory(SegmentMemory smem, int segmentIndex) {
        System.out.println("│     🔸 Segment[" + segmentIndex + "] pos=" + smem.getPos() + ":");
        System.out.println("│       • Linked: " + smem.isSegmentLinked());
        System.out.println("│       • Linked node mask: 0b" + Long.toBinaryString(smem.getLinkedNodeMask()));
        System.out.println("│       • All linked test mask: 0b" + Long.toBinaryString(smem.getAllLinkedMaskTest()));
        System.out.println("│       • Dirty node mask: 0b" + Long.toBinaryString(smem.getDirtyNodeMask()));

        TupleSets stagedTuples = smem.getStagedLeftTuples();
        if (stagedTuples != null) {
            System.out.println("│       • Staged inserts: " + stagedTuples.getInsertSize());
            System.out.println("│       • Staged updates: " + countUpdates(stagedTuples));
            System.out.println("│       • Staged deletes: " + countDeletes(stagedTuples));
        }

        List<PathMemory> pathMemories = smem.getPathMemories();
        if (pathMemories != null && pathMemories.size() > 1) {
            System.out.println("│       • Shared by " + pathMemories.size() + " rules:");
            for (PathMemory pm : pathMemories) {
                if (pm.getRule() != null) {
                    System.out.println("│         - " + pm.getRule().getName());
                }
            }
        }
    }

    private int countUpdates(TupleSets tupleSets) {
        int count = 0;
        TupleImpl tuple = tupleSets.getUpdateFirst();
        while (tuple != null) {
            count++;
            tuple = tuple.getStagedNext();
        }
        return count;
    }

    private int countDeletes(TupleSets tupleSets) {
        int count = 0;
        TupleImpl tuple = tupleSets.getDeleteFirst();
        while (tuple != null) {
            count++;
            tuple = tuple.getStagedNext();
        }
        return count;
    }

}
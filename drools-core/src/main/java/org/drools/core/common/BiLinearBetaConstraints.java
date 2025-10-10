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
package org.drools.core.common;

import org.drools.base.base.ObjectType;
import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.BaseTuple;
import org.drools.base.rule.Pattern;
import org.drools.base.rule.constraint.BetaConstraint;
import org.drools.core.RuleBaseConfiguration;
import org.drools.core.reteoo.*;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.util.index.TupleList;
import org.drools.util.bitmask.BitMask;
import org.kie.api.runtime.rule.FactHandle;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Objects;

/**
 * BiLinearBetaConstraints wraps standard BetaConstraints to provide cross-network
 * variable resolution for BiLinearJoinNode. It intercepts constraint evaluation
 * calls and ensures that variables from both input networks are available during
 * evaluation.
 *
 * This wrapper maintains compatibility with existing BetaConstraints interface
 * while extending functionality for dual-network scenarios.
 */
public class BiLinearBetaConstraints implements BetaConstraints<BiLinearContextEntry> {

    /** The wrapped standard beta constraints */
    private final BetaConstraints wrappedConstraints;

    /** Declaration context for cross-network variable resolution */
    private final BiLinearDeclarationContext declarationContext;

    /**
     * Creates BiLinearBetaConstraints wrapping standard constraints
     *
     * @param wrappedConstraints The standard beta constraints to wrap
     * @param declarationContext The cross-network declaration context
     */
    public BiLinearBetaConstraints(BetaConstraints wrappedConstraints,
                                   BiLinearDeclarationContext declarationContext) {
        this.wrappedConstraints = wrappedConstraints;
        this.declarationContext = declarationContext;
    }

    @Override
    public void init(BuildContext context, int betaNodeType) {
        wrappedConstraints.init(context, betaNodeType);
    }

    @Override
    public void initIndexes(int depth, int betaNodeType, RuleBaseConfiguration config) {
        wrappedConstraints.initIndexes(depth, betaNodeType, config);
    }

    @Override
    public BiLinearContextEntry createContext() {
        // Create BiLinear context instead of standard context
        return new BiLinearContextEntry(declarationContext);
    }

    @Override
    public boolean isIndexed() {
        return wrappedConstraints.isIndexed();
    }

    @Override
    public int getIndexCount() {
        return wrappedConstraints.getIndexCount();
    }

    @Override
    public boolean isEmpty() {
        return wrappedConstraints.isEmpty();
    }

    @Override
    public BetaMemory createBetaMemory(RuleBaseConfiguration config, int betaNodeType) {
        // Create BiLinearMemory with BiLinearContextEntry instead of delegating to wrapped constraints
        // This ensures the context type matches what BiLinearBetaConstraints expects and provides
        // proper BiLinear memory management capabilities
        return new org.drools.core.reteoo.BiLinearMemory<BiLinearContextEntry>(
                config.isSequential() ? null : new TupleList(),
                new TupleList(),
                createContext(),
                betaNodeType
        );
    }

    /**
     * Updates constraint context from BiLinear tuple containing both networks
     */
    @Override
    public void updateFromTuple(BiLinearContextEntry context, ValueResolver valueResolver, Tuple tuple) {
        // Update our BiLinear context
        context.updateFromTuple(valueResolver, tuple);

        // If we have a BiLinearTuple, we can use it directly
        if (tuple instanceof BiLinearTuple) {
            BiLinearTuple biLinearTuple = (BiLinearTuple) tuple;

            // Update context with both network tuples
            context.updateFromBiLinearTuples(
                    valueResolver,
                    biLinearTuple.getFirstNetworkTuple(),
                    biLinearTuple.getSecondNetworkTuple()
            );
        }

        Tuple primaryTuple = tuple;
        if (tuple instanceof BiLinearTuple) {
            primaryTuple = ((BiLinearTuple) tuple).getFirstNetworkTuple();
        }

        Object wrappedContext = getWrappedContext(context);
        if (primaryTuple != null) {
            wrappedConstraints.updateFromTuple(wrappedContext, valueResolver, (Tuple) primaryTuple);
        }
    }

    /**
     * Updates constraint context from separate network tuples
     * This method is specific to BiLinearJoinNode processing
     */
    public void updateFromBiLinearTuples(BiLinearContextEntry context,
                                         ValueResolver valueResolver,
                                         Tuple firstNetworkTuple,
                                         Tuple secondNetworkTuple) {
        // Update our BiLinear context with both tuples
        context.updateFromBiLinearTuples(valueResolver, firstNetworkTuple, secondNetworkTuple);

        Object wrappedContext = getWrappedContext(context);
        wrappedConstraints.updateFromTuple(wrappedContext, valueResolver, (Tuple) firstNetworkTuple);
    }

    @Override
    public void updateFromFactHandle(BiLinearContextEntry context, ValueResolver valueResolver, FactHandle handle) {
        // Update our BiLinear context
        context.updateFromFactHandle(valueResolver, handle);

        Object wrappedContext = getWrappedContext(context);
        wrappedConstraints.updateFromFactHandle(wrappedContext, valueResolver, handle);
    }

    @Override
    public void resetTuple(BiLinearContextEntry context) {
        context.resetTuple();

        Object wrappedContext = getWrappedContext(context);
        wrappedConstraints.resetTuple(wrappedContext);
    }

    /**
     * Handle legacy context types by delegating to wrapped constraints
     * This allows BiLinearBetaConstraints to work with regular ContextEntry arrays
     */
    @SuppressWarnings("unchecked")
    public void resetTupleContext(Object context) {
        if (context instanceof BiLinearContextEntry) {
            resetTuple((BiLinearContextEntry) context);
        } else {
            // Delegate to wrapped constraints - pass context as-is since the wrapped
            // constraints know what type they expect (ContextEntry or ContextEntry[])
            wrappedConstraints.resetTuple(context);
        }
    }

    @Override
    public void resetFactHandle(BiLinearContextEntry context) {
        context.resetFactHandle();

        Object wrappedContext = getWrappedContext(context);
        wrappedConstraints.resetFactHandle(wrappedContext);
    }

    @Override
    public boolean isAllowedCachedLeft(BiLinearContextEntry context, FactHandle handle) {
        // Phase 2 Fix: Proper handling for EmptyBetaConstraints and alpha-only joins

        // For EmptyBetaConstraints (alpha-only joins), delegate directly
        if (wrappedConstraints instanceof org.drools.core.common.EmptyBetaConstraints) {
            // EmptyBetaConstraints always returns true - no beta constraints to evaluate
            return wrappedConstraints.isAllowedCachedLeft(wrappedConstraints.createContext(), handle);
        }

        Object wrappedContext = getWrappedContext(context);
        return wrappedConstraints.isAllowedCachedLeft(wrappedContext, handle);
    }

    @Override
    public boolean isAllowedCachedRight(BaseTuple tuple, BiLinearContextEntry context) {
        // Phase 2 Fix: Proper handling for EmptyBetaConstraints and alpha-only joins

        // For EmptyBetaConstraints (alpha-only joins), delegate directly
        if (wrappedConstraints instanceof org.drools.core.common.EmptyBetaConstraints) {
            // EmptyBetaConstraints always returns true - no beta constraints to evaluate
            return wrappedConstraints.isAllowedCachedRight(tuple, wrappedConstraints.createContext());
        }

        Object wrappedContext = getWrappedContext(context);
        return wrappedConstraints.isAllowedCachedRight(tuple, wrappedContext);
    }

    /**
     * Gets or creates a wrapped standard context from BiLinear context
     * This ensures compatibility with existing constraint implementations
     */
    @SuppressWarnings("unchecked")
    private Object getWrappedContext(BiLinearContextEntry context) {
        // Create wrapped context from the wrapped constraints - type depends on implementation
        Object wrappedContext = wrappedConstraints.createContext();

        // Sync state from BiLinear context to wrapped context
        if (context.getFirstNetworkTuple() != null) {
            wrappedConstraints.updateFromTuple(wrappedContext, context.getValueResolver(), (Tuple) context.getFirstNetworkTuple());
        }
        if (context.getRightHandle() != null) {
            wrappedConstraints.updateFromFactHandle(wrappedContext, context.getValueResolver(), context.getRightHandle());
        }

        return wrappedContext;
    }

    // Delegate remaining methods to wrapped constraints

    @Override
    public BetaConstraint[] getConstraints() {
        return wrappedConstraints.getConstraints();
    }

    @Override
    public BetaConstraints getOriginalConstraint() {
        return wrappedConstraints.getOriginalConstraint();
    }

    @Override
    public <T> T cloneIfInUse() {
        // Clone the wrapped constraints and wrap them again in BiLinearBetaConstraints
        BetaConstraints clonedWrapped = (BetaConstraints) wrappedConstraints.cloneIfInUse();
        BiLinearBetaConstraints cloned = new BiLinearBetaConstraints(clonedWrapped, declarationContext);
        return (T) cloned;
    }

    @Override
    public BitMask getListenedPropertyMask(Pattern pattern, ObjectType modifiedType, List<String> settableProperties) {
        return wrappedConstraints.getListenedPropertyMask(pattern, modifiedType, settableProperties);
    }

    @Override
    public boolean isLeftUpdateOptimizationAllowed() {
        return wrappedConstraints.isLeftUpdateOptimizationAllowed();
    }

    @Override
    public void registerEvaluationContext(BuildContext buildContext) {
        wrappedConstraints.registerEvaluationContext(buildContext);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(wrappedConstraints);
        out.writeObject(declarationContext);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // Note: This implementation assumes immutable wrapped constraints
        // In a full implementation, we'd need to handle proper deserialization
        throw new UnsupportedOperationException("Deserialization not yet implemented for BiLinearBetaConstraints");
    }

    /**
     * Gets the wrapped standard constraints
     */
    public BetaConstraints getWrappedConstraints() {
        return wrappedConstraints;
    }

    /**
     * Gets the declaration context
     */
    public BiLinearDeclarationContext getDeclarationContext() {
        return declarationContext;
    }

    @Override
    public String toString() {
        return "BiLinearBetaConstraints{" +
                "wrapped=" + wrappedConstraints +
                ", declarationContext=" + declarationContext +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BiLinearBetaConstraints)) {
            return false;
        }
        BiLinearBetaConstraints other = (BiLinearBetaConstraints) obj;
        return Objects.equals(wrappedConstraints, other.wrappedConstraints) &&
                Objects.equals(declarationContext, other.declarationContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wrappedConstraints, declarationContext);
    }
}
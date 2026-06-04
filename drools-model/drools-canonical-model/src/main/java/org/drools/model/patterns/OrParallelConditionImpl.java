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
package org.drools.model.patterns;

import java.util.List;

import org.drools.model.Condition;
import org.drools.model.Variable;

/**
 * An or-parallel fork-join over branch sequences. Each sub-condition is itself a
 * {@link SequenceConditionImpl}. Lowered from {@code or(sequence, …)}; compiled to
 * a runtime OR_PARALLEL step (OR-join: parent advances on the first branch to end,
 * losing branches torn down).
 */
public class OrParallelConditionImpl implements Condition {

    private final List<Condition> branches;

    public OrParallelConditionImpl(List<Condition> branches) {
        this.branches = branches;
    }

    @Override
    public Type getType() {
        return Type.OR_PARALLEL;
    }

    @Override
    public List<Condition> getSubConditions() {
        return branches;
    }

    @Override
    public Variable<?>[] getBoundVariables() {
        return new Variable[0];
    }
}

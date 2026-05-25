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
package org.drools.model.view;

import org.drools.model.Condition;
import org.drools.model.TimedKind;
import org.drools.model.Variable;

public class TimedExprViewItem<T> extends AbstractExprViewItem<T> implements org.drools.model.SequenceStep {

    private final TimedKind kind;
    private final long durationMillis;
    private final ViewItem step;

    @SuppressWarnings("unchecked")
    public TimedExprViewItem(TimedKind kind, long durationMillis, ViewItem step) {
        super((Variable<T>) step.getFirstVariable());
        this.kind = kind;
        this.durationMillis = durationMillis;
        this.step = step;
    }

    public TimedKind getKind() {
        return kind;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public ViewItem getStep() {
        return step;
    }

    @Override
    public Variable<?>[] getVariables() {
        return step.getVariables();
    }

    // Transparent: the timer is orthogonal to gate kind. The SEQUENCE walker
    // unwraps the timed step before inspecting its type, so this simply reports
    // the wrapped step's type rather than a distinct timed type.
    @Override
    public Condition.Type getType() {
        return step instanceof ExprViewItem ? ((ExprViewItem<?>) step).getType() : Condition.Type.PATTERN;
    }
}

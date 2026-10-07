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
package org.drools.base.base;

import org.drools.base.RuleBase;
import org.drools.base.rule.accessor.GlobalResolver;
import org.drools.base.time.TimerService;

public interface ValueResolver {

    default Object getGlobal(String identifier) {
        return getGlobalResolver().resolveGlobal( identifier );
    }

    long getCurrentTime();

    GlobalResolver getGlobalResolver();

    RuleBase getRuleBase();

    /**
     * Returns the session timer service.
     * Used by the sequencing runtime to schedule per-step and whole-sequence
     * timeout jobs.
     */
    TimerService getTimerService();

    /**
     * Enqueue an action to be executed on the rule evaluation thread.
     * Used by the sequencing timer infrastructure to schedule timeout actions.
     * The action receives this resolver as its argument.
     */
    void addTimerAction(TimerAction action);

    /** A single-method action that the sequencing timer infrastructure can schedule. */
    @FunctionalInterface
    interface TimerAction {
        void execute(ValueResolver valueResolver);
    }
}

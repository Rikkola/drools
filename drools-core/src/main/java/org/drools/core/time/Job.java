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
package org.drools.core.time;

/**
 * Extends the base Job interface so implementations in drools-core are
 * compatible with the drools-base timer infrastructure used by sequencing.
 * The base interface requires execute(org.drools.base.time.JobContext); this
 * interface narrows the parameter to the drools-core JobContext (which itself
 * extends org.drools.base.time.JobContext).
 */
public interface Job extends org.drools.base.time.Job {
    void execute(JobContext ctx);

    @Override
    default void execute(org.drools.base.time.JobContext ctx) {
        execute((JobContext) ctx);
    }
}

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

import org.drools.base.time.JobHandle;
import org.drools.base.time.Trigger;

/**
 * Extends the base SchedulerService so drools-core timer implementations
 * are compatible with the drools-base sequencing timer infrastructure.
 * Implementations must accept drools-base Job/JobContext as well as any
 * drools-core subtype.
 */
public interface SchedulerService extends org.drools.base.time.SchedulerService {

    /**
     * Remove the job identified by the given job handle from the scheduled queue.
     */
    void removeJob(JobHandle jobHandle);
}

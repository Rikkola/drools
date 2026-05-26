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
package org.drools.model;

/** Discriminator for {@link org.drools.model.view.TimedExprViewItem}'s timer kind. */
public enum TimedKind {
    /** {@code within(d, step)} — fail the sequence if no match within d. */
    TIMEOUT,
    /** {@code settle(d, step)} — propagate only if a match is held for d continuously. */
    SETTLE,
    /** {@code armAfter(d, step)} — ignore matches during a quiet window after activation, propagate the first match after. */
    ARM_AFTER
}

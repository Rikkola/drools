/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.feel.lang.types;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoPeriod;

import org.kie.dmn.feel.lang.Type;
import org.kie.dmn.model.api.GwtIncompatible;

@GwtIncompatible
public class BuiltInDateType {

    public static Type determineTypeFromInstance(Object o) {
        if (o instanceof LocalTime || o instanceof OffsetTime) {
            return BuiltInType.TIME;
        } else if (o instanceof ZonedDateTime || o instanceof OffsetDateTime || o instanceof LocalDateTime) {
            return BuiltInType.DATE_TIME;
        } else if (o instanceof Duration || o instanceof ChronoPeriod) {
            return BuiltInType.DURATION;
        }

        return BuiltInType.UNKNOWN;
    }
}

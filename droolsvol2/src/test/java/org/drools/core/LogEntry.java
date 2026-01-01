package org.drools.core;

import org.drools.api.data.DataHandle;

public record LogEntry<T>(int index, String action, DataHandle<T> handle, Object object) {}

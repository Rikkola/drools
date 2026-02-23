package org.drools.core;

import org.drools.api.data.ObjectHandle;

public record LogEntry<T>(int index, String action, ObjectHandle<T> handle, Object object) {}

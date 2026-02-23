package org.drools.core;

import org.drools.api.data.DataProcessor;
import org.drools.base.base.ObjectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassTypeInfo<DS, T>  {
    private Map<ObjectType, List<DataProcessor<DS, T>>> cache = new HashMap<>();

}

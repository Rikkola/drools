package org.kie.yard.core;

import java.util.Map;

public class YaRDResult {
    private final Map<String, Object> map;

    public YaRDResult(Map<String, Object> map) {
        this.map = map;
    }

    public Map<String, Object> getMap() {
        return map;
    }
}

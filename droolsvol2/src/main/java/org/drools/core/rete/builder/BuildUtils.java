package org.drools.core.rete.builder;

import java.util.HashMap;
import java.util.Map;

public class BuildUtils {
    private final Map<Class< ? >, ComponentBuilder> componentBuilders = new HashMap<>();

    /**
     * Adds the given builder for the given target to the builders map
     */
    public void addBuilder(final Class< ? > target,
                           final ComponentBuilder builder) {
        this.componentBuilders.put( target,
                                    builder );
    }
}

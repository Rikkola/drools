package org.kie.dmn.feel.util;

import java.lang.reflect.Modifier;

public class ModifierUtil {

    public static boolean isStatic(int mod) {
        return Modifier.isStatic(mod);
    }
}

package org.kie.dmn.feel.util;

public class ModifierUtil {

    public static final int STATIC = 0x00000008;

    public static boolean isStatic(int mod) {
        return (mod & STATIC) != 0;
    }
}

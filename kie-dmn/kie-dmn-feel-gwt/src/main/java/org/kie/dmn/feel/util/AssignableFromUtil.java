package org.kie.dmn.feel.util;

public class AssignableFromUtil {

    /*
    XXX: Not as good as the real thing.
     */
    public static boolean isAssignableFrom(final Class thisClass,
                                           final Class otherClass) {
        if (otherClass == null) {
            return false;
        }

        if (otherClass.equals(thisClass)) {
            return true;
        }

        Class currentSuperClass = otherClass.getSuperclass();
        while (currentSuperClass != null) {
            if (currentSuperClass.equals(thisClass)) {
                return true;
            }
            currentSuperClass = otherClass.getSuperclass();
        }
        return false;
    }
}

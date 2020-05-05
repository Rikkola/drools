package org.kie.dmn.feel.util;

public class AssignableFromUtil {

    /*
    XXX: Not as good as the real thing.
     */
    public static boolean isAssignableFrom(final Class thisClass,
                                           Class otherClass) {
        while (otherClass != null) {
            if (otherClass.equals(thisClass)) {
                return true;
            }
            otherClass = otherClass.getSuperclass();
        }
        return false;
    }
}

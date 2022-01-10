package dev.morphia.mapping;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;

public class SetAccessibleHelper {
    private static MethodHandle trySetAccessibleHandle;
    
    static {
        try {
            trySetAccessibleHandle = MethodHandles.lookup().findVirtual(AccessibleObject.class, "trySetAccessible",
                    MethodType.methodType(boolean.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // fall back to setAccessible()
            trySetAccessibleHandle = null;
        }
    }
    
    public static boolean trySetAccessible(AccessibleObject ao) {
        if (trySetAccessibleHandle != null) {
            try {
                return (boolean) trySetAccessibleHandle.invokeExact(ao);
            } catch (Throwable e) {
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
            
        } else {
            try {
                ao.setAccessible(true);
                return true;
            } catch (Exception e) {
                if ("java.lang.reflect.InaccessibleObjectException".equals(e.getClass().getName())) return false;
                else if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
        }
    }
}

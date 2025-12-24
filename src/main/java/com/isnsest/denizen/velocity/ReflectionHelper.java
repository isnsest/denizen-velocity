package com.isnsest.denizen.velocity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class ReflectionHelper {

    public static Field getField(Class<?> clazz, String field) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            return f;
        }
        catch (Exception ex) {
            DenizenVelocity.instance.logger.info("ReflectionHelper.getField failed for " + field);
            ex.printStackTrace();
        }
        return null;
    }

    public static MethodHandle getGetter(Class<?> clazz, String name) {
        try {
            return LOOKUP.unreflectGetter(getField(clazz, name));
        }
        catch (Exception ex) {
            DenizenVelocity.instance.logger.info("ReflectionHelper.getGetter failed for " + name);
            ex.printStackTrace();
        }
        return null;
    }

    public static MethodHandle getSetter(Class<?> clazz, String name) {
        try {
            return LOOKUP.unreflectSetter(getField(clazz, name));
        }
        catch (Exception ex) {
            DenizenVelocity.instance.logger.info("ReflectionHelper.getSetter failed for " + name);
            ex.printStackTrace();
        }
        return null;
    }

    private static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
}

package com.gtceuterminal.common.compat;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ExpandableMultiblock {

    private static final String[] BOX_FIELDS = {"lDist", "rDist", "bDist", "fDist", "hDist"};
    private static final Field[] NONE = new Field[0];
    private static final Map<Class<?>, Field[]> CACHE = new ConcurrentHashMap<>();

    private ExpandableMultiblock() {
    }

    public static Restore applySize(Object machine, int radius, int height) {
        if (machine == null || (radius <= 0 && height <= 0)) return null;
        Field[] fields = resolve(machine.getClass());
        if (fields.length != BOX_FIELDS.length) return null;
        try {
            int[] original = new int[fields.length];
            for (int i = 0; i < fields.length; i++) original[i] = fields[i].getInt(machine);
            if (radius > 0) {
                fields[0].setInt(machine, radius);
                fields[1].setInt(machine, radius);
                fields[2].setInt(machine, radius);
                fields[3].setInt(machine, radius);
            }
            if (height > 0) fields[4].setInt(machine, height);
            return new Restore(machine, fields, original);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Field[] resolve(Class<?> cls) {
        return CACHE.computeIfAbsent(cls, ExpandableMultiblock::lookup);
    }

    private static Field[] lookup(Class<?> cls) {
        Field[] found = new Field[BOX_FIELDS.length];
        for (int i = 0; i < BOX_FIELDS.length; i++) {
            Field f = findField(cls, BOX_FIELDS[i]);
            if (f == null || f.getType() != int.class) return NONE;
            f.setAccessible(true);
            found[i] = f;
        }
        return found;
    }

    private static Field findField(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    public static final class Restore {
        private final Object machine;
        private final Field[] fields;
        private final int[] original;

        private Restore(Object machine, Field[] fields, int[] original) {
            this.machine = machine;
            this.fields = fields;
            this.original = original;
        }

        public void close() {
            try {
                for (int i = 0; i < fields.length; i++) fields[i].setInt(machine, original[i]);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}

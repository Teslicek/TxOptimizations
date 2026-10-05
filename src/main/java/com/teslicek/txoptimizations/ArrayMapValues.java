package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class ArrayMapValues {

    private static final VarHandle VALUES = find("value", Object[].class);
    private static final VarHandle SIZE   = find("size", int.class);

    private ArrayMapValues() {
    }

    public static Object[] values(Object2ObjectArrayMap<?, ?> map) {
        return (Object[]) VALUES.get(map);
    }

    public static int size(Object2ObjectArrayMap<?, ?> map) {
        return (int) SIZE.get(map);
    }

    private static VarHandle find(String name, Class<?> type) {
        try {
            return MethodHandles.privateLookupIn(Object2ObjectArrayMap.class, MethodHandles.lookup()).findVarHandle(Object2ObjectArrayMap.class, name, type);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Object2ObjectArrayMap field " + name + " not found", exception);
        }
    }
}

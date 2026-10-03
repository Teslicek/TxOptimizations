package com.teslicek.txoptimizations;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;

public final class EntityCullOrder {

    private static final Class<?>[] PARAMETERS = {Entity.class, Frustum.class, double.class, double.class, double.class, float.class};
    private static final String     METHOD     = findShouldRender();

    private static final ClassValue<Boolean> INHERITED = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod(METHOD, PARAMETERS).getDeclaringClass() == EntityRenderer.class;
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Entity renderer " + type.getName() + " has no shouldRender method", exception);
            }
        }
    };

    private static boolean deferring;

    private EntityCullOrder() {
    }

    public static boolean canDefer(EntityRenderer<?, ?> renderer) {
        return INHERITED.get(renderer.getClass());
    }

    public static boolean isDeferring() {
        return deferring;
    }

    public static void setDeferring(boolean value) {
        deferring = value;
    }

    private static String findShouldRender() {
        Method[] matches = Arrays.stream(EntityRenderer.class.getDeclaredMethods())
            .filter(method -> method.getReturnType() == boolean.class && Arrays.equals(method.getParameterTypes(), PARAMETERS))
            .toArray(Method[]::new);

        if (matches.length != 1)
            throw new IllegalStateException("Expected one EntityRenderer.shouldRender method, found " + matches.length);

        return matches[0].getName();
    }
}

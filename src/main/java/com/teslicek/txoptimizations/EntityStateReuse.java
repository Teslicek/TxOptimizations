package com.teslicek.txoptimizations;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

public final class EntityStateReuse {

    private static final String             VANILLA_PACKAGE   = "net.minecraft.";
    private static final VarHandle          FABRIC_DATA       = fabricData();
    private static final ClassValue<Boolean> VANILLA_RENDERERS = new ClassValue<>() {

        @Override
        protected Boolean computeValue(Class<?> type) {
            return type.getName().startsWith(VANILLA_PACKAGE);
        }
    };

    private static boolean worldEntity;

    private EntityStateReuse() {
    }

    public static void beginWorldEntity() {
        worldEntity = true;
    }

    public static void endWorldEntity() {
        worldEntity = false;
    }

    public static EntityRenderState stateFor(EntityRenderer<?, ?> renderer, Entity entity) {
        boolean reuse = worldEntity && VANILLA_RENDERERS.get(renderer.getClass());

        worldEntity = false;

        if (!reuse)
            return renderer.createRenderState();

        ReusedEntityState holder = (ReusedEntityState) entity;
        EntityRenderState state  = holder.txoptimizations$reusedState(renderer);

        if (state == null) {
            state = renderer.createRenderState();
            holder.txoptimizations$keepState(renderer, state);

            return state;
        }

        Map<?, ?> fabricData = (Map<?, ?>) FABRIC_DATA.get(state);

        if (fabricData != null)
            fabricData.clear();

        state.nameTagAttachment = null;

        return state;
    }

    private static VarHandle fabricData() {
        try {
            return MethodHandles.privateLookupIn(EntityRenderState.class, MethodHandles.lookup()).findVarHandle(EntityRenderState.class, "renderStateData", Map.class);
        } catch (ReflectiveOperationException missing) {
            throw new IllegalStateException("Fabric's render state data field is missing from EntityRenderState", missing);
        }
    }
}

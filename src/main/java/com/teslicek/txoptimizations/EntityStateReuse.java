package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

public final class EntityStateReuse {

    private static final String              VANILLA_PACKAGE   = "net.minecraft.";
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

        state.nameTagAttachment = null;

        return state;
    }
}

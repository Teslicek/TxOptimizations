package com.teslicek.txoptimizations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class NameTags {

    private NameTags() {
    }

    public static boolean maySeeName(Entity entity) {
        return entity.shouldShowName() || entity.hasCustomName() && entity == Minecraft.getInstance().getEntityRenderDispatcher().crosshairPickEntity;
    }
}

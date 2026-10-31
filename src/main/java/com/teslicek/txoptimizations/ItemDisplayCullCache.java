package com.teslicek.txoptimizations;

import net.minecraft.world.entity.Display;

public interface ItemDisplayCullCache {

    boolean txoptimizations$matches(Display.RenderState renderState, Display.ItemDisplay.ItemRenderState itemState, Object models);

    float txoptimizations$radius();

    void txoptimizations$remember(Display.RenderState renderState, Display.ItemDisplay.ItemRenderState itemState, Object models, float radius);

    void txoptimizations$forget();
}

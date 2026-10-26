package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.world.entity.Display;

public interface ItemDisplayCullCache {

    boolean txoptimizations$matches(Display.RenderState renderState, Display.ItemDisplay.ItemRenderState itemState, ItemModel model);

    float txoptimizations$radius();

    void txoptimizations$remember(Display.RenderState renderState, Display.ItemDisplay.ItemRenderState itemState, ItemModel model, float radius);

    void txoptimizations$forget();
}

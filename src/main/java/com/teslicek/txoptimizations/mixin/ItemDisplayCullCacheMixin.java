package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemDisplayCullCache;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Display.ItemDisplay.class)
public abstract class ItemDisplayCullCacheMixin implements ItemDisplayCullCache {

    @Unique
    private Display.RenderState txoptimizations$cullRenderState;

    @Unique
    private Display.ItemDisplay.ItemRenderState txoptimizations$cullItemState;

    @Unique
    private Object txoptimizations$cullModels;

    @Unique
    private float txoptimizations$cullRadius;

    @Override
    public boolean txoptimizations$matches(Display.RenderState renderState, Display.ItemDisplay.ItemRenderState itemState, Object models) {
        return this.txoptimizations$cullModels == models && this.txoptimizations$cullRenderState == renderState && this.txoptimizations$cullItemState == itemState;
    }

    @Override
    public float txoptimizations$radius() {
        return this.txoptimizations$cullRadius;
    }

    @Override
    public void txoptimizations$remember(Display.RenderState renderState, Display.ItemDisplay.ItemRenderState itemState, Object models, float radius) {
        this.txoptimizations$cullRenderState = renderState;
        this.txoptimizations$cullItemState   = itemState;
        this.txoptimizations$cullModels      = models;
        this.txoptimizations$cullRadius      = radius;
    }

    @Override
    public void txoptimizations$forget() {
        this.txoptimizations$cullRenderState = null;
        this.txoptimizations$cullItemState   = null;
        this.txoptimizations$cullModels      = null;
    }
}

package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemDisplayCullCache;
import com.teslicek.txoptimizations.ItemDisplayItemState;
import com.teslicek.txoptimizations.RenderStateBuffers;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Display.ItemDisplay.class)
public abstract class DisplayItemDisplayMixin implements ItemDisplayCullCache, ItemDisplayItemState {

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

    @Unique
    private final ItemStackRenderState[] txoptimizations$itemStates = new ItemStackRenderState[2];

    @Unique
    private final Object[] txoptimizations$resolvedItems = new Object[2];

    @Unique
    private final Object[] txoptimizations$resolvedLevels = new Object[2];

    @Unique
    private final Object[] txoptimizations$resolvedModels = new Object[2];

    @Override
    public ItemStackRenderState txoptimizations$reusedItemState() {
        int parity = RenderStateBuffers.parity();

        if (this.txoptimizations$itemStates[parity] == null)
            this.txoptimizations$itemStates[parity] = new ItemStackRenderState();

        return this.txoptimizations$itemStates[parity];
    }

    @Override
    public boolean txoptimizations$resolvedFor(Object item, Object level, Object models) {
        int parity = RenderStateBuffers.parity();

        return item == this.txoptimizations$resolvedItems[parity] && level == this.txoptimizations$resolvedLevels[parity] && models == this.txoptimizations$resolvedModels[parity];
    }

    @Override
    public void txoptimizations$setResolved(Object item, Object level, Object models, boolean pure) {
        int parity = RenderStateBuffers.parity();

        this.txoptimizations$resolvedItems[parity]  = pure ? item : null;
        this.txoptimizations$resolvedLevels[parity] = pure ? level : null;
        this.txoptimizations$resolvedModels[parity] = pure ? models : null;
    }
}

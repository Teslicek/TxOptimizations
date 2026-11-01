package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemDisplayItemState;
import com.teslicek.txoptimizations.RenderStateBuffers;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Display.ItemDisplay.class)
public abstract class ItemDisplayItemStateMixin implements ItemDisplayItemState {

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

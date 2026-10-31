package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemDisplayItemState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Display;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Display.ItemDisplay.class)
public abstract class ItemDisplayItemStateMixin implements ItemDisplayItemState {

    @Unique
    private ItemStackRenderState txoptimizations$itemState;

    @Unique
    private @Nullable Object txoptimizations$resolvedItem;

    @Unique
    private @Nullable Object txoptimizations$resolvedLevel;

    @Unique
    private @Nullable Object txoptimizations$resolvedModels;

    @Override
    public ItemStackRenderState txoptimizations$reusedItemState() {
        if (this.txoptimizations$itemState == null)
            this.txoptimizations$itemState = new ItemStackRenderState();

        return this.txoptimizations$itemState;
    }

    @Override
    public boolean txoptimizations$resolvedFor(Object item, Object level, Object models) {
        return item == this.txoptimizations$resolvedItem && level == this.txoptimizations$resolvedLevel && models == this.txoptimizations$resolvedModels;
    }

    @Override
    public void txoptimizations$setResolved(Object item, Object level, Object models, boolean pure) {
        this.txoptimizations$resolvedItem   = pure ? item : null;
        this.txoptimizations$resolvedLevel  = pure ? level : null;
        this.txoptimizations$resolvedModels = pure ? models : null;
    }
}

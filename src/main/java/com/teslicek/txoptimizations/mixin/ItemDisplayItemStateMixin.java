package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemDisplayItemState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Display.ItemDisplay.class)
public abstract class ItemDisplayItemStateMixin implements ItemDisplayItemState {

    @Unique
    private ItemStackRenderState txoptimizations$itemState;

    @Override
    public ItemStackRenderState txoptimizations$reusedItemState() {
        if (this.txoptimizations$itemState == null)
            this.txoptimizations$itemState = new ItemStackRenderState();

        return this.txoptimizations$itemState;
    }
}

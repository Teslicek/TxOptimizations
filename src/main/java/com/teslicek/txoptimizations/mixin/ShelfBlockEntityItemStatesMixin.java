package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ShelfItemStates;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ShelfBlockEntity.class)
public abstract class ShelfBlockEntityItemStatesMixin implements ShelfItemStates {

    @Unique
    private ItemStackRenderState[] txoptimizations$itemStates;

    @Override
    public ItemStackRenderState txoptimizations$itemState(int slot, int slots) {
        ItemStackRenderState[] states = this.txoptimizations$itemStates;

        if (states == null || states.length != slots) {
            states                          = new ItemStackRenderState[slots];
            this.txoptimizations$itemStates = states;
        }

        ItemStackRenderState state = states[slot];

        if (state == null) {
            state        = new ItemStackRenderState();
            states[slot] = state;
        }

        return state;
    }
}

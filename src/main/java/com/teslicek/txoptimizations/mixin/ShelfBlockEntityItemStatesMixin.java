package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ShelfItemStates;
import com.teslicek.txoptimizations.RenderStateBuffers;
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
        int                    index  = slot * 2 + RenderStateBuffers.parity();

        if (states == null || states.length != slots * 2) {
            states                          = new ItemStackRenderState[slots * 2];
            this.txoptimizations$itemStates = states;
        }

        ItemStackRenderState state = states[index];

        if (state == null) {
            state         = new ItemStackRenderState();
            states[index] = state;
        }

        return state;
    }
}

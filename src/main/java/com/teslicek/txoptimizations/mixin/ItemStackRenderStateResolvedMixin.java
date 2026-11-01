package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ResolvedItemState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateResolvedMixin implements ResolvedItemState {

    @Unique
    private @Nullable ItemStack txoptimizations$stack;

    @Unique
    private int txoptimizations$version;

    @Unique
    private int txoptimizations$count;

    @Unique
    private @Nullable ItemDisplayContext txoptimizations$context;

    @Unique
    private @Nullable Level txoptimizations$level;

    @Unique
    private @Nullable ItemOwner txoptimizations$owner;

    @Unique
    private int txoptimizations$seed;

    @Unique
    private @Nullable Object txoptimizations$models;

    @Inject(method = "clear", at = @At("HEAD"))
    private void txoptimizations$forgetOnClear(CallbackInfo ci) {
        this.txoptimizations$forget();
    }

    @Inject(method = "newLayer", at = @At("HEAD"))
    private void txoptimizations$forgetOnNewLayer(CallbackInfoReturnable<ItemStackRenderState.LayerRenderState> cir) {
        this.txoptimizations$forget();
    }

    @Override
    public boolean txoptimizations$resolvedFor(ItemStack stack, int version, ItemDisplayContext context, Level level, ItemOwner owner, int seed, Object models) {
        return this.txoptimizations$stack == stack && this.txoptimizations$version == version && this.txoptimizations$count == stack.getCount() && this.txoptimizations$context == context && this.txoptimizations$level == level && this.txoptimizations$owner == owner && this.txoptimizations$seed == seed && this.txoptimizations$models == models;
    }

    @Override
    public void txoptimizations$setResolved(ItemStack stack, int version, ItemDisplayContext context, Level level, ItemOwner owner, int seed, Object models) {
        this.txoptimizations$stack   = stack;
        this.txoptimizations$version = version;
        this.txoptimizations$count   = stack.getCount();
        this.txoptimizations$context = context;
        this.txoptimizations$level   = level;
        this.txoptimizations$owner   = owner;
        this.txoptimizations$seed    = seed;
        this.txoptimizations$models  = models;
    }

    @Unique
    private void txoptimizations$forget() {
        this.txoptimizations$stack  = null;
        this.txoptimizations$level  = null;
        this.txoptimizations$owner  = null;
        this.txoptimizations$models = null;
    }
}

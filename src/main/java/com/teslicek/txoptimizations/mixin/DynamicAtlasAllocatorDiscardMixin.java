package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.gui.render.DynamicAtlasAllocator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicAtlasAllocator.class)
public abstract class DynamicAtlasAllocatorDiscardMixin {

    @Unique
    private boolean txoptimizations$discardPending;

    @ModifyVariable(method = "getOrAllocate", at = @At("HEAD"), argsOnly = true)
    private boolean txoptimizations$noteDiscard(boolean discardAfterFrame) {
        if (discardAfterFrame)
            this.txoptimizations$discardPending = true;

        return discardAfterFrame;
    }

    @Inject(method = "endFrame", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipCleanSweep(CallbackInfo ci) {
        if (!this.txoptimizations$discardPending) {
            ci.cancel();

            return;
        }

        this.txoptimizations$discardPending = false;
    }
}

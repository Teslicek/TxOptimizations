package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.PendingSections;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSection.class, remap = false)
public abstract class RenderSectionPendingCountMixin {

    @Unique
    private boolean txoptimizations$counted;

    @Inject(method = "setPendingUpdate", at = @At("HEAD"))
    private void txoptimizations$countPending(int type, long now, CallbackInfo ci) {
        this.txoptimizations$setCounted(type != 0);
    }

    @Inject(method = {"clearPendingUpdate", "delete"}, at = @At("HEAD"))
    private void txoptimizations$uncountPending(CallbackInfo ci) {
        this.txoptimizations$setCounted(false);
    }

    @Unique
    private void txoptimizations$setCounted(boolean counted) {
        if (counted == this.txoptimizations$counted)
            return;

        this.txoptimizations$counted = counted;

        if (counted)
            PendingSections.add();
        else
            PendingSections.remove();
    }
}

package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PatchedDataComponentMap.class)
public abstract class PatchedDataComponentMapEmptyPatchMixin {

    @Shadow
    private Reference2ObjectMap<DataComponentType<?>, Object> patch;

    @Shadow
    private boolean copyOnWrite;

    @Inject(method = {"applyPatch(Lnet/minecraft/core/component/DataComponentPatch;)V", "restorePatch", "clearPatch"}, at = @At("RETURN"))
    private void txoptimizations$shareEmptyPatch(CallbackInfo ci) {
        this.txoptimizations$shareIfEmpty();
    }

    @Inject(method = {"set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;", "remove"}, at = @At("RETURN"))
    private void txoptimizations$shareEmptyPatchAfterChange(CallbackInfoReturnable<?> cir) {
        this.txoptimizations$shareIfEmpty();
    }

    @Unique
    private void txoptimizations$shareIfEmpty() {
        if (!this.patch.isEmpty())
            return;

        this.patch       = Reference2ObjectMaps.emptyMap();
        this.copyOnWrite = true;
    }
}

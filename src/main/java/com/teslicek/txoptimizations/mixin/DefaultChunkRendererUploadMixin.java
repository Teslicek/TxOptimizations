package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.IndirectCommandUpload;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DefaultChunkRenderer.class)
public abstract class DefaultChunkRendererUploadMixin {

    @Shadow
    @Final
    private DrawContext drawContext;

    @Inject(method = "prepare", at = @At("RETURN"))
    private void txoptimizations$uploadDrawCommands(CallbackInfo ci) {
        ((IndirectCommandUpload) this.drawContext).txoptimizations$uploadCommands();
    }
}

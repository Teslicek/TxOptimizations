package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TransientBlockHandOff;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.TransientBlockRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorTransientBlockMixin {

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addTransientBlock(JLnet/minecraft/client/renderer/state/level/TransientBlockRenderState;)V"), require = 1)
    private void txoptimizations$handTransientBlockToFrame(LevelRenderer renderer, long blockNode, TransientBlockRenderState state) {
        TransientBlockHandOff.add(blockNode, state);
    }
}

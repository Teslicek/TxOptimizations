package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teslicek.txoptimizations.RenderStateBuffers;
import com.teslicek.txoptimizations.TransientBlockHandOff;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.TransientBlockRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererTransientBlockMixin {

    @Shadow
    public abstract void addTransientBlock(long blockNode, TransientBlockRenderState state);

    @Inject(method = "submitTransientBlocks", at = @At("HEAD"), require = 1)
    private void txoptimizations$addFrameTransientBlocks(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
        List<TransientBlockHandOff.Pending> pending = TransientBlockHandOff.drainFor(RenderStateBuffers.renderParity());

        for (TransientBlockHandOff.Pending block : pending)
            this.addTransientBlock(block.blockNode(), block.state());

        pending.clear();
    }
}

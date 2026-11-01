package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teslicek.txoptimizations.FrameQueues;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.TransientBlockRenderState;
import net.minecraft.gizmos.SimpleGizmoCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererFrameQueueMixin {

    @Shadow
    public abstract void addTransientBlock(long blockNode, TransientBlockRenderState state);

    @Shadow
    public abstract void addMainThreadGizmos(List<SimpleGizmoCollector.GizmoInstance> gizmos);

    @Inject(method = "submitTransientBlocks", at = @At("HEAD"), require = 1)
    private void txoptimizations$addFrameTransientBlocks(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
        FrameQueues.TRANSIENT_BLOCKS.drain(block -> this.addTransientBlock(block.blockNode(), block.state()));
    }

    @Inject(method = "finalizeGizmoCollection", at = @At("HEAD"), require = 1)
    private void txoptimizations$addFrameMainThreadGizmos(CallbackInfo ci) {
        FrameQueues.MAIN_THREAD_GIZMOS.drain(this::addMainThreadGizmos);
    }
}

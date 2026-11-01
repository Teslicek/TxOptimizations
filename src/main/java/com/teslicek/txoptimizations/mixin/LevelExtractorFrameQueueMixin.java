package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.FrameQueues;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.TransientBlockRenderState;
import net.minecraft.gizmos.SimpleGizmoCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorFrameQueueMixin {

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addTransientBlock(JLnet/minecraft/client/renderer/state/level/TransientBlockRenderState;)V"), require = 1)
    private void txoptimizations$queueTransientBlock(LevelRenderer renderer, long blockNode, TransientBlockRenderState state) {
        FrameQueues.TRANSIENT_BLOCKS.add(new FrameQueues.TransientBlock(blockNode, state));
    }

    @Redirect(method = "extractGizmos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainThreadGizmos(Ljava/util/List;)V"), require = 1)
    private void txoptimizations$queueMainThreadGizmos(LevelRenderer renderer, List<SimpleGizmoCollector.GizmoInstance> gizmos) {
        FrameQueues.MAIN_THREAD_GIZMOS.add(gizmos);
    }
}

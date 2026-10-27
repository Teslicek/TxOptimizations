package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BlockModelCopy;
import com.teslicek.txoptimizations.DisplayBlockModels;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockModelResolver.class)
public abstract class BlockModelResolverSharedModelMixin {

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$copyPreparedModel(BlockModelRenderState renderState, BlockState blockState, BlockDisplayContext displayContext, CallbackInfo ci) {
        if (DisplayBlockModels.isPreparing() || !((BlockModelCopy) renderState).txoptimizations$copyable())
            return;

        BlockModelRenderState prepared = DisplayBlockModels.prepared((BlockModelResolver) (Object) this, blockState, displayContext);

        if (prepared == null)
            return;

        ((BlockModelCopy) renderState).txoptimizations$copyFrom(prepared);

        ci.cancel();
    }
}

package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.FrameModels;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererFrameModelMixin {

    @Redirect(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockModelResolver;updateForItemFrame(Lnet/minecraft/client/renderer/block/BlockModelRenderState;ZZ)V"), require = 1)
    private void txoptimizations$sharePreparedFrameModel(BlockModelResolver resolver, BlockModelRenderState renderState, boolean glowing, boolean map, @Local(argsOnly = true) ItemFrameRenderState state) {
        BlockModelRenderState prepared = FrameModels.prepared(resolver, glowing, map);

        if (prepared == null) {
            resolver.updateForItemFrame(txoptimizations$ownFrameModel(state), glowing, map);
            return;
        }

        state.frameModel = prepared;
    }

    @Redirect(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockModelRenderState;clear()V"), require = 1)
    private void txoptimizations$clearOwnFrameModel(BlockModelRenderState renderState, @Local(argsOnly = true) ItemFrameRenderState state) {
        txoptimizations$ownFrameModel(state).clear();
    }

    @Unique
    private static BlockModelRenderState txoptimizations$ownFrameModel(ItemFrameRenderState state) {
        if (FrameModels.isShared(state.frameModel))
            state.frameModel = new BlockModelRenderState();

        return state.frameModel;
    }
}

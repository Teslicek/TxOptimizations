package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.ModelCopies;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherModelCopiesMixin {

    @Shadow
    private Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> renderers;

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void txoptimizations$copyModels(ResourceManager resourceManager, CallbackInfo ci, @Local BlockEntityRendererProvider.Context context) {
        ModelCopies.replace(ModelCopies.Source.BLOCK_ENTITIES, this.renderers, BlockEntityRenderers.createEntityRenderers(context));
    }
}

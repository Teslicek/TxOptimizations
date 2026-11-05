package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.teslicek.txoptimizations.CountText;
import com.teslicek.txoptimizations.GuiItemCache;
import com.teslicek.txoptimizations.MapAtlas;
import com.teslicek.txoptimizations.MapAtlasRenderState;
import com.teslicek.txoptimizations.TextLayoutCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {

    @WrapOperation(method = "map", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;innerBlit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuSampler;IIIIFFFFI)V", ordinal = 0))
    private void txoptimizations$blitFromAtlas(GuiGraphicsExtractor graphics, RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color, Operation<Void> original, @Local(name = "mapRenderState", argsOnly = true) MapRenderState mapRenderState) {
        MapAtlasRenderState state = (MapAtlasRenderState) mapRenderState;

        if (state.txoptimizations$getAtlasTexture() == null || !state.txoptimizations$getAtlasTexture().equals(mapRenderState.texture)) {
            original.call(graphics, pipeline, textureView, sampler, x0, y0, x1, y1, u0, u1, v0, v1, color);

            return;
        }

        float atlasU = state.txoptimizations$getAtlasU();
        float atlasV = state.txoptimizations$getAtlasV();

        original.call(graphics, pipeline, textureView, sampler, x0, y0, x1, y1, atlasU, atlasU + MapAtlas.MAP_UV, atlasV, atlasV + MapAtlas.MAP_UV, color);
    }

    @Redirect(method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/locale/Language;getVisualOrder(Lnet/minecraft/network/chat/FormattedText;)Lnet/minecraft/util/FormattedCharSequence;"))
    private FormattedCharSequence txoptimizations$sharedOrder(Language language, FormattedText text, Font font, String str) {
        return TextLayoutCache.visualOrder(language, str);
    }

    @WrapOperation(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;"), require = 1)
    private TrackingItemStackRenderState txoptimizations$cachedState(Operation<TrackingItemStackRenderState> original, @Local(argsOnly = true) LivingEntity owner, @Local(argsOnly = true) Level level, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true, ordinal = 2) int seed) {
        return GuiItemCache.state(owner, level, stack, seed);
    }

    @WrapOperation(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V"), require = 1)
    private void txoptimizations$resolveOnce(ItemModelResolver resolver, ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int seed, Operation<Void> original) {
        GuiItemCache.resolve(resolver, (TrackingItemStackRenderState) state, stack, context, level, owner, seed);
    }

    @Redirect(method = "itemCount", at = @At(value = "INVOKE", target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;"), require = 1)
    private String txoptimizations$sharedCount(int count) {
        return CountText.of(count);
    }
}

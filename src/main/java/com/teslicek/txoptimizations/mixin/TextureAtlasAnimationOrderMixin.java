package com.teslicek.txoptimizations.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasAnimationOrderMixin {

    @ModifyExpressionValue(method = "upload", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList$Builder;build()Lcom/google/common/collect/ImmutableList;", ordinal = 1))
    private ImmutableList<SpriteContents.AnimationState> txoptimizations$groupByPipeline(ImmutableList<SpriteContents.AnimationState> states) {
        ImmutableList.Builder<SpriteContents.AnimationState> grouped = ImmutableList.builderWithExpectedSize(states.size());

        for (SpriteContents.AnimationState state : states) {
            if (state.animationInfo.interpolateFrames)
                grouped.add(state);
        }

        for (SpriteContents.AnimationState state : states) {
            if (!state.animationInfo.interpolateFrames)
                grouped.add(state);
        }

        return grouped.build();
    }
}

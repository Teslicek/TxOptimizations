package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteContents.AnimatedTexture.class)
public abstract class AnimatedTextureSharedFramesMixin {

    @Shadow
    @Final
    SpriteContents this$0;

    @Overwrite
    public SpriteContents.AnimationState createAnimationState(GpuBufferSlice uboSlice, int spriteUboSize) {
        GpuBufferSlice[] spriteUbosByMip = new GpuBufferSlice[this.this$0.byMipLevel.length];

        for (int level = 0; level < spriteUbosByMip.length; level ++)
            spriteUbosByMip[level] = uboSlice.slice((long) level * spriteUboSize, spriteUboSize);

        return this.this$0.new AnimationState((SpriteContents.AnimatedTexture) (Object) this, Int2ObjectMaps.emptyMap(), spriteUbosByMip);
    }
}

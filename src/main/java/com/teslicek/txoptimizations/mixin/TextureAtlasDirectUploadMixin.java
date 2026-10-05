package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AtlasSpriteCopy;
import java.util.List;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasDirectUploadMixin {

    @Shadow
    private List<TextureAtlasSprite> sprites;

    @Shadow
    private int width;

    @Shadow
    private int height;

    @Shadow
    private int mipLevelCount;

    @Shadow
    protected abstract void uploadAnimationFrames();

    @Overwrite
    private void uploadInitialContents() {
        AtlasSpriteCopy.upload(((AbstractTexture) (Object) this).getTexture(), this.width, this.height, this.mipLevelCount, this.sprites);
        this.uploadAnimationFrames();
    }
}

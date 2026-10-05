package com.teslicek.txoptimizations;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.teslicek.txoptimizations.mixin.SpriteContentsAccessor;
import com.teslicek.txoptimizations.mixin.TextureAtlasSpriteAccessor;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.lwjgl.system.MemoryUtil;

public final class AtlasSpriteCopy {

    private static final int  TEXEL_BYTES   = 4;
    private static final long ALIGNMENT     = 4L;
    private static final int  COPY_SRC      = 16;

    private AtlasSpriteCopy() {
    }

    public static void upload(GpuTexture atlas, int atlasWidth, int atlasHeight, int mipLevelCount, List<TextureAtlasSprite> sprites) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        for (int level = 0; level < mipLevelCount; level ++) {
            int  levelWidth  = atlasWidth >> level;
            int  levelHeight = atlasHeight >> level;
            long size        = (long) levelWidth * levelHeight * TEXEL_BYTES;

            try (GpuBufferSlice.MappedView staging = encoder.transientMemory().allocateStaging(size, ALIGNMENT, COPY_SRC)) {
                long base = MemoryUtil.memAddress(staging.data());

                MemoryUtil.memSet(base, 0, size);

                for (TextureAtlasSprite sprite : sprites) {
                    if (!sprite.isAnimated())
                        writeSprite(base, levelWidth, levelHeight, sprite, level);
                }

                encoder.copyBufferToTexture(staging.slice(), 0, 0, levelWidth, levelHeight, atlas, 0, 0, levelWidth, levelHeight, level, 0);
            }
        }
    }

    private static void writeSprite(long base, int levelWidth, int levelHeight, TextureAtlasSprite sprite, int level) {
        NativeImage image   = ((SpriteContentsAccessor) sprite.contents()).txoptimizations$byMipLevel()[level];
        int         width   = sprite.contents().width() >> level;
        int         height  = sprite.contents().height() >> level;
        int         padding = ((TextureAtlasSpriteAccessor) sprite).txoptimizations$padding() >> level;
        int         left    = sprite.getX() >> level;
        int         top     = sprite.getY() >> level;

        if (image.format().components() != TEXEL_BYTES || image.getWidth() != width || image.getHeight() != height)
            throw new IllegalStateException("Sprite " + sprite.contents().name() + " mip level " + level + " is " + image.getWidth() + "x" + image.getHeight() + " with " + image.format().components() + " channels, expected " + width + "x" + height + " RGBA");

        if (left + width + padding * 2 > levelWidth || top + height + padding * 2 > levelHeight)
            throw new IllegalStateException("Sprite " + sprite.contents().name() + " mip level " + level + " does not fit its atlas level " + levelWidth + "x" + levelHeight);

        long source   = image.getPointer();
        long rowBytes = (long) width * TEXEL_BYTES;

        for (int row = 0; row < height + padding * 2; row ++) {
            long sourceRow      = source + Math.clamp(row - padding, 0, height - 1) * rowBytes;
            long destinationRow = base + ((long) (top + row) * levelWidth + left) * TEXEL_BYTES;
            int  firstTexel     = MemoryUtil.memGetInt(sourceRow);
            int  lastTexel      = MemoryUtil.memGetInt(sourceRow + rowBytes - TEXEL_BYTES);

            for (int column = 0; column < padding; column ++) {
                MemoryUtil.memPutInt(destinationRow + (long) column * TEXEL_BYTES, firstTexel);
                MemoryUtil.memPutInt(destinationRow + (long) (padding + width + column) * TEXEL_BYTES, lastTexel);
            }

            MemoryUtil.memCopy(sourceRow, destinationRow + (long) padding * TEXEL_BYTES, rowBytes);
        }
    }
}

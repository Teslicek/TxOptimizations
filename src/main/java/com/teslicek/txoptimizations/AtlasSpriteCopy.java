package com.teslicek.txoptimizations;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTexture;
import com.teslicek.txoptimizations.mixin.FrontendGpuDeviceAccessor;
import com.teslicek.txoptimizations.mixin.SpriteContentsAccessor;
import com.teslicek.txoptimizations.mixin.TextureAtlasSpriteAccessor;
import com.teslicek.txoptimizations.mixin.VulkanCommandEncoderBufferInvoker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageBlit;

public final class AtlasSpriteCopy {

    private static final int  TEXEL_BYTES    = 4;
    private static final long ALIGNMENT      = 4L;
    private static final int  COPY_SRC       = 16;
    private static final long STAGING_BYTES  = 262_144L;
    private static final int  LAYOUT_GENERAL = VK10.VK_IMAGE_LAYOUT_GENERAL;
    private static final int  COLOR_ASPECT   = VK10.VK_IMAGE_ASPECT_COLOR_BIT;
    private static final int  BORDER_BLITS   = 8;

    private AtlasSpriteCopy() {
    }

    public static void upload(GpuTexture atlas, int atlasWidth, int atlasHeight, int mipLevelCount, List<TextureAtlasSprite> sprites) {
        VulkanDevice         device  = (VulkanDevice) ((FrontendGpuDeviceAccessor) RenderSystem.getDevice()).txoptimizations$backend();
        VulkanCommandEncoder encoder = device.createCommandEncoder();
        VkCommandBuffer      buffer  = ((VulkanCommandEncoderBufferInvoker) encoder).txoptimizations$commandBuffer();
        long                 image   = ((VulkanGpuTexture) atlas).vkImage();
        List<Placement>      placed  = new ArrayList<>();

        for (TextureAtlasSprite sprite : sprites) {
            if (!sprite.isAnimated())
                placed.add(Placement.of(sprite, atlasWidth, atlasHeight, mipLevelCount));
        }

        for (int level = 0; level < mipLevelCount; level ++)
            copyInteriors(encoder, buffer, image, placed, level);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanCommandEncoder.memoryBarrier(buffer, stack);
        }

        for (int level = 0; level < mipLevelCount; level ++)
            blitBorders(buffer, image, placed, level);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanCommandEncoder.memoryBarrier(buffer, stack);
        }
    }

    private static void copyInteriors(VulkanCommandEncoder encoder, VkCommandBuffer buffer, long image, List<Placement> placed, int level) {
        int start = 0;

        while (start < placed.size()) {
            long bytes = placed.get(start).bytes(level);
            int  end   = start + 1;

            while (end < placed.size() && bytes + placed.get(end).bytes(level) <= STAGING_BYTES) {
                bytes += placed.get(end).bytes(level);
                end ++;
            }

            copyChunk(encoder, buffer, image, placed.subList(start, end), level, bytes);
            start = end;
        }
    }

    private static void copyChunk(VulkanCommandEncoder encoder, VkCommandBuffer buffer, long image, List<Placement> chunk, int level, long bytes) {
        try (GpuBufferSlice.MappedView staging = encoder.transientMemory().allocateStaging(bytes, ALIGNMENT, COPY_SRC)) {
            GpuBufferSlice           slice   = staging.slice();
            long                     base    = MemoryUtil.memAddress(staging.data());
            VkBufferImageCopy.Buffer regions = VkBufferImageCopy.calloc(chunk.size());

            try {
                long offset = 0L;

                for (int index = 0; index < chunk.size(); index ++) {
                    Placement   placement = chunk.get(index);
                    NativeImage source    = placement.images()[level];
                    int         width     = placement.width() >> level;
                    int         height    = placement.height() >> level;

                    MemoryUtil.memCopy(source.getPointer(), base + offset, placement.bytes(level));

                    VkBufferImageCopy region = regions.get(index);

                    region.bufferOffset(slice.offset() + offset);
                    region.bufferRowLength(width);
                    region.bufferImageHeight(height);
                    region.imageSubresource().set(COLOR_ASPECT, level, 0, 1);
                    region.imageOffset().set(placement.interiorX(level), placement.interiorY(level), 0);
                    region.imageExtent().set(width, height, 1);
                    offset += placement.bytes(level);
                }

                VK10.vkCmdCopyBufferToImage(buffer, ((VulkanGpuBuffer) slice.buffer()).vkBuffer(), image, LAYOUT_GENERAL, regions);
            } finally {
                regions.free();
            }
        }
    }

    private static void blitBorders(VkCommandBuffer buffer, long image, List<Placement> placed, int level) {
        VkImageBlit.Buffer blits = VkImageBlit.calloc(placed.size() * BORDER_BLITS);

        try {
            int count = 0;

            for (Placement placement : placed) {
                int padding = placement.padding() >> level;

                if (padding == 0)
                    continue;

                int left   = placement.interiorX(level);
                int top    = placement.interiorY(level);
                int width  = placement.width() >> level;
                int height = placement.height() >> level;
                int right  = left + width;
                int bottom = top + height;

                count = blit(blits, count, level, left, top, 1, height, left - padding, top, padding, height);
                count = blit(blits, count, level, right - 1, top, 1, height, right, top, padding, height);
                count = blit(blits, count, level, left, top, width, 1, left, top - padding, width, padding);
                count = blit(blits, count, level, left, bottom - 1, width, 1, left, bottom, width, padding);
                count = blit(blits, count, level, left, top, 1, 1, left - padding, top - padding, padding, padding);
                count = blit(blits, count, level, right - 1, top, 1, 1, right, top - padding, padding, padding);
                count = blit(blits, count, level, left, bottom - 1, 1, 1, left - padding, bottom, padding, padding);
                count = blit(blits, count, level, right - 1, bottom - 1, 1, 1, right, bottom, padding, padding);
            }

            if (count == 0)
                return;

            blits.limit(count);
            VK10.vkCmdBlitImage(buffer, image, LAYOUT_GENERAL, image, LAYOUT_GENERAL, blits, VK10.VK_FILTER_NEAREST);
        } finally {
            blits.free();
        }
    }

    private static int blit(VkImageBlit.Buffer blits, int index, int level, int sourceX, int sourceY, int sourceWidth, int sourceHeight, int destinationX, int destinationY, int destinationWidth, int destinationHeight) {
        VkImageBlit blit = blits.get(index);

        blit.srcSubresource().set(COLOR_ASPECT, level, 0, 1);
        blit.srcOffsets(0).set(sourceX, sourceY, 0);
        blit.srcOffsets(1).set(sourceX + sourceWidth, sourceY + sourceHeight, 1);
        blit.dstSubresource().set(COLOR_ASPECT, level, 0, 1);
        blit.dstOffsets(0).set(destinationX, destinationY, 0);
        blit.dstOffsets(1).set(destinationX + destinationWidth, destinationY + destinationHeight, 1);

        return index + 1;
    }

    private record Placement(NativeImage[] images, int x, int y, int width, int height, int padding) {

        private static Placement of(TextureAtlasSprite sprite, int atlasWidth, int atlasHeight, int mipLevelCount) {
            NativeImage[] images  = ((SpriteContentsAccessor) sprite.contents()).txoptimizations$byMipLevel();
            int           width   = sprite.contents().width();
            int           height  = sprite.contents().height();
            int           padding = ((TextureAtlasSpriteAccessor) sprite).txoptimizations$padding();

            if (images.length < mipLevelCount)
                throw new IllegalStateException("Sprite " + sprite.contents().name() + " has " + images.length + " mip levels, the atlas needs " + mipLevelCount);

            if (sprite.getX() < 0 || sprite.getY() < 0 || sprite.getX() + width + padding * 2 > atlasWidth || sprite.getY() + height + padding * 2 > atlasHeight)
                throw new IllegalStateException("Sprite " + sprite.contents().name() + " does not fit its " + atlasWidth + "x" + atlasHeight + " atlas");

            for (int level = 0; level < mipLevelCount; level ++) {
                int step = 1 << level;

                if (width % step != 0 || height % step != 0 || padding % step != 0 || sprite.getX() % step != 0 || sprite.getY() % step != 0)
                    throw new IllegalStateException("Sprite " + sprite.contents().name() + " is not aligned to mip level " + level);

                NativeImage image = images[level];

                if (image.format().components() != TEXEL_BYTES || image.getWidth() != width >> level || image.getHeight() != height >> level)
                    throw new IllegalStateException("Sprite " + sprite.contents().name() + " mip level " + level + " is " + image.getWidth() + "x" + image.getHeight() + " with " + image.format().components() + " channels, expected " + (width >> level) + "x" + (height >> level) + " RGBA");
            }

            return new Placement(images, sprite.getX(), sprite.getY(), width, height, padding);
        }

        private int interiorX(int level) {
            return (this.x + this.padding) >> level;
        }

        private int interiorY(int level) {
            return (this.y + this.padding) >> level;
        }

        private long bytes(int level) {
            return (long) (this.width >> level) * (this.height >> level) * TEXEL_BYTES;
        }
    }
}

package com.teslicek.txoptimizations;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryUtil;

public final class AtlasAnimation implements AutoCloseable {

    private static final int SPRITES_PER_DRAW = 256;
    private static final int SPRITE_BYTES     = 12 * Integer.BYTES;
    private static final int HEADER_BYTES     = 4 * Integer.BYTES;
    private static final int LEVEL_SHIFT      = 3;
    private static final int CORNERS          = 6;

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("txoptimizations", "pipeline/animate_sprites"))
        .withVertexShader(Identifier.fromNamespaceAndPath("txoptimizations", "shaderc/animate_sprites"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("txoptimizations", "shaderc/animate_sprites"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("AnimatedSprites", UniformType.UNIFORM_BUFFER).withUniform("Frames", UniformType.COMBINED_IMAGE_SAMPLER).build())
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withColorTargetState(ColorTargetState.DEFAULT)
        .build();

    private final Supplier<String>   label;
    private final int                atlasWidth;
    private final int                atlasHeight;
    private final int                levels;
    private final int                alignment;
    private final FrameSheet[]       sheets;
    private final int[]              drawn;
    private final MappableRingBuffer uniforms;
    private final FrameSheet[]       chunkSheets;
    private final GpuBufferSlice[]   chunkSlices;
    private final int[]              chunkCounts;

    private AtlasAnimation(Identifier location, int atlasWidth, int atlasHeight, int levels, FrameSheet[] sheets) {
        int alignment = RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment();
        int chunks    = 0;
        int bytes     = 0;

        for (FrameSheet sheet : sheets) {
            for (int first = 0; first < sheet.sprites.length; first += SPRITES_PER_DRAW) {
                bytes = Mth.roundToward(bytes, alignment) + HEADER_BYTES + Math.min(SPRITES_PER_DRAW, sheet.sprites.length - first) * SPRITE_BYTES;
                chunks ++;
            }
        }

        this.label       = () -> "Animate " + location;
        this.atlasWidth  = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.levels      = levels;
        this.alignment   = alignment;
        this.sheets      = sheets;
        this.drawn       = new int[sheets.length];
        this.uniforms    = new MappableRingBuffer(() -> "Animated sprites " + location, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, bytes);
        this.chunkSheets = new FrameSheet[chunks];
        this.chunkSlices = new GpuBufferSlice[chunks];
        this.chunkCounts = new int[chunks];
    }

    public void draw(GpuTextureView[] mipViews) {
        if (mipViews.length != this.levels)
            throw new IllegalStateException("Animated sprites were uploaded for " + this.levels + " mip levels but the atlas has " + mipViews.length);

        int drawn = 0;

        for (int sheet = 0; sheet < this.sheets.length; sheet ++) {
            this.drawn[sheet] = this.sheets[sheet].drawnSprites();
            drawn            += this.drawn[sheet];
        }

        if (drawn == 0)
            return;

        this.uniforms.rotate();

        GpuBuffer target = this.uniforms.currentBuffer();
        int       chunks;

        if (target instanceof VulkanGpuBuffer.Direct) {
            chunks = this.write(((PersistentRingBuffer) this.uniforms).txoptimizations$mapCurrent(), target);
        } else {
            try (GpuBufferSlice.MappedView view = target.map(false, true)) {
                chunks = this.write(view.data(), target);
            }
        }

        CommandEncoder         encoder  = RenderSystem.getDevice().createCommandEncoder();
        CompiledRenderPipeline pipeline = RenderSystem.getCompiledPipeline(PIPELINE);
        GpuSampler             sampler  = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);

        for (int level = 0; level < this.levels; level ++) {
            try (RenderPass pass = encoder.createRenderPass(this.label, mipViews[level], Optional.empty())) {
                pass.setPipeline(pipeline);

                for (int chunk = 0; chunk < chunks; chunk ++) {
                    pass.setUniform("Frames", this.chunkSheets[chunk].view, sampler);
                    pass.setUniform("AnimatedSprites", this.chunkSlices[chunk]);
                    pass.draw(CORNERS, this.chunkCounts[chunk], level << LEVEL_SHIFT, 0);
                }
            }
        }
    }

    private int write(ByteBuffer data, GpuBuffer target) {
        int chunks = 0;
        int offset = 0;

        for (int index = 0; index < this.sheets.length; index ++) {
            FrameSheet sheet     = this.sheets[index];
            int        remaining = this.drawn[index];
            int        next      = 0;

            while (remaining > 0) {
                int  count   = Math.min(remaining, SPRITES_PER_DRAW);
                int  bytes   = HEADER_BYTES + count * SPRITE_BYTES;
                long address = NativeWrites.at(data, offset, bytes);
                int  written = 0;

                MemoryUtil.memPutInt(address, this.atlasWidth);
                MemoryUtil.memPutInt(address + 4, this.atlasHeight);
                MemoryUtil.memPutLong(address + 8, 0L);

                while (written < count) {
                    AnimatedSprite sprite = sheet.sprites[next ++];

                    if (!sprite.state.needsToDraw())
                        continue;

                    sprite.write(address + HEADER_BYTES + (long) written * SPRITE_BYTES);
                    written ++;
                }

                this.chunkSheets[chunks] = sheet;
                this.chunkSlices[chunks] = target.slice(offset, bytes);
                this.chunkCounts[chunks] = count;
                chunks ++;
                offset     = Mth.roundToward(offset + bytes, this.alignment);
                remaining -= count;
            }
        }

        return chunks;
    }

    @Override
    public void close() {
        this.uniforms.close();

        for (FrameSheet sheet : this.sheets) {
            sheet.view.close();
            sheet.texture.close();
        }
    }

    public static AtlasAnimation create(Identifier location, List<TextureAtlasSprite> sprites, List<SpriteContents.AnimationState> states, int atlasWidth, int atlasHeight, int levels) {
        List<List<Animated>> bySize = new ArrayList<>();
        int                  state  = 0;

        for (TextureAtlasSprite sprite : sprites) {
            SpriteContents contents = sprite.contents();

            if (contents.animatedTexture == null)
                continue;

            SpriteContents.AnimationState animation = states.get(state);

            if (animation.animationInfo != contents.animatedTexture)
                throw new IllegalStateException("Animation state " + state + " of atlas " + location + " does not belong to sprite " + contents.name());

            sameSize(bySize, contents).add(new Animated(sprite, animation));
            state ++;
        }

        if (state != states.size())
            throw new IllegalStateException("Atlas " + location + " has " + states.size() + " animation states for " + state + " animated sprites");

        FrameSheet[] sheets = new FrameSheet[bySize.size()];

        for (int size = 0; size < sheets.length; size ++)
            sheets[size] = FrameSheet.create(location, bySize.get(size), levels);

        return new AtlasAnimation(location, atlasWidth, atlasHeight, levels, sheets);
    }

    private static List<Animated> sameSize(List<List<Animated>> bySize, SpriteContents contents) {
        for (List<Animated> sized : bySize) {
            SpriteContents first = sized.getFirst().sprite.contents();

            if (first.width() == contents.width() && first.height() == contents.height())
                return sized;
        }

        List<Animated> sized = new ArrayList<>();

        bySize.add(sized);

        return sized;
    }

    private record Animated(TextureAtlasSprite sprite, SpriteContents.AnimationState state) {
    }

    private record FrameSheet(GpuTexture texture, GpuTextureView view, AnimatedSprite[] sprites) {

        private int drawnSprites() {
            int drawn = 0;

            for (AnimatedSprite sprite : this.sprites) {
                if (sprite.state.needsToDraw())
                    drawn ++;
            }

            return drawn;
        }

        private static FrameSheet create(Identifier location, List<Animated> sprites, int levels) {
            GpuDevice device = RenderSystem.getDevice();
            int       width  = sprites.getFirst().sprite.contents().width();
            int       height = sprites.getFirst().sprite.contents().height();
            int       frames = 0;

            for (Animated entry : sprites)
                frames += entry.sprite.contents().animatedTexture.uniqueFrames.size();

            int largest = device.getDeviceInfo().limits().maxTextureSizeForFormat(GpuFormat.RGBA8_UNORM);
            int columns = Math.ceilDiv(frames, largest / height);
            int rows    = Math.ceilDiv(frames, columns);

            if (columns > largest / width)
                throw new IllegalStateException("Atlas " + location + " has " + frames + " animation frames of " + width + "x" + height + ", more than one " + largest + " texture holds");

            GpuTexture       texture  = device.createTexture(() -> location + " animation frames " + width + "x" + height, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, GpuFormat.RGBA8_UNORM, width * columns, height * rows, 1, levels);
            CommandEncoder   encoder  = device.createCommandEncoder();
            AnimatedSprite[] animated = new AnimatedSprite[sprites.size()];
            int              slot     = 0;

            for (int index = 0; index < animated.length; index ++) {
                TextureAtlasSprite             sprite   = sprites.get(index).sprite;
                SpriteContents                 contents = sprite.contents();
                SpriteContents.AnimatedTexture info     = contents.animatedTexture;
                NativeImage[]                  images   = contents.byMipLevel;

                if (images.length != levels)
                    throw new IllegalStateException("Animated sprite " + contents.name() + " has " + images.length + " mip levels but atlas " + location + " has " + levels);

                List<ByteBuffer> pixels = new ArrayList<>(levels);

                for (NativeImage image : images)
                    pixels.add(image.getPixelBytes());

                List<GpuBufferSlice> staging = encoder.transientMemory().multiUploadStaging(pixels, 1L, GpuBuffer.USAGE_COPY_SRC);

                for (int unique = 0; unique < info.uniqueFrames.size(); unique ++) {
                    int frame = info.uniqueFrames.getInt(unique);
                    int x     = (slot + unique) % columns * width;
                    int y     = (slot + unique) / columns * height;

                    for (int level = 0; level < levels; level ++)
                        encoder.copyBufferToTexture(staging.get(level), info.getFrameX(frame) * width >> level, info.getFrameY(frame) * height >> level, images[level].getWidth(), images[level].getHeight(), texture, x >> level, y >> level, width >> level, height >> level, level, 0);
                }

                animated[index] = new AnimatedSprite(sprites.get(index).state, sprite, info, slot, columns);
                slot += info.uniqueFrames.size();
            }

            return new FrameSheet(texture, device.createTextureView(texture), animated);
        }
    }

    private static final class AnimatedSprite {

        private final SpriteContents.AnimationState state;
        private final boolean                       interpolate;
        private final int                           slotX;
        private final int                           slotY;
        private final int                           slotWidth;
        private final int                           slotHeight;
        private final int                           padding;
        private final int                           width;
        private final int                           height;
        private final int[]                         frameX;
        private final int[]                         frameY;
        private final int[]                         times;

        private AnimatedSprite(SpriteContents.AnimationState state, TextureAtlasSprite sprite, SpriteContents.AnimatedTexture info, int firstSlot, int columns) {
            List<SpriteContents.FrameInfo> frames = info.frames;

            this.state       = state;
            this.interpolate = info.interpolateFrames;
            this.slotX       = sprite.getX();
            this.slotY       = sprite.getY();
            this.padding     = sprite.padding;
            this.width       = sprite.contents().width();
            this.height      = sprite.contents().height();
            this.slotWidth   = this.width + this.padding * 2;
            this.slotHeight  = this.height + this.padding * 2;
            this.frameX      = new int[frames.size()];
            this.frameY      = new int[frames.size()];
            this.times       = new int[frames.size()];

            for (int position = 0; position < frames.size(); position ++) {
                SpriteContents.FrameInfo frame = frames.get(position);
                int                      slot  = firstSlot + info.uniqueFrames.indexOf(frame.index());

                this.frameX[position] = slot % columns * this.width;
                this.frameY[position] = slot / columns * this.height;
                this.times[position]  = frame.time();
            }
        }

        private void write(long address) {
            int position = this.state.frame;
            int next     = this.interpolate ? (position + 1) % this.times.length : position;
            int progress = this.interpolate ? (int) ((float) this.state.subFrame / this.times[position] * 1000.0F) : 0;

            MemoryUtil.memPutInt(address, this.slotX);
            MemoryUtil.memPutInt(address + 4, this.slotY);
            MemoryUtil.memPutInt(address + 8, this.slotWidth);
            MemoryUtil.memPutInt(address + 12, this.slotHeight);
            MemoryUtil.memPutInt(address + 16, this.padding);
            MemoryUtil.memPutInt(address + 20, this.width);
            MemoryUtil.memPutInt(address + 24, this.height);
            MemoryUtil.memPutInt(address + 28, progress);
            MemoryUtil.memPutInt(address + 32, this.frameX[position]);
            MemoryUtil.memPutInt(address + 36, this.frameY[position]);
            MemoryUtil.memPutInt(address + 40, this.frameX[next]);
            MemoryUtil.memPutInt(address + 44, this.frameY[next]);
        }
    }
}

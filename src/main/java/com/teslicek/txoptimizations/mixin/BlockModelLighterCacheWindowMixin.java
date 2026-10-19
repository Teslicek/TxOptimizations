package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionLightWindow;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import java.util.Arrays;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BlockModelLighter.Cache.class, priority = 999)
public abstract class BlockModelLighterCacheWindowMixin implements SectionLightWindow {

    @Unique
    private static final int SPAN = 18;

    @Unique
    private static final int VOLUME = SPAN * SPAN * SPAN;

    @Unique
    private static final int UNSET_LIGHT = Integer.MAX_VALUE;

    @Unique
    private static final int LRU_SIZE = 100;

    @Shadow
    private boolean enabled;

    @Shadow
    @Final
    private Long2FloatLinkedOpenHashMap brightnessCache;

    @Shadow
    @Final
    private LightCoordsUtil.BrightnessGetter cachedBrightnessGetter;

    @Unique
    private final LightCoordsUtil.BrightnessGetter txoptimizations$windowBrightness = (level, pos) -> this.txoptimizations$packedBrightness(level, pos);

    @Unique
    private int[] txoptimizations$light;

    @Unique
    private float[] txoptimizations$shade;

    @Unique
    private boolean txoptimizations$windowed;

    @Unique
    private boolean txoptimizations$dirty;

    @Unique
    private int txoptimizations$minX;

    @Unique
    private int txoptimizations$minY;

    @Unique
    private int txoptimizations$minZ;

    @Override
    public void txoptimizations$beginSection(int minX, int minY, int minZ) {
        if (this.txoptimizations$light == null) {
            this.txoptimizations$light = new int[VOLUME];
            this.txoptimizations$shade = new float[VOLUME];
            this.txoptimizations$dirty = true;
        }

        if (this.txoptimizations$dirty) {
            Arrays.fill(this.txoptimizations$light, UNSET_LIGHT);
            Arrays.fill(this.txoptimizations$shade, Float.NaN);
            this.txoptimizations$dirty = false;
        }

        this.txoptimizations$windowed = true;
        this.txoptimizations$minX     = minX;
        this.txoptimizations$minY     = minY;
        this.txoptimizations$minZ     = minZ;
    }

    @Override
    public void txoptimizations$endSection() {
        this.txoptimizations$windowed = false;
    }

    @Overwrite
    public int getLightCoords(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
        return LightCoordsUtil.getLightCoords(this.enabled ? this.txoptimizations$windowBrightness : LightCoordsUtil.BrightnessGetter.DEFAULT, level, state, pos);
    }

    @Overwrite
    public float getShadeBrightness(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
        int index = this.enabled ? this.txoptimizations$index(pos) : -1;

        if (index >= 0)
            return this.txoptimizations$windowShade(state, level, pos, index);

        long key = pos.asLong();

        if (this.enabled) {
            float cached = this.brightnessCache.get(key);

            if (!Float.isNaN(cached))
                return cached;
        }

        float brightness = state.getShadeBrightness(level, pos);

        if (this.enabled) {
            if (this.brightnessCache.size() == LRU_SIZE)
                this.brightnessCache.removeFirstFloat();

            this.brightnessCache.put(key, brightness);
        }

        return brightness;
    }

    @Unique
    private int txoptimizations$packedBrightness(BlockAndLightGetter level, BlockPos pos) {
        int index = this.txoptimizations$index(pos);

        if (index < 0)
            return this.cachedBrightnessGetter.packedBrightness(level, pos);

        int packed = this.txoptimizations$light[index];

        if (packed != UNSET_LIGHT)
            return packed;

        packed = LightCoordsUtil.BrightnessGetter.DEFAULT.packedBrightness(level, pos);

        this.txoptimizations$light[index] = packed;
        this.txoptimizations$dirty        = true;

        return packed;
    }

    @Unique
    private float txoptimizations$windowShade(BlockState state, BlockAndTintGetter level, BlockPos pos, int index) {
        float cached = this.txoptimizations$shade[index];

        if (!Float.isNaN(cached))
            return cached;

        float brightness = state.getShadeBrightness(level, pos);

        this.txoptimizations$shade[index] = brightness;
        this.txoptimizations$dirty        = true;

        return brightness;
    }

    @Unique
    private int txoptimizations$index(BlockPos pos) {
        if (!this.txoptimizations$windowed)
            return -1;

        int x = pos.getX() - this.txoptimizations$minX;
        int y = pos.getY() - this.txoptimizations$minY;
        int z = pos.getZ() - this.txoptimizations$minZ;

        if (x < 0 || y < 0 || z < 0 || x >= SPAN || y >= SPAN || z >= SPAN)
            return -1;

        return (y * SPAN + z) * SPAN + x;
    }
}

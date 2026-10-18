package com.teslicek.txoptimizations;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;

public final class BiomeBlendWindow {

    private static final int                           SECTION       = 16;
    private static final AtomicLong                    BIOME_CHANGES = new AtomicLong();
    private static final ThreadLocal<BiomeBlendWindow> CURRENT       = ThreadLocal.withInitial(BiomeBlendWindow::new);

    private final BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();

    private Holder<?>[] biomes;
    private ClientLevel level;
    private long        biomeChanges;
    private int         usedLayers;
    private int         width;
    private int         minX;
    private int         minY;
    private int         minZ;

    private BiomeBlendWindow() {
    }

    public static void begin(ClientLevel level, SectionPos section) {
        BiomeBlendWindow window = CURRENT.get();
        int              radius = Minecraft.getInstance().options.biomeBlendRadius().get();

        window.clear();

        if (radius == 0)
            return;

        int width = SECTION + radius * 2;

        if (window.biomes == null || window.biomes.length < width * width * SECTION)
            window.biomes = new Holder<?>[width * width * SECTION];

        window.level        = level;
        window.biomeChanges = BIOME_CHANGES.get();
        window.width        = width;
        window.minX         = section.minBlockX() - radius;
        window.minY         = section.minBlockY();
        window.minZ         = section.minBlockZ() - radius;
    }

    public static void end() {
        CURRENT.get().clear();
    }

    public static void biomesChanged() {
        BIOME_CHANGES.incrementAndGet();
    }

    public static int blend(ClientLevel level, BlockPos pos, int radius, ColorResolver resolver) {
        BiomeBlendWindow window   = CURRENT.get();
        boolean          windowed = window.level == level && window.covers(pos, radius);
        int              y        = pos.getY();
        int              count    = (radius * 2 + 1) * (radius * 2 + 1);
        int              red      = 0;
        int              green    = 0;
        int              blue     = 0;

        if (windowed) {
            long biomeChanges = BIOME_CHANGES.get();

            if (biomeChanges != window.biomeChanges) {
                window.forgetBiomes();
                window.biomeChanges = biomeChanges;
            }

            window.usedLayers |= 1 << (y - window.minY);
        }

        for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z ++) {
            for (int x = pos.getX() - radius; x <= pos.getX() + radius; x ++) {
                Holder<?> biome = windowed ? window.biome(x, y, z) : level.getBiome(window.samplePos.set(x, y, z));
                int       color = resolver.getColor((Biome) biome.value(), x, z);

                red   += ARGB.red(color);
                green += ARGB.green(color);
                blue  += ARGB.blue(color);
            }
        }

        return ARGB.color(red / count, green / count, blue / count);
    }

    private void clear() {
        this.forgetBiomes();
        this.level = null;
    }

    private void forgetBiomes() {
        int area = this.width * this.width;

        for (int layers = this.usedLayers; layers != 0; layers &= layers - 1) {
            int layer = Integer.numberOfTrailingZeros(layers);

            Arrays.fill(this.biomes, layer * area, layer * area + area, null);
        }

        this.usedLayers = 0;
    }

    private boolean covers(BlockPos pos, int radius) {
        int x = pos.getX() - this.minX;
        int y = pos.getY() - this.minY;
        int z = pos.getZ() - this.minZ;

        return y >= 0 && y < SECTION && x >= radius && z >= radius && x + radius < this.width && z + radius < this.width;
    }

    private Holder<?> biome(int x, int y, int z) {
        int       index = ((y - this.minY) * this.width + z - this.minZ) * this.width + x - this.minX;
        Holder<?> biome = this.biomes[index];

        if (biome != null)
            return biome;

        biome              = this.level.getBiome(this.samplePos.set(x, y, z));
        this.biomes[index] = biome;

        return biome;
    }
}

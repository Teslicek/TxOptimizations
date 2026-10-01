package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public final class MapAtlas implements AutoCloseable {

    public static final int   ATLAS_SIZE = 2048;
    public static final int   MAP_SIZE   = 128;
    public static final float MAP_UV     = (float) MAP_SIZE / ATLAS_SIZE;

    private static final int MAPS_PER_ROW   = ATLAS_SIZE / MAP_SIZE;
    private static final int MAPS_PER_ATLAS = MAPS_PER_ROW * MAPS_PER_ROW;
    private static final int SLOT_BITS      = 16;
    private static final int SLOT_MASK      = (1 << SLOT_BITS) - 1;

    private final int        id;
    private final Identifier textureId;
    private final Texture    texture;
    private int              mapCount;

    public MapAtlas(int id) {
        this.id        = id;
        this.textureId = Identifier.fromNamespaceAndPath("txoptimizations", "map_atlas/" + id);
        this.texture   = new Texture("TxOptimizations map atlas " + id);

        Minecraft.getInstance().getTextureManager().register(this.textureId, this.texture);
    }

    public boolean isFull() {
        return this.mapCount >= MAPS_PER_ATLAS;
    }

    public int allocate() {
        if (this.isFull())
            throw new IllegalStateException("Map atlas " + this.id + " is full");

        return this.id << SLOT_BITS | this.mapCount ++;
    }

    public Identifier getTextureId() {
        return this.textureId;
    }

    public GpuTexture getGpuTexture() {
        return this.texture.getTexture();
    }

    @Override
    public void close() {
        Minecraft.getInstance().getTextureManager().release(this.textureId);
    }

    public static int atlasId(int location) {
        return location >>> SLOT_BITS;
    }

    public static int pixelX(int location) {
        return (location & SLOT_MASK) % MAPS_PER_ROW * MAP_SIZE;
    }

    public static int pixelY(int location) {
        return (location & SLOT_MASK) / MAPS_PER_ROW * MAP_SIZE;
    }

    private static final class Texture extends AbstractTexture {

        private Texture(String label) {
            this.texture     = RenderSystem.getDevice().createTexture(label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, GpuFormat.RGBA8_UNORM, ATLAS_SIZE, ATLAS_SIZE, 1, 1);
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
            this.sampler     = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, false);
        }
    }
}

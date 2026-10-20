package com.teslicek.txoptimizations;

import com.mojang.renderpearl.util.TextureViewAndSampler;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;

public final class TextureList extends AbstractList<PreparedRenderType.Texture> implements RandomAccess {

    private final PreparedRenderType.Texture[] textures;
    private final TextureViewAndSampler[]      bindings;
    private final int                          hash;

    public TextureList(PreparedRenderType.Texture[] textures) {
        this.textures = textures;
        this.bindings = new TextureViewAndSampler[textures.length];
        this.hash     = Arrays.hashCode(textures);

        for (int index = 0; index < textures.length; index ++) {
            PreparedRenderType.Texture texture = textures[index];

            if (texture.textureView() == null || texture.sampler() == null)
                throw new IllegalStateException("Render type texture " + texture.name() + " has no view or sampler: " + texture);

            this.bindings[index] = new TextureViewAndSampler(texture.textureView(), texture.sampler());
        }
    }

    public TextureViewAndSampler binding(int index) {
        return this.bindings[index];
    }

    @Override
    public PreparedRenderType.Texture get(int index) {
        return this.textures[index];
    }

    @Override
    public int size() {
        return this.textures.length;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;

        if (other instanceof TextureList list)
            return list.hash == this.hash && Arrays.equals(this.textures, list.textures);

        return super.equals(other);
    }
}

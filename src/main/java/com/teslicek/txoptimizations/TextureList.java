package com.teslicek.txoptimizations;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;

public final class TextureList extends AbstractList<PreparedRenderType.Texture> implements RandomAccess {

    private final PreparedRenderType.Texture[] textures;
    private final int                          hash;

    public TextureList(PreparedRenderType.Texture[] textures) {
        this.textures = textures;
        this.hash     = Arrays.hashCode(textures);
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

        if (other instanceof TextureList list && list.hash != this.hash)
            return false;

        return super.equals(other);
    }
}

package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.MapAtlasRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MapRenderState.class)
public abstract class MapRenderStateAtlasMixin implements MapAtlasRenderState {

    @Unique
    private Identifier txoptimizations$atlasTexture;

    @Unique
    private float txoptimizations$atlasU;

    @Unique
    private float txoptimizations$atlasV;

    @Override
    public Identifier txoptimizations$getAtlasTexture() {
        return this.txoptimizations$atlasTexture;
    }

    @Override
    public float txoptimizations$getAtlasU() {
        return this.txoptimizations$atlasU;
    }

    @Override
    public float txoptimizations$getAtlasV() {
        return this.txoptimizations$atlasV;
    }

    @Override
    public void txoptimizations$setAtlas(Identifier atlasTexture, float u, float v) {
        this.txoptimizations$atlasTexture = atlasTexture;
        this.txoptimizations$atlasU       = u;
        this.txoptimizations$atlasV       = v;
    }
}

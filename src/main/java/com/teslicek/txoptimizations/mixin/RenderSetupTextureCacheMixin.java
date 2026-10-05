package com.teslicek.txoptimizations.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderSetup.class)
public abstract class RenderSetupTextureCacheMixin {

    @Shadow
    @Final
    Map<String, RenderSetup.TextureBinding> textures;

    @Shadow
    @Final
    boolean useLightmap;

    @Shadow
    @Final
    boolean useOverlay;

    @Unique
    private List<PreparedRenderType.Texture> txoptimizations$prepared;

    @Overwrite
    public List<PreparedRenderType.Texture> prepareTextures(TextureManager textureManager, SamplerCache samplerCache, GpuTextureView overlayTexture, GpuTextureView lightmapTexture) {
        if (this.textures.isEmpty() && !this.useOverlay && !this.useLightmap)
            return List.of();

        List<PreparedRenderType.Texture> prepared = this.txoptimizations$prepared;

        if (prepared != null && this.txoptimizations$matches(prepared, textureManager, samplerCache, overlayTexture, lightmapTexture))
            return prepared;

        ImmutableList.Builder<PreparedRenderType.Texture> built = ImmutableList.builderWithExpectedSize(this.textures.size() + 2);

        if (this.useOverlay)
            built.add(new PreparedRenderType.Texture("Sampler1", overlayTexture, samplerCache.getClampToEdge(FilterMode.LINEAR)));

        if (this.useLightmap)
            built.add(new PreparedRenderType.Texture("Sampler2", lightmapTexture, samplerCache.getClampToEdge(FilterMode.LINEAR)));

        for (Map.Entry<String, RenderSetup.TextureBinding> entry : this.textures.entrySet()) {
            AbstractTexture texture         = textureManager.getTexture(entry.getValue().location());
            GpuSampler      samplerOverride = entry.getValue().sampler().get();

            built.add(new PreparedRenderType.Texture(entry.getKey(), texture.getTextureView(), samplerOverride != null ? samplerOverride : texture.getSampler()));
        }

        prepared                      = built.build();
        this.txoptimizations$prepared = prepared;

        return prepared;
    }

    @Unique
    private boolean txoptimizations$matches(List<PreparedRenderType.Texture> prepared, TextureManager textureManager, SamplerCache samplerCache, GpuTextureView overlayTexture, GpuTextureView lightmapTexture) {
        int expected = this.textures.size() + (this.useOverlay ? 1 : 0) + (this.useLightmap ? 1 : 0);

        if (prepared.size() != expected)
            return false;

        int index = 0;

        if (this.useOverlay && !txoptimizations$same(prepared.get(index ++), "Sampler1", overlayTexture, samplerCache.getClampToEdge(FilterMode.LINEAR)))
            return false;

        if (this.useLightmap && !txoptimizations$same(prepared.get(index ++), "Sampler2", lightmapTexture, samplerCache.getClampToEdge(FilterMode.LINEAR)))
            return false;

        for (Map.Entry<String, RenderSetup.TextureBinding> entry : this.textures.entrySet()) {
            AbstractTexture texture         = textureManager.getTexture(entry.getValue().location());
            GpuSampler      samplerOverride = entry.getValue().sampler().get();

            if (!txoptimizations$same(prepared.get(index ++), entry.getKey(), texture.getTextureView(), samplerOverride != null ? samplerOverride : texture.getSampler()))
                return false;
        }

        return true;
    }

    @Unique
    private static boolean txoptimizations$same(PreparedRenderType.Texture texture, String name, GpuTextureView textureView, GpuSampler sampler) {
        return texture.name().equals(name) && texture.textureView() == textureView && texture.sampler() == sampler;
    }
}

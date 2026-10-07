package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.teslicek.txoptimizations.TextureList;
import com.teslicek.txoptimizations.TextureManagerVersion;
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
    private TextureList txoptimizations$prepared;

    @Unique
    private String[] txoptimizations$names;

    @Unique
    private RenderSetup.TextureBinding[] txoptimizations$bindings;

    @Unique
    private AbstractTexture[] txoptimizations$textures;

    @Unique
    private TextureManager txoptimizations$texturesOwner;

    @Unique
    private int txoptimizations$texturesVersion;

    @Overwrite
    public List<PreparedRenderType.Texture> prepareTextures(TextureManager textureManager, SamplerCache samplerCache, GpuTextureView overlayTexture, GpuTextureView lightmapTexture) {
        if (this.textures.isEmpty() && !this.useOverlay && !this.useLightmap)
            return List.of();

        if (this.txoptimizations$names == null)
            this.txoptimizations$readBindings();

        TextureList prepared = this.txoptimizations$prepared;

        if (prepared != null && this.txoptimizations$matches(prepared, textureManager, samplerCache, overlayTexture, lightmapTexture))
            return prepared;

        PreparedRenderType.Texture[] built = new PreparedRenderType.Texture[this.txoptimizations$names.length + (this.useOverlay ? 1 : 0) + (this.useLightmap ? 1 : 0)];
        int                          index = 0;

        if (this.useOverlay)
            built[index ++] = new PreparedRenderType.Texture("Sampler1", overlayTexture, samplerCache.getClampToEdge(FilterMode.LINEAR));

        if (this.useLightmap)
            built[index ++] = new PreparedRenderType.Texture("Sampler2", lightmapTexture, samplerCache.getClampToEdge(FilterMode.LINEAR));

        AbstractTexture[] textures = this.txoptimizations$resolve(textureManager);

        for (int binding = 0; binding < this.txoptimizations$names.length; binding ++) {
            AbstractTexture texture         = textures[binding];
            GpuSampler      samplerOverride = this.txoptimizations$bindings[binding].sampler().get();

            built[index ++] = new PreparedRenderType.Texture(this.txoptimizations$names[binding], texture.getTextureView(), samplerOverride != null ? samplerOverride : texture.getSampler());
        }

        prepared                      = new TextureList(built);
        this.txoptimizations$prepared = prepared;

        return prepared;
    }

    @Unique
    private void txoptimizations$readBindings() {
        String[]                     names    = new String[this.textures.size()];
        RenderSetup.TextureBinding[] bindings = new RenderSetup.TextureBinding[names.length];
        int                          index    = 0;

        for (Map.Entry<String, RenderSetup.TextureBinding> entry : this.textures.entrySet()) {
            names[index]    = entry.getKey();
            bindings[index] = entry.getValue();
            index ++;
        }

        this.txoptimizations$bindings = bindings;
        this.txoptimizations$names    = names;
    }

    @Unique
    private AbstractTexture[] txoptimizations$resolve(TextureManager textureManager) {
        TextureManagerVersion version  = (TextureManagerVersion) textureManager;
        AbstractTexture[]     textures = this.txoptimizations$textures;

        if (textures != null && this.txoptimizations$texturesOwner == textureManager && this.txoptimizations$texturesVersion == version.txoptimizations$getVersion())
            return textures;

        textures = new AbstractTexture[this.txoptimizations$names.length];

        for (int binding = 0; binding < textures.length; binding ++)
            textures[binding] = textureManager.getTexture(this.txoptimizations$bindings[binding].location());

        this.txoptimizations$textures        = textures;
        this.txoptimizations$texturesOwner   = textureManager;
        this.txoptimizations$texturesVersion = version.txoptimizations$getVersion();

        return textures;
    }

    @Unique
    private boolean txoptimizations$matches(TextureList prepared, TextureManager textureManager, SamplerCache samplerCache, GpuTextureView overlayTexture, GpuTextureView lightmapTexture) {
        int index = 0;

        if (this.useOverlay && !txoptimizations$same(prepared.get(index ++), "Sampler1", overlayTexture, samplerCache.getClampToEdge(FilterMode.LINEAR)))
            return false;

        if (this.useLightmap && !txoptimizations$same(prepared.get(index ++), "Sampler2", lightmapTexture, samplerCache.getClampToEdge(FilterMode.LINEAR)))
            return false;

        AbstractTexture[] textures = this.txoptimizations$resolve(textureManager);

        for (int binding = 0; binding < this.txoptimizations$names.length; binding ++) {
            AbstractTexture texture         = textures[binding];
            GpuSampler      samplerOverride = this.txoptimizations$bindings[binding].sampler().get();

            if (!txoptimizations$same(prepared.get(index ++), this.txoptimizations$names[binding], texture.getTextureView(), samplerOverride != null ? samplerOverride : texture.getSampler()))
                return false;
        }

        return true;
    }

    @Unique
    private static boolean txoptimizations$same(PreparedRenderType.Texture texture, String name, GpuTextureView textureView, GpuSampler sampler) {
        return texture.name().equals(name) && texture.textureView() == textureView && texture.sampler() == sampler;
    }
}

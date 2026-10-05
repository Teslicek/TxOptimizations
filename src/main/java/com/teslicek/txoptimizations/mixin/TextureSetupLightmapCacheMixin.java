package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TextureSetup.class)
public abstract class TextureSetupLightmapCacheMixin {

    @Unique
    private static TextureSetup txoptimizations$last;

    @Overwrite
    public static TextureSetup singleTextureWithLightmap(GpuTextureView texture, GpuSampler sampler) {
        GpuTextureView lightmap        = Minecraft.getInstance().gameRenderer.lightmap();
        GpuSampler     lightmapSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        TextureSetup   last            = txoptimizations$last;

        if (last != null && last.texure0() == texture && last.sampler0() == sampler && last.texure2() == lightmap && last.sampler2() == lightmapSampler)
            return last;

        TextureSetup setup = new TextureSetup(texture, null, lightmap, sampler, null, lightmapSampler);

        txoptimizations$last = setup;

        return setup;
    }
}

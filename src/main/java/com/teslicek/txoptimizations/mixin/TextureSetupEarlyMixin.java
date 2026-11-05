package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TextureSetup.class, priority = 999)
public abstract class TextureSetupEarlyMixin {

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

    @Unique
    private static final int PRIME = 31;

    @Shadow
    @Final
    private GpuTextureView texure0;

    @Shadow
    @Final
    private GpuTextureView texure1;

    @Shadow
    @Final
    private GpuTextureView texure2;

    @Shadow
    @Final
    private GpuSampler sampler0;

    @Shadow
    @Final
    private GpuSampler sampler1;

    @Shadow
    @Final
    private GpuSampler sampler2;

    @Unique
    private int txoptimizations$hash;

    @Overwrite
    public final int hashCode() {
        int hash = this.txoptimizations$hash;

        if (hash != 0)
            return hash;

        hash = Objects.hashCode(this.texure0);
        hash = hash * PRIME + Objects.hashCode(this.texure1);
        hash = hash * PRIME + Objects.hashCode(this.texure2);
        hash = hash * PRIME + Objects.hashCode(this.sampler0);
        hash = hash * PRIME + Objects.hashCode(this.sampler1);
        hash = hash * PRIME + Objects.hashCode(this.sampler2);

        this.txoptimizations$hash = hash;

        return hash;
    }
}

package com.teslicek.txoptimizations.mixin;

import java.util.List;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderTypeFeatureRenderer.class)
public interface RenderTypeFeatureRendererGroupsAccessor {

    @Accessor("groups")
    List<?> txoptimizations$groups();
}

package com.teslicek.txoptimizations.mixin;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderTypeFeatureRenderer.class)
public abstract class RenderTypeFeatureRendererIndexRequestMixin {

    @Redirect(method = "prepareGroup", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0), require = 1)
    private Iterator<Object> txoptimizations$requestOnlyNewGroup(List<Object> groups) {
        return List.of(groups.getLast()).iterator();
    }
}

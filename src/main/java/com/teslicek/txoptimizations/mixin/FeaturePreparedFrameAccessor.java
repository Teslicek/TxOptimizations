package com.teslicek.txoptimizations.mixin;

import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public interface FeaturePreparedFrameAccessor {

    @Accessor("groupsByFeature")
    Map<FeatureRendererType<?>, List<?>> txoptimizations$groupsByFeature();

    @Accessor("allSubmits")
    List<SubmitNode> txoptimizations$allSubmits();
}

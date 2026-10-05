package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.List;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.oit.OitStage;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.renderer.feature.FeatureRenderDispatcher$PreparedGroup")
public interface FeaturePreparedGroupAccessor {

    @Accessor("featureType")
    FeatureRendererType<?> txoptimizations$featureType();

    @Invoker("execute")
    void txoptimizations$execute(FeatureFrameContext context, @Nullable OitStage stage, RenderPass renderPass, FeatureRendererMap featureRenderers, List<SubmitNode> submits);
}

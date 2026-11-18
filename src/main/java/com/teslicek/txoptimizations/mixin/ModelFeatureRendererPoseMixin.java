package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.ModelPartTree;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ModelFeatureRenderer.class)
public abstract class ModelFeatureRendererPoseMixin {

    @Overwrite
    private <S> void prepareModel(ModelFeatureRenderer.Submit<S> submit) {
        VertexConsumer buffer = ((ModelFeatureRenderer) (Object) this).getVertexBuilder(submit.renderType());

        if (submit.sheetedDecalPose() != null)
            buffer = new SheetedDecalTextureGenerator(buffer, submit.sheetedDecalPose(), 1.0F);
        else if (submit.uvMapping() != null)
            buffer = submit.uvMapping().wrap(buffer);

        Model<? super S> model = submit.model();

        model.setupAnim(submit.state());

        ((ModelPartTree) (Object) model.root()).txoptimizations$renderFrom(submit.pose(), buffer, submit.lightCoords(), submit.overlayCoords(), submit.tintedColor());
    }
}

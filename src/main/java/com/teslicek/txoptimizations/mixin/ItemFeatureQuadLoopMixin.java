package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureQuadLoopMixin {

    @Shadow
    @Final
    private QuadInstance quadInstance;

    @Shadow
    private static PoseStack.Pose computeFoilDecalPose(ItemDisplayContext type, PoseStack.Pose pose) {
        throw new AssertionError();
    }

    @Shadow
    private static int getLayerColorSafe(int[] tintLayers, BakedQuad.MaterialInfo material) {
        throw new AssertionError();
    }

    @Overwrite
    private void prepareMainSubmit(ItemFeatureRenderer.Submit submit) {
        this.quadInstance.setLightCoords(submit.lightCoords());
        this.quadInstance.setOverlayCoords(submit.overlayCoords());

        ItemStackRenderState.FoilType foilType      = submit.foilType();
        boolean                       special       = foilType == ItemStackRenderState.FoilType.SPECIAL;
        PoseStack.Pose                foilDecalPose = special ? computeFoilDecalPose(submit.displayContext(), submit.pose()) : null;
        List<BakedQuad>               quads         = submit.quads();
        int                           count         = quads.size();
        RenderType                    lastType      = null;
        VertexConsumer                builder       = null;
        boolean                       consolidates  = false;

        for (int index = 0; index < count; index ++) {
            BakedQuad              quad       = quads.get(index);
            BakedQuad.MaterialInfo material   = quad.materialInfo();
            RenderType             renderType = txoptimizations$renderType(material, foilType);

            this.quadInstance.setColor(getLayerColorSafe(submit.tintLayers(), material));

            if (renderType != lastType || !consolidates) {
                builder      = ((ItemFeatureRenderer) (Object) this).getVertexBuilder(renderType);
                lastType     = renderType;
                consolidates = renderType.canConsolidateConsecutiveGeometry();
            }

            if (special)
                builder.putBakedQuadWithGlint(submit.pose(), quad, this.quadInstance, foilDecalPose);
            else
                builder.putBakedQuad(submit.pose(), quad, this.quadInstance);
        }
    }

    @Overwrite
    private void prepareOutlineSubmit(ItemFeatureRenderer.Submit submit) {
        List<BakedQuad> quads = submit.quads();
        int             count = quads.size();

        for (int index = 0; index < count; index ++) {
            BakedQuad  quad       = quads.get(index);
            RenderType renderType = quad.materialInfo().itemRenderType().outline().orElse(null);

            if (renderType == null)
                continue;

            this.quadInstance.setColor(submit.outlineColor());
            ((ItemFeatureRenderer) (Object) this).getVertexBuilder(renderType).putBakedQuad(submit.pose(), quad, this.quadInstance);
        }
    }

    @Unique
    private static RenderType txoptimizations$renderType(BakedQuad.MaterialInfo material, ItemStackRenderState.FoilType foilType) {
        if (foilType == ItemStackRenderState.FoilType.NONE)
            return material.itemRenderType();

        if (foilType == ItemStackRenderState.FoilType.STANDARD)
            return material.itemGlintRenderType();

        if (foilType == ItemStackRenderState.FoilType.SPECIAL)
            return material.itemGlintSpecialRenderType();

        throw new IllegalStateException("Unknown item foil type " + foilType);
    }
}

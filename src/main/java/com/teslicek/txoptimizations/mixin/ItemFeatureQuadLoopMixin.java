package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teslicek.txoptimizations.BakedQuadVertices;
import com.teslicek.txoptimizations.DirectVertexBuffer;
import com.teslicek.txoptimizations.NormalCache;
import java.util.List;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ItemFeatureRenderer.class, priority = 999)
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
        PoseStack.Pose                pose          = submit.pose();
        PoseStack.Pose                foilDecalPose = special ? computeFoilDecalPose(submit.displayContext(), pose) : null;
        boolean                       normalChosen  = !special && NormalCache.usable();
        List<BakedQuad>               quads         = submit.quads();
        int                           count         = quads.size();
        RenderType                    lastType      = null;
        VertexConsumer                builder       = null;
        boolean                       consolidates  = false;
        int                           index         = 0;

        if (normalChosen)
            NormalCache.select(pose);

        while (index < count) {
            BakedQuad              quad       = quads.get(index);
            BakedQuad.MaterialInfo material   = quad.materialInfo();
            RenderType             renderType = txoptimizations$renderType(material, foilType);

            if (renderType != lastType || !consolidates) {
                builder      = ((ItemFeatureRenderer) (Object) this).getVertexBuilder(renderType);
                lastType     = renderType;
                consolidates = renderType.canConsolidateConsecutiveGeometry();
            }

            int written = normalChosen && consolidates ? this.txoptimizations$putEntityRun(builder, submit, index, renderType) : 0;

            if (written > 0) {
                index += written;
                continue;
            }

            this.quadInstance.setColor(getLayerColorSafe(submit.tintLayers(), material));

            if (special)
                builder.putBakedQuadWithGlint(pose, quad, this.quadInstance, foilDecalPose);
            else
                builder.putBakedQuad(pose, quad, this.quadInstance);

            index ++;
        }
    }

    @Unique
    private int txoptimizations$putEntityRun(VertexConsumer builder, ItemFeatureRenderer.Submit submit, int from, RenderType renderType) {
        if (!(builder instanceof DirectVertexBuffer direct) || direct.txoptimizations$format() != DefaultVertexFormat.ENTITY)
            return 0;

        List<BakedQuad>               quads    = submit.quads();
        ItemStackRenderState.FoilType foilType = submit.foilType();
        int                           end      = from + 1;

        while (end < quads.size() && txoptimizations$renderType(quads.get(end).materialInfo(), foilType) == renderType)
            end ++;

        long pointer = direct.txoptimizations$tryReserveVertices(DefaultVertexFormat.ENTITY, (end - from) * BakedQuadVertices.VERTICES);

        if (pointer == MemoryUtil.NULL)
            return 0;

        PoseStack.Pose pose   = submit.pose();
        int            stride = DefaultVertexFormat.ENTITY.getVertexSize();

        for (int index = from; index < end; index ++) {
            BakedQuad quad = quads.get(index);

            this.quadInstance.setColor(getLayerColorSafe(submit.tintLayers(), quad.materialInfo()));
            BakedQuadVertices.putEntity(pointer, stride, pose, quad, this.quadInstance, NormalCache.packed(pose, quad.direction()));
            pointer += (long) BakedQuadVertices.VERTICES * stride;
        }

        return end - from;
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

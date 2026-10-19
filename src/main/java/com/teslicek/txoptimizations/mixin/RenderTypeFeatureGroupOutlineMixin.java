package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.teslicek.txoptimizations.OutlineDraw;
import com.teslicek.txoptimizations.OutlineDrawGroup;
import java.util.List;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group")
public abstract class RenderTypeFeatureGroupOutlineMixin implements OutlineDrawGroup {

    @Shadow
    @Final
    private List<StagedVertexBuffer.Draw> draws;

    @Shadow
    @Final
    private List<PreparedRenderType> drawRenderTypes;

    @Override
    public List<StagedVertexBuffer.Draw> txoptimizations$draws() {
        return this.draws;
    }

    @Override
    public List<PreparedRenderType> txoptimizations$drawRenderTypes() {
        return this.drawRenderTypes;
    }

    @ModifyExpressionValue(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/StagedVertexBuffer;appendDraw(Lcom/mojang/renderpearl/api/vertex/VertexFormat;Lcom/mojang/renderpearl/api/pipeline/PrimitiveTopology;Lcom/mojang/blaze3d/vertex/VertexSorting;)Lnet/minecraft/client/renderer/StagedVertexBuffer$Draw;"))
    private StagedVertexBuffer.Draw txoptimizations$trackOutlineDraw(StagedVertexBuffer.Draw draw, @Local(argsOnly = true) RenderType renderType) {
        if (renderType.isOutline() && renderType.format() == DefaultVertexFormat.POSITION_TEX_COLOR)
            ((OutlineDraw) draw).txoptimizations$trackOutline();

        return draw;
    }
}

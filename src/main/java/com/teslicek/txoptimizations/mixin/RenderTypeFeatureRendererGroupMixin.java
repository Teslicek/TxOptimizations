package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.teslicek.txoptimizations.GpuCubeDraw;
import com.teslicek.txoptimizations.OutlineDraw;
import com.teslicek.txoptimizations.OutlineDrawGroup;
import com.teslicek.txoptimizations.ParallelFeatures;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.List;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group")
public abstract class RenderTypeFeatureRendererGroupMixin implements OutlineDrawGroup {

    @Redirect(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderType;prepare()Lnet/minecraft/client/renderer/rendertype/PreparedRenderType;"))
    private PreparedRenderType txoptimizations$prepareLocked(RenderType renderType) {
        return ParallelFeatures.prepare(renderType);
    }

    @Redirect(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getProjectionType()Lcom/mojang/blaze3d/ProjectionType;"))
    private ProjectionType txoptimizations$projectionType() {
        return ParallelFeatures.projectionType();
    }

    @Inject(method = "getOrAddDraw", at = @At("RETURN"))
    private void txoptimizations$tagRenderType(CallbackInfoReturnable<StagedVertexBuffer.Draw> cir, @Local PreparedRenderType prepared) {
        ((GpuCubeDraw) cir.getReturnValue()).txoptimizations$setPrepared(prepared);
    }

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

    @Unique
    private static final int NOT_FOUND = -1;

    @Unique
    private static final int LINEAR_SEARCH_LIMIT = 8;

    @Unique
    private Reference2IntOpenHashMap<PreparedRenderType> txoptimizations$index;

    @Redirect(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Ljava/util/List;indexOf(Ljava/lang/Object;)I"))
    private int txoptimizations$findIndexed(List<PreparedRenderType> list, Object type) {
        if (this.txoptimizations$index != null)
            return this.txoptimizations$index.getInt(type);

        for (int position = 0; position < list.size(); position ++) {
            if (list.get(position) == type)
                return position;
        }

        return NOT_FOUND;
    }

    @Redirect(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1))
    private boolean txoptimizations$indexAdded(List<PreparedRenderType> list, Object type) {
        boolean added = list.add((PreparedRenderType) type);

        if (this.txoptimizations$index != null) {
            this.txoptimizations$index.putIfAbsent((PreparedRenderType) type, list.size() - 1);

            return added;
        }

        if (list.size() > LINEAR_SEARCH_LIMIT) {
            Reference2IntOpenHashMap<PreparedRenderType> index = new Reference2IntOpenHashMap<>(list.size() * 2);

            index.defaultReturnValue(NOT_FOUND);

            for (int position = 0; position < list.size(); position ++)
                index.putIfAbsent(list.get(position), position);

            this.txoptimizations$index = index;
        }

        return added;
    }
}

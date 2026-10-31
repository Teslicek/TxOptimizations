package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.teslicek.txoptimizations.LastWrittenData;
import com.teslicek.txoptimizations.OutlineModelView;
import com.teslicek.txoptimizations.ParallelFeatures;
import com.teslicek.txoptimizations.RenderThreadStack;
import java.util.Objects;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderType.class)
public abstract class RenderTypeOutlineModelViewMixin {

    @Unique
    private static final Matrix4f MODEL_VIEW = new Matrix4f();

    @Unique
    private static Matrix4fc txoptimizations$outlineModelView;

    @Shadow
    public abstract boolean isOutline();

    @Redirect(method = "prepare", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getModelViewMatrixCopy()Lorg/joml/Matrix4f;"))
    private Matrix4f txoptimizations$scratchModelView() {
        if (!RenderThreadStack.isOwner())
            return ParallelFeatures.modelViewMatrixCopy();

        return MODEL_VIEW.set(RenderSystem.getModelViewStack());
    }

    @Redirect(method = "writeDynamicTransforms", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
    private GpuBufferSlice txoptimizations$writeOrReuseTransform(DynamicGpuData data, Matrix4f modelView, Matrix4f textureMatrix) {
        if (modelView == MODEL_VIEW) {
            DynamicGpuData.Transform last = txoptimizations$lastTransform(data);

            if (last != null && txoptimizations$repeats(last, textureMatrix)) {
                this.txoptimizations$keepOutlineModelView(last.modelView());

                return data.writeTransform(last);
            }

            modelView = new Matrix4f(MODEL_VIEW);
        }

        this.txoptimizations$keepOutlineModelView(modelView);

        return data.writeTransform(modelView, textureMatrix);
    }

    @ModifyReturnValue(method = "prepare", at = @At("RETURN"))
    private PreparedRenderType txoptimizations$attachOutlineModelView(PreparedRenderType prepared) {
        if (!this.isOutline())
            return prepared;

        if (txoptimizations$outlineModelView == null)
            throw new IllegalStateException("Outline render type was prepared without writing its model-view matrix");

        ((OutlineModelView) (Object) prepared).txoptimizations$setModelView(txoptimizations$outlineModelView);
        txoptimizations$outlineModelView = null;

        return prepared;
    }

    @Unique
    private void txoptimizations$keepOutlineModelView(Matrix4fc modelView) {
        if (this.isOutline())
            txoptimizations$outlineModelView = modelView;
    }

    @Unique
    private static DynamicGpuData.Transform txoptimizations$lastTransform(DynamicGpuData data) {
        return (DynamicGpuData.Transform) ((LastWrittenData) ((DynamicGpuDataTransformsAccessor) data).txoptimizations$transforms()).txoptimizations$lastData();
    }

    @Unique
    private static boolean txoptimizations$repeats(DynamicGpuData.Transform last, Matrix4f textureMatrix) {
        return Objects.equals(last.modelOffset(), DynamicGpuDataTransformsAccessor.txoptimizations$noOffset()) && Objects.equals(last.colorModulator(), DynamicGpuDataTransformsAccessor.txoptimizations$white()) && Objects.equals(last.modelView(), MODEL_VIEW) && Objects.equals(last.textureMatrix(), textureMatrix);
    }
}

package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.teslicek.txoptimizations.GpuCubeBatch;
import com.teslicek.txoptimizations.GpuCubeBuilder;
import com.teslicek.txoptimizations.GpuCubeDraw;
import com.teslicek.txoptimizations.GpuCubes;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StagedVertexBuffer.class)
public abstract class StagedVertexBufferGpuCubesMixin {

    @Shadow
    private BufferBuilder lastVertexBuilder;

    @Unique
    private final GpuCubeBatch txoptimizations$cubes = new GpuCubeBatch("staged vertices");

    @Inject(method = "appendDraw(Lcom/mojang/renderpearl/api/vertex/VertexFormat;Lcom/mojang/renderpearl/api/pipeline/PrimitiveTopology;Lcom/mojang/blaze3d/vertex/VertexSorting;)Lnet/minecraft/client/renderer/StagedVertexBuffer$Draw;", at = @At("RETURN"))
    private void txoptimizations$giveCubeBatch(CallbackInfoReturnable<StagedVertexBuffer.Draw> cir) {
        ((GpuCubeDraw) cir.getReturnValue()).txoptimizations$setBatch(this.txoptimizations$cubes);
    }

    @Inject(method = "getVertexBuilder", at = @At("RETURN"))
    private void txoptimizations$tagBuilder(StagedVertexBuffer.Draw draw, CallbackInfoReturnable<?> cir) {
        ((GpuCubeBuilder) this.lastVertexBuilder).txoptimizations$setCubeDraw((GpuCubeDraw) draw);
    }

    @Inject(method = "finishLastVertexBuilder", at = @At("HEAD"))
    private void txoptimizations$untagBuilder(CallbackInfo ci) {
        if (this.lastVertexBuilder != null)
            ((GpuCubeBuilder) this.lastVertexBuilder).txoptimizations$setCubeDraw(null);
    }

    @Inject(method = "upload", at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/StagedVertexBuffer;uploadDrawsToBuffers(Lcom/mojang/renderpearl/api/device/GpuDevice;Ljava/util/List;Lcom/mojang/renderpearl/api/buffers/GpuBuffer;Lcom/mojang/renderpearl/api/buffers/GpuBuffer;II)V"), @At("RETURN")})
    private void txoptimizations$uploadCubes(CallbackInfo ci) {
        this.txoptimizations$cubes.upload(RenderSystem.getDevice());
    }

    @Inject(method = "endDraw", at = @At("TAIL"))
    private void txoptimizations$resetCubes(CallbackInfo ci) {
        this.txoptimizations$cubes.reset();
    }

    @Inject(method = "endFrame", at = @At("TAIL"))
    private void txoptimizations$retireCubeBuffers(CallbackInfo ci) {
        GpuCubes.endFrame();
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void txoptimizations$closeCubes(CallbackInfo ci) {
        this.txoptimizations$cubes.close();
    }
}

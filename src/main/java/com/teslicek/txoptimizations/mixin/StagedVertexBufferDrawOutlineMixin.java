package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.MeshData;
import com.teslicek.txoptimizations.OutlineDraw;
import com.teslicek.txoptimizations.OutlineScissor;
import java.util.Arrays;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StagedVertexBuffer.Draw.class)
public abstract class StagedVertexBufferDrawOutlineMixin implements OutlineDraw {

    @Unique
    private static final int INITIAL_BOXES = 2;

    @Unique
    private float[] txoptimizations$outlineBoxes;

    @Unique
    private int txoptimizations$outlineBoxValues;

    @Override
    public void txoptimizations$trackOutline() {
        this.txoptimizations$outlineBoxes = new float[OutlineScissor.BOX_VALUES * INITIAL_BOXES];
    }

    @Override
    public float[] txoptimizations$outlineBoxes() {
        return this.txoptimizations$outlineBoxes;
    }

    @Override
    public int txoptimizations$outlineBoxValues() {
        return this.txoptimizations$outlineBoxValues;
    }

    @Inject(method = "append", at = @At("RETURN"))
    private void txoptimizations$measureOutline(MeshData mesh, CallbackInfo ci) {
        if (this.txoptimizations$outlineBoxes == null)
            return;

        if (this.txoptimizations$outlineBoxValues == this.txoptimizations$outlineBoxes.length)
            this.txoptimizations$outlineBoxes = Arrays.copyOf(this.txoptimizations$outlineBoxes, this.txoptimizations$outlineBoxes.length * 2);

        OutlineScissor.measureVertices(mesh, this.txoptimizations$outlineBoxes, this.txoptimizations$outlineBoxValues);

        this.txoptimizations$outlineBoxValues += OutlineScissor.BOX_VALUES;
    }
}

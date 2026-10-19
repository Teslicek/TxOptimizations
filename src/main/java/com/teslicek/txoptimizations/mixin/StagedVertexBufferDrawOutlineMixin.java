package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.OutlineDraw;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(StagedVertexBuffer.Draw.class)
public abstract class StagedVertexBufferDrawOutlineMixin implements OutlineDraw {

    @Unique
    private boolean txoptimizations$outlineTracked;

    @Unique
    private float[] txoptimizations$outlineBoxes;

    @Override
    public void txoptimizations$trackOutline() {
        this.txoptimizations$outlineTracked = true;
    }

    @Override
    public boolean txoptimizations$isOutlineTracked() {
        return this.txoptimizations$outlineTracked;
    }

    @Override
    public float[] txoptimizations$outlineBoxes() {
        return this.txoptimizations$outlineBoxes;
    }

    @Override
    public void txoptimizations$setOutlineBoxes(float[] boxes) {
        this.txoptimizations$outlineBoxes = boxes;
    }
}

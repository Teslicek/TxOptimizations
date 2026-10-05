package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.PackedGizmoLines;
import net.minecraft.gizmos.ArrowGizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ArrowGizmo.class)
public abstract class ArrowGizmoPackedMixin {

    @Unique
    private static final Quaternionf ROTATION = new Quaternionf();

    @Unique
    private static final Vector3f DIRECTION = new Vector3f();

    @Unique
    private static final Vector3f TIP = new Vector3f();

    @Shadow
    @Final
    private Vec3 start;

    @Shadow
    @Final
    private Vec3 end;

    @Shadow
    @Final
    private int color;

    @Shadow
    @Final
    private float width;

    @Overwrite
    public void emit(GizmoPrimitives primitives, float alphaMultiplier) {
        int color = ARGB.multiplyAlpha(this.color, alphaMultiplier);

        if (!(primitives instanceof PackedGizmoLines packed))
            throw new IllegalStateException("Gizmo primitives are not packed: " + primitives.getClass().getName());

        packed.txoptimizations$addLine(this.start.x, this.start.y, this.start.z, this.end.x, this.end.y, this.end.z, color, this.width);
        DIRECTION.set((float) (this.end.x + -this.start.x), (float) (this.end.y + -this.start.y), (float) (this.end.z + -this.start.z)).normalize();
        ROTATION.rotationTo(1.0F, 0.0F, 0.0F, DIRECTION.x, DIRECTION.y, DIRECTION.z);

        float len = (float) Mth.clamp(this.end.distanceTo(this.start) * 0.1F, 0.1F, 1.0);

        txoptimizations$tip(packed, -len, len, 0.0F, color);
        txoptimizations$tip(packed, -len, 0.0F, len, color);
        txoptimizations$tip(packed, -len, -len, 0.0F, color);
        txoptimizations$tip(packed, -len, 0.0F, -len, color);
    }

    @Unique
    private void txoptimizations$tip(PackedGizmoLines packed, float x, float y, float z, int color) {
        ROTATION.transform(x, y, z, TIP);
        packed.txoptimizations$addLine(this.end.x + TIP.x, this.end.y + TIP.y, this.end.z + TIP.z, this.end.x, this.end.y, this.end.z, color, this.width);
    }
}

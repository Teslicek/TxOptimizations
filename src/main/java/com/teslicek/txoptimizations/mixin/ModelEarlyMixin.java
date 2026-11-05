package com.teslicek.txoptimizations.mixin;

import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Model.class, priority = 999)
public abstract class ModelEarlyMixin {

    @Unique
    private Last txoptimizations$last;

    @Shadow
    public abstract Function<Identifier, RenderType> renderType();

    @Overwrite
    public final RenderType renderType(Identifier texture) {
        Last last = this.txoptimizations$last;

        if (last != null && last.texture() == texture)
            return last.type();

        RenderType type = this.renderType().apply(texture);

        this.txoptimizations$last = new Last(texture, type);

        return type;
    }

    @Unique
    private record Last(Identifier texture, RenderType type) {
    }

    @Shadow
    @Final
    private List<ModelPart> allParts;

    @Unique
    private static final int POSE_VALUES = 9;

    @Unique
    private ModelPart[] txoptimizations$parts;

    @Unique
    private PartPose[] txoptimizations$posesFor;

    @Unique
    private float[] txoptimizations$poses;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$snapshotParts(ModelPart root, Function<Identifier, RenderType> renderType, CallbackInfo ci) {
        this.txoptimizations$parts    = this.allParts.toArray(ModelPart[]::new);
        this.txoptimizations$posesFor = new PartPose[this.txoptimizations$parts.length];
        this.txoptimizations$poses    = new float[this.txoptimizations$parts.length * POSE_VALUES];
    }

    @Overwrite
    public final void resetPose() {
        ModelPart[] parts    = this.txoptimizations$parts;
        PartPose[]  posesFor = this.txoptimizations$posesFor;
        float[]     poses    = this.txoptimizations$poses;

        for (int index = 0; index < parts.length; index ++) {
            ModelPart part = parts[index];

            if (part.getClass() != ModelPart.class) {
                part.resetPose();
                continue;
            }

            PartPose initial = part.getInitialPose();
            int      at      = index * POSE_VALUES;

            if (initial != posesFor[index]) {
                poses[at]       = initial.x();
                poses[at + 1]   = initial.y();
                poses[at + 2]   = initial.z();
                poses[at + 3]   = initial.xRot();
                poses[at + 4]   = initial.yRot();
                poses[at + 5]   = initial.zRot();
                poses[at + 6]   = initial.xScale();
                poses[at + 7]   = initial.yScale();
                poses[at + 8]   = initial.zScale();
                posesFor[index] = initial;
            }

            part.x      = poses[at];
            part.y      = poses[at + 1];
            part.z      = poses[at + 2];
            part.xRot   = poses[at + 3];
            part.yRot   = poses[at + 4];
            part.zRot   = poses[at + 5];
            part.xScale = poses[at + 6];
            part.yScale = poses[at + 7];
            part.zScale = poses[at + 8];
        }
    }
}

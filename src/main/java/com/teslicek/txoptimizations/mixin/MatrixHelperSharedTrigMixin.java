package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teslicek.txoptimizations.ZyxRotation;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = MatrixHelper.class, remap = false)
public abstract class MatrixHelperSharedTrigMixin {

    @Overwrite
    public static void rotateZYX(PoseStack.Pose matrices, float angleZ, float angleY, float angleX) {
        ZyxRotation.rotate(matrices.pose(), matrices.normal(), angleZ, angleY, angleX);
    }
}

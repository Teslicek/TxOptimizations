package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ModelCopies;
import net.minecraft.client.model.object.bell.BellModel;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BellRenderer.class)
public abstract class BellRendererTwinMixin {

    @Redirect(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BellRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/blockentity/BellRenderer;model:Lnet/minecraft/client/model/object/bell/BellModel;", opcode = Opcodes.GETFIELD, ordinal = 0), require = 1)
    private BellModel txoptimizations$poseTwin(BellRenderer renderer) {
        return ModelCopies.mainModel(((BellRendererModelAccessor) renderer).txoptimizations$model());
    }
}

package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderStateBuffers;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public abstract class GameRendererRoleStateMixin {

    @Redirect(method = {"render", "renderLevel", "render3dHud", "renderItemInHand"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;gameRenderState:Lnet/minecraft/client/renderer/state/GameRenderState;", opcode = Opcodes.GETFIELD), require = 4)
    private GameRenderState txoptimizations$renderTree(GameRenderer renderer) {
        return RenderStateBuffers.renderTree();
    }

    @Redirect(method = {"gameRenderState", "useImprovedTransparency", "bobHurt"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;gameRenderState:Lnet/minecraft/client/renderer/state/GameRenderState;", opcode = Opcodes.GETFIELD), require = 3)
    private GameRenderState txoptimizations$treeForRole(GameRenderer renderer) {
        return RenderStateBuffers.forRole(((GameRendererStateAccessor) renderer).txoptimizations$gameRenderState());
    }
}

package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ElementSort;
import java.util.Comparator;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererElementOrderMixin {

    @Shadow
    @Final
    private static Comparator<GuiElementRenderState> ELEMENT_SORT_COMPARATOR;

    @ModifyArg(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;sortElements(Ljava/util/Comparator;)V"))
    private Comparator<GuiElementRenderState> txoptimizations$sortByKeys(Comparator<GuiElementRenderState> comparator) {
        if (comparator != ELEMENT_SORT_COMPARATOR)
            return comparator;

        return ElementSort.ORDER;
    }
}

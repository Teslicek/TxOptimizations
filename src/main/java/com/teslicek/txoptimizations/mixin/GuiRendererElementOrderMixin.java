package com.teslicek.txoptimizations.mixin;

import java.util.Comparator;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererElementOrderMixin {

    @Unique
    private static final Comparator<GuiElementRenderState> UNBOXED_ELEMENT_ORDER = GuiRendererElementOrderMixin::txoptimizations$compareElements;

    @Shadow
    @Final
    private static Comparator<GuiElementRenderState> ELEMENT_SORT_COMPARATOR;

    @ModifyArg(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;sortElements(Ljava/util/Comparator;)V"))
    private Comparator<GuiElementRenderState> txoptimizations$sortUnboxed(Comparator<GuiElementRenderState> comparator) {
        if (comparator != ELEMENT_SORT_COMPARATOR)
            return comparator;

        return UNBOXED_ELEMENT_ORDER;
    }

    @Unique
    private static int txoptimizations$compareElements(GuiElementRenderState first, GuiElementRenderState second) {
        int order = txoptimizations$compareScissors(first.scissorArea(), second.scissorArea());

        if (order != 0)
            return order;

        order = Integer.compare(first.pipeline().getSortKey(), second.pipeline().getSortKey());

        if (order != 0)
            return order;

        return txoptimizations$compareTextures(first.textureSetup(), second.textureSetup());
    }

    @Unique
    private static int txoptimizations$compareScissors(ScreenRectangle first, ScreenRectangle second) {
        if (first == null)
            return second == null ? 0 : -1;

        if (second == null)
            return 1;

        int order = Integer.compare(first.top(), second.top());

        if (order != 0)
            return order;

        order = Integer.compare(first.bottom(), second.bottom());

        if (order != 0)
            return order;

        order = Integer.compare(first.left(), second.left());

        if (order != 0)
            return order;

        return Integer.compare(first.right(), second.right());
    }

    @Unique
    private static int txoptimizations$compareTextures(TextureSetup first, TextureSetup second) {
        if (first == null)
            return second == null ? 0 : -1;

        if (second == null)
            return 1;

        return Integer.compare(first.getSortKey(), second.getSortKey());
    }
}

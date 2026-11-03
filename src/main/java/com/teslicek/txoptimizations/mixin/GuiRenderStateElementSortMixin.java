package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ElementSort;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateElementSortMixin {

    @Redirect(method = "lambda$sortElements$0", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"), require = 1)
    private static void txoptimizations$sortByKeys(List<GuiElementRenderState> elements, Comparator<GuiElementRenderState> comparator) {
        if (comparator != ElementSort.ORDER) {
            elements.sort(comparator);
            return;
        }

        ElementSort.sort(elements);
    }
}

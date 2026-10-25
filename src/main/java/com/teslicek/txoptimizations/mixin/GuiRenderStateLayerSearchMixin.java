package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GuiLayerBounds;
import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiRenderState.class, priority = 999)
public abstract class GuiRenderStateLayerSearchMixin {

    @Shadow
    @Final
    private List<GuiRenderState.Node> strata;

    @Shadow
    private GuiRenderState.Node current;

    @Shadow
    public void up() {
        throw new AssertionError();
    }

    @Overwrite
    private void navigateToAboveHighestElementWithIntersectingBounds(ScreenRectangle bounds) {
        GuiRenderState.Node node = this.strata.getLast();

        while (node.up != null)
            node = node.up;

        int     left   = bounds.left();
        int     top    = bounds.top();
        int     right  = bounds.right();
        int     bottom = bounds.bottom();
        boolean found  = false;

        while (!found) {
            GuiLayerBounds layer = (GuiLayerBounds) node;

            layer.txoptimizations$recordNewBounds();
            found = layer.txoptimizations$intersects(left, top, right, bottom);

            if (node.parent == null)
                break;

            if (!found)
                node = node.parent;
        }

        this.current = node;

        if (found)
            this.up();
    }
}

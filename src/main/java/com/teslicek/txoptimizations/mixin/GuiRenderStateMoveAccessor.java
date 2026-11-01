package com.teslicek.txoptimizations.mixin;

import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiRenderState.class)
public interface GuiRenderStateMoveAccessor {

    @Accessor("strata")
    List<GuiRenderState.Node> txoptimizations$strata();

    @Accessor("itemModelIdentities")
    Set<Object> txoptimizations$itemModelIdentities();

    @Accessor("current")
    GuiRenderState.Node txoptimizations$current();

    @Accessor("current")
    void txoptimizations$setCurrent(GuiRenderState.Node value);

    @Accessor("firstStratumAfterBlur")
    int txoptimizations$firstStratumAfterBlur();

    @Accessor("firstStratumAfterBlur")
    void txoptimizations$setFirstStratumAfterBlur(int value);

    @Accessor("windowRectangleForDebug")
    ScreenRectangle txoptimizations$windowRectangleForDebug();

    @Accessor("windowRectangleForDebug")
    void txoptimizations$setWindowRectangleForDebug(ScreenRectangle value);
}

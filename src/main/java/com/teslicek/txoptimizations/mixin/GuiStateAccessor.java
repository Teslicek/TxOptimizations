package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiStateAccessor {

    @Accessor("guiRenderState")
    @Mutable
    void txoptimizations$setGuiRenderState(GuiRenderState value);
}

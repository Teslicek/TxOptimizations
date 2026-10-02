package com.teslicek.txoptimizations.mixin.cull;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderSectionManager.class)
public interface RenderSectionManagerInvoker {

    @Invoker("getRenderSection")
    RenderSection txoptimizations$getRenderSection(int x, int y, int z);
}

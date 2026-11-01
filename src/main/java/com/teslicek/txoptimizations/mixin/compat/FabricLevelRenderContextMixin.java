package com.teslicek.txoptimizations.mixin.compat;

import com.teslicek.txoptimizations.FabricLevelRenderContext;
import net.fabricmc.fabric.impl.client.rendering.level.LevelRenderContextImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LevelRenderer.class, priority = 1100)
public abstract class FabricLevelRenderContextMixin implements FabricLevelRenderContext {

    @Shadow(remap = false)
    @Final
    private LevelRenderContextImpl renderContext;

    @Override
    public void txoptimizations$prepareBeforeFirstRender(LevelRenderState levelRenderState, SubmitNodeStorage storage) {
        if (this.renderContext.levelState() != null)
            return;

        this.renderContext.prepare(Minecraft.getInstance().gameRenderer, (LevelRenderer) (Object) this, levelRenderState, storage);
    }
}

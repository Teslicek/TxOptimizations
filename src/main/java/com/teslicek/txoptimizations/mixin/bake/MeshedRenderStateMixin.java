package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.MeshedRenderState;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.entity.state.CushionRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({BannerRenderState.class, CushionRenderState.class})
public abstract class MeshedRenderStateMixin implements MeshedRenderState {

    @Unique
    private boolean txoptimizations$meshed;

    @Override
    public boolean txoptimizations$isMeshed() {
        return this.txoptimizations$meshed;
    }

    @Override
    public void txoptimizations$setMeshed(boolean meshed) {
        this.txoptimizations$meshed = meshed;
    }
}

package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.DeferrableRenderer;
import com.teslicek.txoptimizations.EntityCullOrder;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererDeferrableMixin implements DeferrableRenderer {

    @Unique
    private static final byte UNKNOWN = 0;

    @Unique
    private static final byte DEFERRABLE = 1;

    @Unique
    private static final byte OVERRIDDEN = 2;

    @Unique
    private byte txoptimizations$deferState;

    @Override
    public boolean txoptimizations$canDefer() {
        if (this.txoptimizations$deferState == UNKNOWN)
            this.txoptimizations$deferState = EntityCullOrder.inheritsShouldRender(this.getClass()) ? DEFERRABLE : OVERRIDDEN;

        return this.txoptimizations$deferState == DEFERRABLE;
    }
}

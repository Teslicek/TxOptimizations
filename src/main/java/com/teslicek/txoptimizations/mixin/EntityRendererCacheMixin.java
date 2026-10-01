package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RendererCache;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityRendererCacheMixin implements RendererCache {

    @Unique
    private Map<?, ?> txoptimizations$rendererSource;

    @Unique
    private Object txoptimizations$renderer;

    @Override
    public Map<?, ?> txoptimizations$getRendererSource() {
        return this.txoptimizations$rendererSource;
    }

    @Override
    public Object txoptimizations$getRenderer() {
        return this.txoptimizations$renderer;
    }

    @Override
    public void txoptimizations$setRenderer(Map<?, ?> rendererSource, Object renderer) {
        this.txoptimizations$rendererSource = rendererSource;
        this.txoptimizations$renderer       = renderer;
    }
}

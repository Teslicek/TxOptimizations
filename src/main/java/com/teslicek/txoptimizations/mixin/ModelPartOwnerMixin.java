package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ModelPartOwner;
import net.minecraft.client.model.geom.ModelPart;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ModelPart.class)
public abstract class ModelPartOwnerMixin implements ModelPartOwner {

    @Unique
    private @Nullable Object txoptimizations$owner;

    @Override
    public @Nullable Object txoptimizations$owner() {
        return this.txoptimizations$owner;
    }

    @Override
    public void txoptimizations$claim(Object model) {
        Object owner = this.txoptimizations$owner;

        this.txoptimizations$owner = owner == null || owner == model ? model : ModelPartOwner.SHARED;
    }
}

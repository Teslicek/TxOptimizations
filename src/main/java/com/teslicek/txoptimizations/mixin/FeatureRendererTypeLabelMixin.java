package com.teslicek.txoptimizations.mixin;

import java.util.function.Supplier;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FeatureRendererType.class)
public abstract class FeatureRendererTypeLabelMixin implements Supplier<String> {

    @Shadow
    @Final
    private String name;

    @Override
    public String get() {
        return this.name;
    }
}

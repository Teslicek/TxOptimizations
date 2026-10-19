package com.teslicek.txoptimizations.mixin;

import java.util.List;
import java.util.Objects;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeLayer;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.attribute.EnvironmentAttributeSystem$ValueSampler", priority = 999)
public abstract class EnvironmentAttributeSamplerLayersMixin {

    @Shadow
    @Final
    private EnvironmentAttribute<Object> attribute;

    @Shadow
    @Final
    private Object baseValue;

    @Shadow
    @Final
    private List<EnvironmentAttributeLayer<Object>> layers;

    @Shadow
    private int cacheTickId;

    @Overwrite
    private Object computeValuePositional(Vec3 pos, @Nullable SpatialAttributeInterpolator biomeInterpolator) {
        Object result = this.baseValue;

        for (int index = 0; index < this.layers.size(); index ++) {
            EnvironmentAttributeLayer<Object> layer = this.layers.get(index);

            if (layer instanceof EnvironmentAttributeLayer.Constant<Object> constant)
                result = constant.applyConstant(result);
            else if (layer instanceof EnvironmentAttributeLayer.TimeBased<Object> timeBased)
                result = timeBased.applyTimeBased(result, this.cacheTickId);
            else if (layer instanceof EnvironmentAttributeLayer.Positional<Object> positional)
                result = positional.applyPositional(result, Objects.requireNonNull(pos), biomeInterpolator);
            else
                throw new IllegalStateException("Unknown environment attribute layer " + layer.getClass().getName());
        }

        return this.attribute.sanitizeValue(result);
    }
}

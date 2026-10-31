package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AttributeVersion;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityScaleMixin {

    @Unique
    private AttributeMap txoptimizations$scaleMap;

    @Unique
    private int txoptimizations$scaleVersion;

    @Unique
    private float txoptimizations$scale;

    @Shadow
    public abstract AttributeMap getAttributes();

    @Shadow
    protected abstract float sanitizeScale(float scale);

    @Overwrite
    public final float getScale() {
        AttributeMap attributes = this.getAttributes();

        if (attributes == null)
            return 1.0F;

        int version = ((AttributeVersion) attributes).txoptimizations$version();

        if (attributes != this.txoptimizations$scaleMap || version != this.txoptimizations$scaleVersion) {
            this.txoptimizations$scale        = this.sanitizeScale((float) attributes.getValue(Attributes.SCALE));
            this.txoptimizations$scaleMap     = attributes;
            this.txoptimizations$scaleVersion = version;
        }

        return this.txoptimizations$scale;
    }
}

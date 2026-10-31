package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AttributeVersion;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AttributeMap.class)
public abstract class AttributeMapVersionMixin implements AttributeVersion {

    @Unique
    private int txoptimizations$version = 1;

    @Inject(method = "onAttributeModified", at = @At("HEAD"))
    private void txoptimizations$bumpVersion(AttributeInstance attributeInstance, CallbackInfo ci) {
        this.txoptimizations$version ++;
    }

    @Override
    public int txoptimizations$version() {
        return this.txoptimizations$version;
    }
}

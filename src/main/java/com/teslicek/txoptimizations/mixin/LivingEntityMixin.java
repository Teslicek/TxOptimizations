package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AttributeVersion;
import com.teslicek.txoptimizations.CullingHead;
import com.teslicek.txoptimizations.CullingHeadTracker;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements CullingHead {

    @Shadow
    @Final
    protected EntityEquipment equipment;

    @Unique
    private ItemStack txoptimizations$cullingHead;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$trackEquipment(EntityType<? extends LivingEntity> type, Level level, CallbackInfo ci) {
        ((CullingHeadTracker) this.equipment).txoptimizations$trackFor(this);
    }

    @Override
    public ItemStack txoptimizations$getCullingHead() {
        return this.txoptimizations$cullingHead;
    }

    @Override
    public void txoptimizations$setCullingHead(ItemStack head) {
        this.txoptimizations$cullingHead = head;
    }

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

package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.CullingHead;
import com.teslicek.txoptimizations.CullingHeadTracker;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCullingHeadMixin implements CullingHead {

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
}

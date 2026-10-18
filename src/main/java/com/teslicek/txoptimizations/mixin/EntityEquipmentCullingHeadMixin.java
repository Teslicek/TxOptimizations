package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.CullingHead;
import com.teslicek.txoptimizations.CullingHeadTracker;
import java.util.EnumMap;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityEquipment.class)
public abstract class EntityEquipmentCullingHeadMixin implements CullingHeadTracker {

    @Shadow
    @Final
    private EnumMap<EquipmentSlot, ItemStack> items;

    @Unique
    private CullingHead txoptimizations$owner;

    @Override
    public void txoptimizations$trackFor(CullingHead owner) {
        if (this.txoptimizations$owner != null)
            throw new IllegalStateException("Equipment already tracks the culling head of another entity");

        this.txoptimizations$owner = owner;
        this.txoptimizations$updateCullingHead();
    }

    @Inject(method = {"setAll", "clear"}, at = @At("RETURN"))
    private void txoptimizations$trackCullingHead(CallbackInfo ci) {
        this.txoptimizations$updateCullingHead();
    }

    @Inject(method = "set", at = @At("RETURN"))
    private void txoptimizations$trackSetCullingHead(EquipmentSlot slot, ItemStack itemStack, CallbackInfoReturnable<ItemStack> cir) {
        if (slot == EquipmentSlot.HEAD)
            this.txoptimizations$updateCullingHead();
    }

    @Unique
    private void txoptimizations$updateCullingHead() {
        if (this.txoptimizations$owner == null)
            return;

        ItemStack head = this.items.getOrDefault(EquipmentSlot.HEAD, ItemStack.EMPTY);

        this.txoptimizations$owner.txoptimizations$setCullingHead(head != ItemStack.EMPTY && head.item.value() == Items.DRAGON_HEAD ? head : ItemStack.EMPTY);
    }
}

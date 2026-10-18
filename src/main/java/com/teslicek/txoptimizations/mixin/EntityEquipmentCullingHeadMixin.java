package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.CullingHead;
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
public abstract class EntityEquipmentCullingHeadMixin implements CullingHead {

    @Shadow
    @Final
    private EnumMap<EquipmentSlot, ItemStack> items;

    @Unique
    private ItemStack txoptimizations$cullingHead;

    @Override
    public ItemStack txoptimizations$getCullingHead() {
        return this.txoptimizations$cullingHead;
    }

    @Inject(method = {"<init>(Ljava/util/EnumMap;)V", "setAll", "clear"}, at = @At("RETURN"))
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
        ItemStack head = this.items.getOrDefault(EquipmentSlot.HEAD, ItemStack.EMPTY);

        this.txoptimizations$cullingHead = head != ItemStack.EMPTY && head.item.value() == Items.DRAGON_HEAD ? head : ItemStack.EMPTY;
    }
}

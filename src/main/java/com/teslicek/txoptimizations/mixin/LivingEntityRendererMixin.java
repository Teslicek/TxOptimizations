package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.CullingHead;
import com.teslicek.txoptimizations.ModelCopies;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Unique
    private static final List<Class<?>> VANILLA_LAYERS = List.of(HumanoidArmorLayer.class, ItemInHandLayer.class, WingsLayer.class, CustomHeadLayer.class);

    @Shadow
    @Final
    protected List<RenderLayer<?, ?>> layers;

    @Unique
    private int txoptimizations$vanillaLayers;

    @ModifyExpressionValue(method = "submit", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private boolean txoptimizations$skipBareArmorStandLayers(boolean empty, @Local(argsOnly = true) LivingEntityRenderState state) {
        return empty || state instanceof ArmorStandRenderState armorStand && this.txoptimizations$hasVanillaLayers() && txoptimizations$isBare(armorStand);
    }

    @Unique
    private boolean txoptimizations$hasVanillaLayers() {
        if (this.txoptimizations$vanillaLayers == 0)
            this.txoptimizations$vanillaLayers = ((Object) this).getClass() == ArmorStandRenderer.class && this.layers.stream().map(Object::getClass).toList().equals(VANILLA_LAYERS) ? 1 : -1;

        return this.txoptimizations$vanillaLayers == 1;
    }

    @Unique
    private static boolean txoptimizations$isBare(ArmorStandRenderState state) {
        return state.headEquipment.isEmpty() && state.chestEquipment.isEmpty() && state.legsEquipment.isEmpty() && state.feetEquipment.isEmpty() && state.rightHandItemState.isEmpty() && state.leftHandItemState.isEmpty() && state.headItem.isEmpty() && state.wornHeadType == null;
    }

    @Redirect(method = "getBoundingBoxForCulling(Lnet/minecraft/world/entity/LivingEntity;F)Lnet/minecraft/world/phys/AABB;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack txoptimizations$cullingHead(LivingEntity entity, EquipmentSlot slot) {
        return ((CullingHead) entity).txoptimizations$getCullingHead();
    }

    @Redirect(method = "submit", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;model:Lnet/minecraft/client/model/EntityModel;", opcode = Opcodes.GETFIELD, ordinal = 1), require = 1)
    private EntityModel<?> txoptimizations$poseTwinForLayers(LivingEntityRenderer<?, ?, ?> renderer) {
        return ModelCopies.mainModel(renderer.getModel());
    }
}

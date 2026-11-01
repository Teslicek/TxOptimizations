package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AvatarSkins;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererExtractMixin {

    @Unique
    private Avatar txoptimizations$poseAvatar;

    @Unique
    private HumanoidModel.ArmPose txoptimizations$otherArmPose;

    @Shadow
    private static HumanoidModel.ArmPose getArmPose(Avatar avatar, ItemStack itemInHand, InteractionHand hand) {
        throw new AssertionError();
    }

    @Redirect(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/entity/HumanoidArm;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;", ordinal = 0))
    private HumanoidModel.ArmPose txoptimizations$bothArmPoses(Avatar avatar, HumanoidArm arm) {
        ItemStack             mainHandItem = avatar.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack             offHandItem  = avatar.getItemInHand(InteractionHand.OFF_HAND);
        HumanoidModel.ArmPose mainHandPose = getArmPose(avatar, mainHandItem, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose offHandPose  = getArmPose(avatar, offHandItem, InteractionHand.OFF_HAND);

        if (mainHandPose.isTwoHanded())
            offHandPose = offHandItem.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;

        boolean mainArm = avatar.getMainArm() == arm;

        this.txoptimizations$poseAvatar   = avatar;
        this.txoptimizations$otherArmPose = mainArm ? offHandPose : mainHandPose;

        return mainArm ? mainHandPose : offHandPose;
    }

    @Redirect(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/entity/HumanoidArm;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;", ordinal = 1))
    private HumanoidModel.ArmPose txoptimizations$otherArmPose(Avatar avatar, HumanoidArm arm) {
        if (avatar != this.txoptimizations$poseAvatar)
            throw new IllegalStateException("Arm pose of " + avatar + " was requested after the poses of " + this.txoptimizations$poseAvatar);

        this.txoptimizations$poseAvatar = null;

        return this.txoptimizations$otherArmPose;
    }

    @Redirect(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/ClientAvatarEntity;getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;"))
    private PlayerSkin txoptimizations$reuseFrameSkin(ClientAvatarEntity avatar) {
        return AvatarSkins.frameSkin(avatar);
    }
}

package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.BakeableBlockEntity;
import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.RenderMode;
import java.util.Optional;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecoratedPotBlockEntity.class)
public abstract class DecoratedPotBlockEntityBakeMixin {

    @Unique
    private static final PotDecorations BRICK_DECORATIONS = new PotDecorations(brick(), brick(), brick(), brick());

    @Shadow
    private PotDecorations decorations;

    @Inject(method = {"<init>", "loadAdditional", "applyImplicitComponents"}, at = @At("RETURN"))
    private void txoptimizations$forceEntityForSherds(CallbackInfo ci) {
        ((BakeableBlockEntity) this).txoptimizations$setForcedEntity(!this.decorations.equals(PotDecorations.EMPTY) && !this.decorations.equals(BRICK_DECORATIONS));
    }

    @Inject(method = "triggerEvent", at = @At("RETURN"))
    private void txoptimizations$wobbleAsEntity(int event, int data, CallbackInfoReturnable<Boolean> cir) {
        DecoratedPotBlockEntity pot = (DecoratedPotBlockEntity) (Object) this;

        Baking.requestMode(pot, RenderMode.ENTITY);
        ((BakeableBlockEntity) pot).txoptimizations$setTimer(pot.wobbleStartedAtTick, pot.lastWobbleStyle.duration);
    }

    @Inject(method = "getDecorations", at = @At("RETURN"))
    private void txoptimizations$settleAfterWobble(CallbackInfoReturnable<PotDecorations> cir) {
        DecoratedPotBlockEntity pot = (DecoratedPotBlockEntity) (Object) this;

        if (!((BakeableBlockEntity) pot).txoptimizations$isTimerFinished())
            return;

        Baking.requestMode(pot, RenderMode.TERRAIN);
    }

    @Unique
    private static Optional<ItemStackTemplate> brick() {
        return Optional.of(new ItemStackTemplate(Items.BRICK));
    }
}

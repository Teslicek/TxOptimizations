package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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

@Mixin(DecoratedPotBlockEntity.class)
public abstract class DecoratedPotBlockEntityBakeMixin {

    @Unique
    private static final PotDecorations BRICK_DECORATIONS = new PotDecorations(brick(), brick(), brick(), brick());

    @Shadow
    private PotDecorations decorations;

    @Inject(method = {"loadAdditional", "applyImplicitComponents"}, at = @At("RETURN"))
    private void txoptimizations$forceEntityForSherds(CallbackInfo ci) {
        ((BakeableBlockEntity) this).txoptimizations$setForcedEntity(!this.decorations.equals(PotDecorations.EMPTY) && !this.decorations.equals(BRICK_DECORATIONS));
    }

    @ModifyReturnValue(method = "triggerEvent", at = @At("RETURN"))
    private boolean txoptimizations$wobbleAsEntity(boolean wobbled) {
        DecoratedPotBlockEntity pot = (DecoratedPotBlockEntity) (Object) this;

        if (wobbled && pot.getLevel().isClientSide())
            Baking.requestMode(pot, RenderMode.ENTITY);

        return wobbled;
    }

    @ModifyReturnValue(method = "getDecorations", at = @At("RETURN"))
    private PotDecorations txoptimizations$settleAfterWobble(PotDecorations decorations) {
        DecoratedPotBlockEntity pot = (DecoratedPotBlockEntity) (Object) this;

        if (pot.lastWobbleStyle != null && pot.hasLevel() && pot.getLevel().isClientSide() && pot.getLevel().getGameTime() - pot.wobbleStartedAtTick > pot.lastWobbleStyle.duration)
            Baking.requestMode(pot, RenderMode.TERRAIN);

        return decorations;
    }

    @Unique
    private static Optional<ItemStackTemplate> brick() {
        return Optional.of(new ItemStackTemplate(Items.BRICK));
    }
}

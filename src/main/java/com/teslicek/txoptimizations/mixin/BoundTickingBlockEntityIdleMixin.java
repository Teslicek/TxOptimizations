package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.IdleBlockEntityTicks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BoundTickingBlockEntityIdleMixin {

    @Shadow
    @Final
    private BlockEntity blockEntity;

    @Shadow
    @Final
    LevelChunk this$0;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipIdleTick(CallbackInfo ci) {
        if (IdleBlockEntityTicks.isIdle(this.this$0.getLevel(), this.blockEntity))
            ci.cancel();
    }
}

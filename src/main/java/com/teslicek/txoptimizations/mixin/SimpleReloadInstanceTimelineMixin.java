package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.ReloadTimeline;
import com.teslicek.txoptimizations.ReloadTimelineHolder;
import java.util.Map;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ProfiledReloadInstance;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SimpleReloadInstance.class)
public abstract class SimpleReloadInstanceTimelineMixin implements ReloadTimelineHolder {

    @Unique
    private final long txoptimizations$startNanos = System.nanoTime();

    @Unique
    private final Map<String, Long> txoptimizations$preparedTimes = ReloadTimeline.newTimes();

    @ModifyReturnValue(method = "createBarrierForListener", at = @At("RETURN"))
    private PreparableReloadListener.PreparationBarrier txoptimizations$timePreparation(PreparableReloadListener.PreparationBarrier barrier, @Local(argsOnly = true) PreparableReloadListener listener) {
        if (!((Object) this instanceof ProfiledReloadInstance))
            return barrier;

        return new ReloadTimeline(barrier, listener.getName(), this.txoptimizations$startNanos, this.txoptimizations$preparedTimes);
    }

    @Override
    public Map<String, Long> txoptimizations$preparedTimes() {
        return this.txoptimizations$preparedTimes;
    }
}

package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ReloadTimeline;
import java.util.List;
import net.minecraft.server.packs.resources.ProfiledReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProfiledReloadInstance.class)
public abstract class ProfiledReloadInstanceTimelineMixin {

    @Inject(method = "finish", at = @At("HEAD"))
    private void txoptimizations$logTimeline(List<ProfiledReloadInstance.State> result, CallbackInfoReturnable<List<ProfiledReloadInstance.State>> cir) {
        ReloadTimeline.finish();
    }
}

package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.renderpearl.frontend.shaders.SPIRVModule;
import com.teslicek.txoptimizations.BackgroundCleanup;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SPIRVModule.class)
public abstract class SPIRVModuleBackgroundCloseMixin {

    @WrapMethod(method = "close")
    private void txoptimizations$closeInBackground(Operation<Void> original) {
        BackgroundCleanup.run(original::call);
    }
}

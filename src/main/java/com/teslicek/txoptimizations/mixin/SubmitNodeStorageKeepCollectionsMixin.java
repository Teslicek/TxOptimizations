package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import java.util.function.Predicate;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SubmitNodeStorage.class)
public abstract class SubmitNodeStorageKeepCollectionsMixin {

    @WrapOperation(method = "drainPhases", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectCollection;removeIf(Ljava/util/function/Predicate;)Z"))
    private boolean txoptimizations$drainWithoutRemoving(ObjectCollection<Object> collections, Predicate<Object> drain, Operation<Boolean> original) {
        for (Object collection : collections)
            drain.test(collection);

        return false;
    }
}

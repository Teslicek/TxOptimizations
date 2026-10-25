package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import java.util.function.Predicate;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeStorage.class)
public abstract class SubmitNodeStorageKeepCollectionsMixin {

    @Unique
    private static final int FIRST_ORDER = 0;

    @Shadow
    @Final
    private Int2ObjectAVLTreeMap<SubmitNodeCollection> submitsPerOrder;

    @Shadow
    @Final
    private TranslucentFeatureRenderPhase seeThrough;

    @Shadow
    private boolean useImprovedTransparency;

    @Unique
    private @Nullable SubmitNodeCollection txoptimizations$firstOrder;

    @Overwrite
    public SubmitNodeCollection order(int order) {
        if (order == FIRST_ORDER && this.txoptimizations$firstOrder != null)
            return this.txoptimizations$firstOrder;

        SubmitNodeCollection collection = this.submitsPerOrder.get(order);

        if (collection == null) {
            collection = new SubmitNodeCollection(this.useImprovedTransparency, this.seeThrough);
            this.submitsPerOrder.put(order, collection);
        }

        if (order == FIRST_ORDER)
            this.txoptimizations$firstOrder = collection;

        return collection;
    }

    @Inject(method = "setUseImprovedTransparency", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectAVLTreeMap;clear()V"))
    private void txoptimizations$forgetFirstOrder(boolean useImprovedTransparency, CallbackInfo ci) {
        this.txoptimizations$firstOrder = null;
    }

    @WrapOperation(method = "drainPhases", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectCollection;removeIf(Ljava/util/function/Predicate;)Z"))
    private boolean txoptimizations$drainWithoutRemoving(ObjectCollection<Object> collections, Predicate<Object> drain, Operation<Boolean> original) {
        for (Object collection : collections)
            drain.test(collection);

        return false;
    }
}

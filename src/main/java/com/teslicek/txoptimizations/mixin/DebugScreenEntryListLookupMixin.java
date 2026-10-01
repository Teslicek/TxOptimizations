package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DebugScreenEntryList.class)
public abstract class DebugScreenEntryListLookupMixin {

    @Shadow
    private long currentlyEnabledVersion;

    @Unique
    private final Set<Identifier> txoptimizations$enabledLookup = new HashSet<>();

    @Unique
    private long txoptimizations$enabledLookupVersion = -1L;

    @WrapOperation(method = "isCurrentlyEnabled", at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"))
    private boolean txoptimizations$lookupEnabled(List<Identifier> currentlyEnabled, Object id, Operation<Boolean> original) {
        if (this.txoptimizations$enabledLookupVersion != this.currentlyEnabledVersion) {
            this.txoptimizations$enabledLookup.clear();
            this.txoptimizations$enabledLookup.addAll(currentlyEnabled);
            this.txoptimizations$enabledLookupVersion = this.currentlyEnabledVersion;
        }

        return this.txoptimizations$enabledLookup.contains(id);
    }
}

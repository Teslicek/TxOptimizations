package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListAppendMixin {

    @Shadow
    @Final
    private List<? extends LayoutElement> children;

    @Unique
    private boolean txoptimizations$refilling;

    @WrapMethod(method = "replaceEntries")
    private void txoptimizations$refill(Collection<?> entries, Operation<Void> original) {
        if (this.txoptimizations$refilling)
            throw new IllegalStateException("Selection list refilled while it was already being refilled");

        this.txoptimizations$refilling = true;

        try {
            original.call(entries);
        } finally {
            this.txoptimizations$refilling = false;
        }
    }

    @Redirect(method = "addEntry(Lnet/minecraft/client/gui/components/AbstractSelectionList$Entry;I)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;getNextY()I"))
    private int txoptimizations$appendY(AbstractSelectionList<?> list) {
        if (!this.txoptimizations$refilling || this.children.isEmpty())
            return list.getNextY();

        LayoutElement last = this.children.getLast();

        return last.getY() + last.getHeight();
    }
}

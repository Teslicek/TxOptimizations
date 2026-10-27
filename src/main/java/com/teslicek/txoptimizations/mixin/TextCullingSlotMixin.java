package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TextCullingBox;
import com.teslicek.txoptimizations.TextCullingSlot;
import net.minecraft.world.entity.Display;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Display.TextDisplay.CachedInfo.class)
public abstract class TextCullingSlotMixin implements TextCullingSlot {

    @Unique
    private @Nullable TextCullingBox txoptimizations$cullingBox;

    @Override
    public @Nullable TextCullingBox txoptimizations$cullingBox() {
        return this.txoptimizations$cullingBox;
    }

    @Override
    public void txoptimizations$setCullingBox(TextCullingBox box) {
        this.txoptimizations$cullingBox = box;
    }
}

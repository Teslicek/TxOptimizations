package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.SignMessageCache;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SignText.class)
public abstract class SignTextMessageCacheMixin implements SignMessageCache {

    @Unique
    private static final byte UNKNOWN = 0;

    @Unique
    private static final byte EMPTY = 1;

    @Unique
    private static final byte PRESENT = 2;

    @Unique
    private byte txoptimizations$message = UNKNOWN;

    @Unique
    private byte txoptimizations$filteredMessage = UNKNOWN;

    @Override
    public boolean txoptimizations$hasMessage(boolean filtered) {
        byte cached = filtered ? this.txoptimizations$filteredMessage : this.txoptimizations$message;

        if (cached != UNKNOWN)
            return cached == PRESENT;

        cached = ((SignText) (Object) this).hasMessage(filtered) ? PRESENT : EMPTY;

        if (filtered)
            this.txoptimizations$filteredMessage = cached;
        else
            this.txoptimizations$message = cached;

        return cached == PRESENT;
    }
}

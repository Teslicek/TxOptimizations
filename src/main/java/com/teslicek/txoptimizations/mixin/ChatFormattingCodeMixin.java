package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ChatFormattingCode;
import com.teslicek.txoptimizations.ChatFormattingCodes;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatFormatting.class)
public abstract class ChatFormattingCodeMixin implements ChatFormattingCode {

    @Shadow
    @Final
    private char code;

    @Override
    public char txoptimizations$code() {
        return this.code;
    }

    @Overwrite
    public static @Nullable ChatFormatting getByCode(char code) {
        return ChatFormattingCodes.byCode(code);
    }
}

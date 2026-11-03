package com.teslicek.txoptimizations;

import net.minecraft.ChatFormatting;
import org.jspecify.annotations.Nullable;

public final class ChatFormattingCodes {

    private static final int              ASCII    = 128;
    private static final ChatFormatting[] FORMATS  = ChatFormatting.values();
    private static final ChatFormatting[] BY_ASCII = byAscii();

    private ChatFormattingCodes() {
    }

    public static @Nullable ChatFormatting byCode(char code) {
        char lower = Character.toLowerCase(code);

        if (lower < ASCII)
            return BY_ASCII[lower];

        for (ChatFormatting format : FORMATS) {
            if (((ChatFormattingCode) (Object) format).txoptimizations$code() == lower)
                return format;
        }

        return null;
    }

    private static ChatFormatting[] byAscii() {
        ChatFormatting[] table = new ChatFormatting[ASCII];

        for (int index = FORMATS.length - 1; index >= 0; index --) {
            char code = ((ChatFormattingCode) (Object) FORMATS[index]).txoptimizations$code();

            if (code < ASCII)
                table[code] = FORMATS[index];
        }

        return table;
    }
}

package com.teslicek.txoptimizations.mixin;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterDirection;
import net.minecraft.client.resources.language.FormattedBidiReorder;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.SubStringSource;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FormattedBidiReorder.class)
public abstract class FormattedBidiReorderFastPathMixin {

    @Unique
    private static final int FIRST_RIGHT_TO_LEFT_CODE_POINT = 0x0590;

    @Unique
    private static final int REORDERING_DIRECTIONS = 1 << UCharacterDirection.RIGHT_TO_LEFT
        | 1 << UCharacterDirection.RIGHT_TO_LEFT_ARABIC
        | 1 << UCharacterDirection.ARABIC_NUMBER
        | 1 << UCharacterDirection.LEFT_TO_RIGHT_EMBEDDING
        | 1 << UCharacterDirection.LEFT_TO_RIGHT_OVERRIDE
        | 1 << UCharacterDirection.RIGHT_TO_LEFT_EMBEDDING
        | 1 << UCharacterDirection.RIGHT_TO_LEFT_OVERRIDE
        | 1 << UCharacterDirection.POP_DIRECTIONAL_FORMAT
        | 1 << UCharacterDirection.LEFT_TO_RIGHT_ISOLATE
        | 1 << UCharacterDirection.RIGHT_TO_LEFT_ISOLATE
        | 1 << UCharacterDirection.FIRST_STRONG_ISOLATE
        | 1 << UCharacterDirection.POP_DIRECTIONAL_ISOLATE;

    @Inject(method = "reorder", at = @At("HEAD"), cancellable = true)
    private static void txoptimizations$keepLeftToRightOrder(FormattedText text, boolean defaultRightToLeft, CallbackInfoReturnable<FormattedCharSequence> cir) {
        if (defaultRightToLeft)
            return;

        SubStringSource source    = SubStringSource.create(text);
        String          plainText = source.getPlainText();

        if (txoptimizations$needsReordering(plainText))
            return;

        cir.setReturnValue(FormattedCharSequence.composite(source.substring(0, plainText.length(), false)));
    }

    @Unique
    private static boolean txoptimizations$needsReordering(String text) {
        int index = 0;

        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);

            if (codePoint >= FIRST_RIGHT_TO_LEFT_CODE_POINT && (REORDERING_DIRECTIONS & 1 << UCharacter.getDirection(codePoint)) != 0)
                return true;
        }

        return false;
    }
}

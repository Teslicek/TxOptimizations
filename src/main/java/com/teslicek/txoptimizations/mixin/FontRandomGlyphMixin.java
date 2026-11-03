package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.util.RandomSource;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Font.class)
public abstract class FontRandomGlyphMixin {

    @Unique
    private static final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(RandomSource::create);

    @Redirect(method = "getGlyph", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Font;random:Lnet/minecraft/util/RandomSource;", opcode = Opcodes.GETFIELD), require = 1)
    private RandomSource txoptimizations$threadRandom(Font font) {
        return RANDOM.get();
    }
}
